package org.elasticsearch.autocancel.core;

import org.elasticsearch.autocancel.manager.MainManager;
import org.elasticsearch.autocancel.utils.id.CancellableID;
import org.elasticsearch.autocancel.utils.logger.Logger;
import org.elasticsearch.autocancel.utils.resource.CPUResource;
import org.elasticsearch.autocancel.utils.resource.MemoryResource;
import org.elasticsearch.autocancel.utils.resource.QueueResource;
import org.elasticsearch.autocancel.utils.resource.Resource;
import org.elasticsearch.autocancel.utils.resource.ResourceName;
import org.elasticsearch.autocancel.utils.resource.ResourceType;
import org.elasticsearch.autocancel.core.monitor.MainMonitor;
import org.elasticsearch.autocancel.core.performance.Performance;
import org.elasticsearch.autocancel.core.utils.OperationRequest;
import org.elasticsearch.autocancel.core.utils.ResourcePool;
import org.elasticsearch.autocancel.core.utils.ResourceUsage;
import org.elasticsearch.autocancel.core.utils.Cancellable;
import org.elasticsearch.autocancel.core.utils.CancellableGroup;
import org.elasticsearch.autocancel.core.utils.OperationMethod;
import org.elasticsearch.autocancel.utils.Settings;

import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.Collection;
import java.util.HashMap;
import java.lang.Thread;
import java.util.List;
import java.util.Set;
import java.util.HashSet;

public class AutoCancelCore {

    private MainManager mainManager = null;

    private MainMonitor mainMonitor;

    private Map<CancellableID, CancellableGroup> rootCancellableToCancellableGroup;

    private Map<CancellableID, Cancellable> cancellables;

    private ResourcePool resourcePool;

    private Performance performanceMetrix;

    private RequestParser requestParser;

    private Logger logger;

    private AutoCancelInfoCenter infoCenter;

    public AutoCancelCore(MainManager mainManager) {
        this.cancellables = new HashMap<CancellableID, Cancellable>();
        this.rootCancellableToCancellableGroup = new HashMap<CancellableID, CancellableGroup>();
        this.requestParser = new RequestParser();
        this.logger = new Logger("corerequest");
        this.performanceMetrix = new Performance();
        this.resourcePool = new ResourcePool(true);

        this.resourcePool.addResource(Resource.createResource(ResourceType.CPU, ResourceName.CPU));
        this.resourcePool.addResource(Resource.createResource(ResourceType.MEMORY, ResourceName.MEMORY));

        this.infoCenter = new AutoCancelInfoCenter(this.rootCancellableToCancellableGroup,
                this.cancellables,
                this.resourcePool,
                this.performanceMetrix);

        this.initialize(mainManager);
    }

    public AutoCancelCore() {
        this.cancellables = new HashMap<CancellableID, Cancellable>();
        this.rootCancellableToCancellableGroup = new HashMap<CancellableID, CancellableGroup>();
        this.requestParser = new RequestParser();
        this.logger = new Logger("corerequest");
        this.performanceMetrix = new Performance();
        this.resourcePool = new ResourcePool(true);

        this.resourcePool.addResource(Resource.createResource(ResourceType.CPU, ResourceName.CPU));
        this.resourcePool.addResource(Resource.createResource(ResourceType.MEMORY, ResourceName.MEMORY));

        this.infoCenter = new AutoCancelInfoCenter(this.rootCancellableToCancellableGroup,
                this.cancellables,
                this.resourcePool,
                this.performanceMetrix);
    }

    public void initialize(MainManager mainManager) {
        if (this.mainManager == null) {
            this.mainManager = mainManager;
            this.mainMonitor = new MainMonitor(this.mainManager, this.cancellables,
                    this.rootCancellableToCancellableGroup);
        }
    }

    public Boolean isInitialized() {
        return this.mainManager != null;
    }

    public void start() {
        while (!Thread.interrupted()) {
            try {
                this.startOneLoop();

                Thread.sleep((Long) Settings.getSetting("core_update_cycle_ms"));
            } catch (InterruptedException e) {
                break;
            }
        }
        this.stop();
    }

