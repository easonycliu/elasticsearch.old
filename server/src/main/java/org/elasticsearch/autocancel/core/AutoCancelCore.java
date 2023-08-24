package org.elasticsearch.autocancel.core;

import org.elasticsearch.autocancel.manager.MainManager;
import org.elasticsearch.autocancel.utils.id.CancellableID;
import org.elasticsearch.autocancel.utils.logger.Logger;
import org.elasticsearch.autocancel.core.monitor.MainMonitor;
import org.elasticsearch.autocancel.core.utils.OperationRequest;
import org.elasticsearch.autocancel.core.utils.ResourceUsage;
import org.elasticsearch.autocancel.core.utils.Cancellable;
import org.elasticsearch.autocancel.core.utils.CancellableGroup;
import org.elasticsearch.autocancel.core.utils.OperationMethod;
import org.elasticsearch.autocancel.utils.Settings;
import org.elasticsearch.autocancel.utils.Resource.ResourceType;

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

    private RequestParser requestParser;

    private Logger logger;

    public AutoCancelCore(MainManager mainManager) {
        this.mainManager = mainManager;
        this.cancellables = new HashMap<CancellableID, Cancellable>();
        this.rootCancellableToCancellableGroup = new HashMap<CancellableID, CancellableGroup>();
        this.mainMonitor = new MainMonitor(this.mainManager, this.cancellables, this.rootCancellableToCancellableGroup);
        this.requestParser = new RequestParser();
        this.logger = new Logger("corerequest");
    }

    public void start() {
        while (!Thread.interrupted()) {
            try {
                this.logger.log(String.format("Current time: %d", System.currentTimeMillis()));
                Integer requestBufferSize = this.mainManager.getManagerRequestToCoreBufferSize();
                for (Integer ignore = 0; ignore < requestBufferSize; ++ignore) {
                    OperationRequest request = this.mainManager.getManagerRequestToCore();
                    this.requestParser.parse(request);
                }

                this.refreshCancellableGroups();

                this.mainMonitor.updateTasksResources();

                Integer updateBufferSize = this.mainMonitor.getMonitorUpdateToCoreBufferSizeWithoutLock();
                for (Integer ignore = 0; ignore < updateBufferSize; ++ignore) {
                    OperationRequest request = this.mainMonitor.getMonitorUpdateToCoreWithoutLock();
                    this.requestParser.parse(request);
                }

                Thread.sleep((Long) Settings.getSetting("core_update_cycle_ms"));
            }
            catch (InterruptedException e) {
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

            Set<ResourceType> resourceTypes = entries.getValue().getResourceTypes();
            for (ResourceType type : resourceTypes) {
                this.logger.log(String.format("Cancellable group with root %s used %s resource %s", entries.getKey().toString(), type.toString(), entries.getValue().getResourceUsage(type)));
                OperationRequest request = new OperationRequest(OperationMethod.UPDATE, entries.getKey(), type);
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
        }
        else {
            this.rootCancellableToCancellableGroup.get(cancellable.getRootID()).putCancellable(cancellable);
        }
    }

    protected void removeCancellable(Cancellable cancellable) {
        this.cancellables.remove(cancellable.getID());

        
        if (cancellable.isRoot()) {
            // TODO: Problematic point: nullptr
            // CancellableGroup cancellableGroup = this.rootCancellableToCancellableGroup.remove(cancellable.getID());
            // // Remove all child cancellables at the same time
            // Collection<Cancellable> childCancellables = cancellableGroup.getChildCancellables();
            // for (Cancellable childCancellable : childCancellables) {
            //     this.cancellables.remove(childCancellable.getID());
            // }
            this.rootCancellableToCancellableGroup.get(cancellable.getID()).exit();
        }
        else {
            // Nothing to do: In the group, we don't care about whether a cancellable is existing
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
            assert request.getParams().containsKey("parent_cancellable_id") : "Must set parent_cancellable_id when create cancellable.";
            assert request.getTarget() != new CancellableID() : "Create operation must have cancellable id set";
            
            CancellableID parentID = (CancellableID) request.getParams().get("parent_cancellable_id");
            CancellableID rootID = (CancellableID) this.paramHandlers.handle("parent_cancellable_id", request);
            Cancellable cancellable = new Cancellable(request.getTarget(), parentID, rootID);
            
            addCancellable(cancellable);

            Map<String, Object> params = request.getParams();
            for (String key : params.keySet()) {
                this.paramHandlers.handleIndependentParam(key, request);
            }
        }

        private void retrieve(OperationRequest request) {

        }

        private void update(OperationRequest request) {
            Map<String, Object> params = request.getParams();
            for (String key : params.keySet()) {
                this.paramHandlers.handleIndependentParam(key, request);
            }
        }

        private void delete(OperationRequest request) {
            assert request.getTarget() != new CancellableID() : "Create operation must have cancellable id set";
            assert cancellables.containsKey(request.getTarget()) : "Must cancel a existing cancellable.";
            
            if (cancellables.containsKey(request.getTarget())) {
                removeCancellable(cancellables.get(request.getTarget()));

                Map<String, Object> params = request.getParams();
                for (String key : params.keySet()) {
                    this.paramHandlers.handleIndependentParam(key, request);
                }
            }
            else {
                // Some task will exit before its child task exit, we will remove all child tasks when root task exit (See removeCancellable())
                // TODO: Find a method to handle it
                System.out.println(String.format("Cancellable id not found: Time: %d, %s", System.currentTimeMillis(), request.getTarget().toString()));
            }
        }

    }

    private class ParamHandlers {

        // These parameters' parsing order doesn't matter
        private final Map<String, Consumer<OperationRequest>> independentParamHandlers = Map.of(
            "is_cancellable", request -> this.isCancellable(request),
            "set_group_resource", request -> this.setGroupResource(request),
            "monitor_resource", request -> this.monitorResource(request),
            "cancellable_name", request -> this.cancellableName(request),
            "add_group_resource", request -> this.addGroupResource(request)
        );
        /*
         * Some parameters' parsing order does matter, currently there are:
         * TODO: Find a way to unify them
         */
        private final Map<String, Function<OperationRequest, Object>> functionHandlers = Map.of(
            "parent_cancellable_id", request -> this.parentCancellableID(request)
        );

        public ParamHandlers() {

        }

        public Object handle(String type, OperationRequest request) {
            Object ret;
            if (this.functionHandlers.containsKey(type)) {
                ret = this.functionHandlers.get(type).apply(request);
            }
            // TODO: Add other handler types
            else {
                ret = null;
                assert false : "Invalid parameter";
            }
            return ret;
        }

        public void handleIndependentParam(String type, OperationRequest request) {
            if (this.functionHandlers.containsKey(type)) {
                // TODO: handle this situation properly
                return;
            }
            assert this.independentParamHandlers.containsKey(type) : "Invalid parameter " + type;
            this.independentParamHandlers.get(type).accept(request);
        }

        private void isCancellable(OperationRequest request) {
            Cancellable cancellable = cancellables.get(request.getTarget());
            if (cancellable.isRoot()) {
                // This is a root cancellable
                // Parameter is_cancellable is useful only if this cancellable is a root cancellable
                // TODO: Add a warning if this is not a root cancellable
                Boolean isCancellable = (Boolean)request.getParams().get("is_cancellable");
                rootCancellableToCancellableGroup.get(cancellable.getID()).setIsCancellable(isCancellable);
            }
        }

        private void setGroupResource(OperationRequest request) {
            Cancellable cancellable = cancellables.get(request.getTarget());
            // TODO: Problematic point: nullptr
            if (cancellable.isRoot()) {
                Double value = (Double)request.getParams().get("set_group_resource");
                rootCancellableToCancellableGroup.get(cancellable.getID()).setResourceUsage(request.getResourceType(), value);
            }
        }

        private void monitorResource(OperationRequest request) {
            Cancellable cancellable = cancellables.get(request.getTarget());
            if (cancellable.isRoot()) {
                // This is a root cancellable
                // Parameter monitor_resource is useful only if this cancellable is a root cancellable
                // TODO: Add a warning if this is not a root cancellable
                List<?> resourceTypes = (List<?>)request.getParams().get("monitor_resource");
                CancellableGroup cancellableGroup = rootCancellableToCancellableGroup.get(cancellable.getID());
                for (Object resourceType : resourceTypes) {
                    cancellableGroup.setResourceUsage((ResourceType)resourceType, 0.0);
                }
            }
        }

        private void cancellableName(OperationRequest request) {
            Cancellable cancellable = cancellables.get(request.getTarget());
            String name = (String)request.getParams().get("cancellable_name");
            cancellable.setName(name);
        }

        private void addGroupResource(OperationRequest request) {
            Cancellable cancellable = cancellables.get(request.getTarget());
            Double value = (Double)request.getParams().get("add_group_resource");
            rootCancellableToCancellableGroup.get(cancellable.getRootID()).addResourceUsage(request.getResourceType(), value);
        }

        private CancellableID parentCancellableID(OperationRequest request) {
            CancellableID parentID = (CancellableID) request.getParams().get("parent_cancellable_id");
            CancellableID rootID = null;
            if (!parentID.isValid()) {
                // Itself is a root cancellable
                rootID = request.getTarget();
            }
            else {
                rootID = cancellables.get(parentID).getID();
            }
            return rootID;
        }
    }
}
