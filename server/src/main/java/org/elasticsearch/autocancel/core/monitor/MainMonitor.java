package org.elasticsearch.autocancel.core.monitor;

import org.elasticsearch.autocancel.core.utils.Cancellable;
import org.elasticsearch.autocancel.core.utils.CancellableGroup;
import org.elasticsearch.autocancel.core.utils.OperationRequest;
import org.elasticsearch.autocancel.manager.MainManager;
import org.elasticsearch.autocancel.utils.id.CancellableID;
import org.elasticsearch.autocancel.utils.resource.ResourceName;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;

public class MainMonitor {

    private Queue<OperationRequest> monitorUpdateToCoreBuffer;

    private final Map<CancellableID, Cancellable> cancellables;

    private final Map<CancellableID, CancellableGroup> rootCancellableToCancellableGroup;

    private final MainManager mainManager;

    private Map<ResourceName, Monitor> monitors;

    public MainMonitor(MainManager mainManager, Map<CancellableID, Cancellable> cancellables,
            Map<CancellableID, CancellableGroup> rootCancellableToCancellableGroup) {
        this.monitorUpdateToCoreBuffer = new LinkedList<OperationRequest>();
        this.mainManager = mainManager;
        this.cancellables = cancellables;
        this.rootCancellableToCancellableGroup = rootCancellableToCancellableGroup;

        this.monitors = new HashMap<ResourceName, Monitor>();
        this.monitors.put(ResourceName.CPU, new CPUMonitor(this.mainManager));
        this.monitors.put(ResourceName.MEMORY, new MemoryMonitor(this.mainManager));

    }

    public void updateTasksResources() {
        this.mainManager.startNewVersion();
        for (Cancellable cancellable : this.cancellables.values()) {
            assert this.rootCancellableToCancellableGroup.containsKey(cancellable.getRootID())
                    : String.format("Ungrouped cancellable %d", cancellable.getID());
            // TODO: Problematic point: nullptr
            for (ResourceName resourceName : this.rootCancellableToCancellableGroup.get(cancellable.getRootID())
                    .getResourceNames()) {
                if (this.monitors.containsKey(resourceName)) {
                    this.monitorUpdateToCoreBuffer
                            .addAll(this.monitors.get(resourceName).updateResource(cancellable.getID()));
                } else {
                    // Unsupported name in monitor
                    // But may be updated by app
                    // So there is nothing to do
                    // TODO: Add proper checking to make sure this resource is updated by app
                }
            }
        }
    }

    // Objects in autocancel core is owned by a single thread currently
    // So we do not need a lock, at least now
    public OperationRequest getMonitorUpdateToCoreWithoutLock() {
        OperationRequest request;
        request = this.monitorUpdateToCoreBuffer.poll();
        return request;
    }

    public Integer getMonitorUpdateToCoreBufferSizeWithoutLock() {
        Integer size;
        size = this.monitorUpdateToCoreBuffer.size();
        return size;
    }
}