    public void startOneLoop() {
        if (this.isInitialized()) {

            this.refreshCancellableGroups();

            this.resourcePool.refreshResources(this.logger);

            this.logger.log(this.performanceMetrix.toString());

            Long timestampMilli = System.currentTimeMillis();

            this.performanceMetrix.reset(timestampMilli);

            this.logger.log(String.format("Current time: %d", timestampMilli));

            CancellableGroup.cancellableGroupLog(String.format("Current time: %d", timestampMilli));

            // refresh stuff should be done before update
            Integer requestBufferSize = this.mainManager.getManagerRequestToCoreBufferSize();
            for (Integer ignore = 0; ignore < requestBufferSize; ++ignore) {
                OperationRequest request = this.mainManager.getManagerRequestToCore();
                this.requestParser.parse(request);
            }

            this.mainMonitor.updateTasksResources();

            Integer updateBufferSize = this.mainMonitor.getMonitorUpdateToCoreBufferSizeWithoutLock();
            for (Integer ignore = 0; ignore < updateBufferSize; ++ignore) {
                OperationRequest request = this.mainMonitor.getMonitorUpdateToCoreWithoutLock();
                this.requestParser.parse(request);
            }
        } else {
            Logger.systemWarn("AutoCancelCore hasn't initialized, use initialize() first");
        }
    }

    AutoCancelInfoCenter getInfoCenter() {
        if (this.isInitialized()) {
            return this.infoCenter;
        } else {
            Logger.systemWarn("AutoCancelCore hasn't initialized, use initialize() first");
            return null;
        }
    }

    public void stop() {
        if (this.isInitialized()) {
            this.logger.close();
            System.out.println("Recieve interrupt, exit");
        } else {
            Logger.systemWarn("AutoCancelCore hasn't initialized, use initialize() first");
        }
    }

    private void refreshCancellableGroups() {
        for (Map.Entry<CancellableID, CancellableGroup> entry : this.rootCancellableToCancellableGroup.entrySet()) {

            if (entry.getValue().isExit()) {
                continue;
            }

            entry.getValue().refreshResourcePool();
        }
    }

    protected void addCancellable(Cancellable cancellable) {
        this.cancellables.put(cancellable.getID(), cancellable);

        Logger.systemTrace("Add cancellable with " + cancellable.toString());

        if (cancellable.isRoot()) {
            this.rootCancellableToCancellableGroup.put(cancellable.getID(), new CancellableGroup(cancellable));
        } else {
            this.rootCancellableToCancellableGroup.get(cancellable.getRootID()).putCancellable(cancellable);
        }
    }

    protected void removeCancellable(Cancellable cancellable) {
        this.cancellables.remove(cancellable.getID());

        if (cancellable.isRoot()) {
            // TODO: Problematic point: nullptr
            // CancellableGroup cancellableGroup =
            // this.rootCancellableToCancellableGroup.remove(cancellable.getID());
            // // Remove all child cancellables at the same time
            // Collection<Cancellable> childCancellables =
            // cancellableGroup.getChildCancellables();
            // for (Cancellable childCancellable : childCancellables) {
            // this.cancellables.remove(childCancellable.getID());
            // }
            this.rootCancellableToCancellableGroup.get(cancellable.getID()).exit();
            if (((Set<?>) Settings.getSetting("monitor_actions")).contains(cancellable.getAction())) {
                this.performanceMetrix.increaseFinishedTask();
            }
        } else {
            // Nothing to do: In the group, we don't care about whether a cancellable is
            // existing
            // because we update cancellables according to this.cancellables
            // TODO: find a more robust way to handle it

        }
    }

    private class RequestParser {
        ParamHandlers paramHandlers;

        public RequestParser() {
            this.paramHandlers = new ParamHandlers();
        }

        public void parse(OperationRequest request) {
            logger.log(request.toString());
            switch (request.getOperation()) {
                case CREATE:
                    create(request);
                    break;
                case RETRIEVE:
                    retrieve(request);
                    break;
                case UPDATE:
                    update(request);
                    break;
                case DELETE:
                    delete(request);
                    break;
                default:
                    break;
            }
        }

        private void create(OperationRequest request) {
            CancellableID parentID = request.getParentCancellableID();

            assert parentID != null : "Must set parent_cancellable_id when create cancellable.";
            assert request.getCancellableID() != new CancellableID() : "Create operation must have cancellable id set";

            CancellableID rootID = this.getRootID(request);
            Cancellable cancellable = new Cancellable(request.getCancellableID(), parentID, rootID);

            addCancellable(cancellable);

            Map<String, Object> params = request.getParams();
            for (String key : params.keySet()) {
                this.paramHandlers.handle(key, request);
            }
        }

