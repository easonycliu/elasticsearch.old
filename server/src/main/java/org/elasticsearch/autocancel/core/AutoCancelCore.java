package org.elasticsearch.autocancel.core;

import org.elasticsearch.autocancel.manager.MainManager;
import org.elasticsearch.autocancel.utils.id.CancellableID;
import org.elasticsearch.autocancel.utils.logger.Logger;
import org.elasticsearch.autocancel.core.monitor.MainMonitor;
import org.elasticsearch.autocancel.core.utils.OperationRequest;
import org.elasticsearch.autocancel.core.utils.ResourcePool;
import org.elasticsearch.autocancel.core.utils.ResourceUsage;
import org.elasticsearch.autocancel.core.utils.Cancellable;
import org.elasticsearch.autocancel.core.utils.CancellableGroup;
import org.elasticsearch.autocancel.core.utils.OperationMethod;
import org.elasticsearch.autocancel.utils.Settings;
import org.elasticsearch.autocancel.utils.Resource.ResourceName;
import org.elasticsearch.autocancel.utils.Resource.ResourceType;
import org.elasticsearch.autocancel.utils.Resource.CPUResource;
import org.elasticsearch.autocancel.utils.Resource.MemoryResource;
import org.elasticsearch.autocancel.utils.Resource.LockResource;

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

    private MainManager mainManager;

    private MainMonitor mainMonitor;

    private Map<CancellableID, CancellableGroup> rootCancellableToCancellableGroup;

    private Map<CancellableID, Cancellable> cancellables;

    private ResourcePool resourcePool;

    private RequestParser requestParser;

    private Logger logger;

    public AutoCancelCore(MainManager mainManager) {
        this.mainManager = mainManager;
        this.cancellables = new HashMap<CancellableID, Cancellable>();
        this.rootCancellableToCancellableGroup = new HashMap<CancellableID, CancellableGroup>();
        this.mainMonitor = new MainMonitor(this.mainManager, this.cancellables, this.rootCancellableToCancellableGroup);
        this.requestParser = new RequestParser();
        this.logger = new Logger("corerequest");
        this.resourcePool = new ResourcePool();

        this.resourcePool.addResource(new CPUResource());
        this.resourcePool.addResource(new MemoryResource());
    }

    public void start() {
        while (!Thread.interrupted()) {
            try {
                this.refreshCancellableGroups();

                this.logger.log(String.format("Current time: %d", System.currentTimeMillis()));
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

                Thread.sleep((Long) Settings.getSetting("core_update_cycle_ms"));
            } catch (InterruptedException e) {
                break;
            }
        }
        this.stop();
    }

    private void stop() {
        this.logger.close();
        System.out.println("Recieve interrupt, exit");
    }

    private void refreshCancellableGroups() {
        for (Map.Entry<CancellableID, CancellableGroup> entries : this.rootCancellableToCancellableGroup.entrySet()) {

            if (entries.getValue().isExit()) {
                continue;
            }

            Set<ResourceName> resourceNames = entries.getValue().getResourceNames();
            for (ResourceName resourceName : resourceNames) {
                this.logger.log(String.format("Cancellable group with root %s used %s resource %s",
                        entries.getKey().toString(), resourceName.toString(),
                        entries.getValue().getResourceUsage(resourceName)));
                OperationRequest request = new OperationRequest(OperationMethod.UPDATE,
                        Map.of("cancellable_id", entries.getKey(), "resource_name", resourceName));
                request.addRequestParam("set_group_resource", 0.0);
                requestParser.parse(request);
            }
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
                rootID = cancellables.get(parentID).getID();
            }
            return rootID;
        }

    }

    private class ParamHandlers {

        // These parameters' parsing order doesn't matter
        private final Map<String, Consumer<OperationRequest>> paramHandlers = Map.of(
                "basic_info", (request) -> {},
                "is_cancellable", request -> this.isCancellable(request),
                "set_group_resource", request -> this.setGroupResource(request),
                "monitor_resource", request -> this.monitorResource(request),
                "cancellable_name", request -> this.cancellableName(request),
                "add_group_resource", request -> this.addGroupResource(request),
                "resource_update_info", request -> this.resourceUpdateInfo(request));

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

        private void setGroupResource(OperationRequest request) {
            Cancellable cancellable = cancellables.get(request.getCancellableID());
            // TODO: Problematic point: nullptr
            if (cancellable.isRoot()) {
                Double value = (Double) request.getParams().get("set_group_resource");
                rootCancellableToCancellableGroup.get(cancellable.getID()).setResourceUsage(request.getResourceName(),
                        value);
            }
        }

        private void monitorResource(OperationRequest request) {
            Cancellable cancellable = cancellables.get(request.getCancellableID());
            if (cancellable.isRoot()) {
                // This is a root cancellable
                // Parameter monitor_resource is useful only if this cancellable is a root
                // cancellable
                // TODO: Add a warning if this is not a root cancellable
                List<?> resourceNames = (List<?>) request.getParams().get("monitor_resource");
                CancellableGroup cancellableGroup = rootCancellableToCancellableGroup.get(cancellable.getID());
                for (Object resourceName : resourceNames) {
                    cancellableGroup.setResourceUsage((ResourceName) resourceName, 0.0);
                }
            }
        }

        private void cancellableName(OperationRequest request) {
            Cancellable cancellable = cancellables.get(request.getCancellableID());
            String name = (String) request.getParams().get("cancellable_name");
            cancellable.setName(name);
        }

        private void addGroupResource(OperationRequest request) {
            Cancellable cancellable = cancellables.get(request.getCancellableID());
            Double value = (Double) request.getParams().get("add_group_resource");
            rootCancellableToCancellableGroup.get(cancellable.getRootID()).addResourceUsage(request.getResourceName(),
                    value);
        }

        @SuppressWarnings("unchecked")
        private void resourceUpdateInfo(OperationRequest request) {
            ResourceName resourceName = request.getResourceName();
            ResourceType resourceType = request.getResourceType();
            if (!resourceName.equals(ResourceName.NULL) && !resourceType.equals(ResourceType.NULL)) {
                if (resourcePool.isResourceExist(request.getResourceName())) {
                    resourcePool.setResourceUpdateInfo(resourceName, (Map<String, Object>) request.getParams().get("resource_update_info"));
                }
                else {
                    switch (resourceType) {
                        case CPU:
                            resourcePool.addResource(new CPUResource(resourceName));
                            break;
                        case MEMORY:
                            resourcePool.addResource(new MemoryResource(resourceName));
                            break;
                        case LOCK:
                            resourcePool.addResource(new LockResource(resourceName));
                            break;
                        case NULL:
                            assert false : "Should never be here";
                            return;
                    }
                    
                    resourcePool.setResourceUpdateInfo(resourceName, (Map<String, Object>) request.getParams().get("resource_update_info"));
                }
            }
            else {
                Logger.systemWarn("Update resource info should have resource type and name set");
            }
        }

    }
}