        private void retrieve(OperationRequest request) {

        }

        private void update(OperationRequest request) {
            Map<String, Object> params = request.getParams();
            for (String key : params.keySet()) {
                this.paramHandlers.handle(key, request);
            }
        }

        private void delete(OperationRequest request) {
            assert request.getCancellableID() != new CancellableID() : "Delete operation must have cancellable id set";

            if (cancellables.containsKey(request.getCancellableID())) {
                removeCancellable(cancellables.get(request.getCancellableID()));

                Map<String, Object> params = request.getParams();
                for (String key : params.keySet()) {
                    this.paramHandlers.handle(key, request);
                }
            } else {
                // Some task will exit before its child task exit, we will remove all child
                // tasks when root task exit (See removeCancellable())
                // TODO: Find a method to handle it
                System.out.println(String.format("Cancellable id not found: Time: %d, %s", System.currentTimeMillis(),
                        request.getCancellableID().toString()));
            }
        }

        private CancellableID getRootID(OperationRequest request) {
            CancellableID parentID = request.getParentCancellableID();
            assert parentID != null : "Must set parent_cancellable_id when create cancellable.";
            CancellableID rootID = null;
            if (!parentID.isValid()) {
                // Itself is a root cancellable
                rootID = request.getCancellableID();
            } else {
                rootID = cancellables.get(parentID).getRootID();
            }
            return rootID;
        }

    }

    private class ParamHandlers {

        // These parameters' parsing order doesn't matter
        private final Map<String, Consumer<OperationRequest>> paramHandlers = Map.of(
                "basic_info", request -> {},
                "is_cancellable", request -> this.isCancellable(request),
                "cancellable_name", request -> this.cancellableName(request),
                "cancellable_action", request -> this.cancellableAction(request),
                "update_group_resource", request -> this.updateGroupResource(request));

        public ParamHandlers() {

        }

        public void handle(String type, OperationRequest request) {
            assert this.paramHandlers.containsKey(type) : "Invalid parameter " + type;
            this.paramHandlers.get(type).accept(request);
        }

        private void isCancellable(OperationRequest request) {
            Cancellable cancellable = cancellables.get(request.getCancellableID());
            if (cancellable.isRoot()) {
                // This is a root cancellable
                // Parameter is_cancellable is useful only if this cancellable is a root
                // cancellable
                // TODO: Add a warning if this is not a root cancellable
                Boolean isCancellable = (Boolean) request.getParams().get("is_cancellable");
                rootCancellableToCancellableGroup.get(cancellable.getID()).setIsCancellable(isCancellable);
            }
        }

        private void cancellableName(OperationRequest request) {
            Cancellable cancellable = cancellables.get(request.getCancellableID());
            String name = (String) request.getParams().get("cancellable_name");
            cancellable.setName(name);
        }

        private void cancellableAction(OperationRequest request) {
            Cancellable cancellable = cancellables.get(request.getCancellableID());
            String action = (String) request.getParams().get("cancellable_action");
            cancellable.setAction(action);
        }

        @SuppressWarnings("unchecked")
        private void updateGroupResource(OperationRequest request) {
            Cancellable cancellable = cancellables.get(request.getCancellableID());
            if (cancellable != null) {
                ResourceType resourceType = request.getResourceType();
                ResourceName resourceName = request.getResourceName();
                if (!resourceName.equals(ResourceName.NULL) && !resourceType.equals(ResourceType.NULL)) {
                    Map<String, Object> resourceUpdateInfo = (Map<String, Object>) request.getParams().get("update_group_resource");
                    rootCancellableToCancellableGroup.get(cancellable.getRootID()).updateResource(resourceType, resourceName, resourceUpdateInfo);
                    if (!resourcePool.isResourceExist(request.getResourceName())) {
                        resourcePool.addResource(Resource.createResource(resourceType, resourceName));
                    }
                    resourcePool.setResourceUpdateInfo(resourceName, resourceUpdateInfo);
                }

            } else {
                System.out.println("Can't find cancellable for cid " + request.getCancellableID());
            }
        }
    }
}
