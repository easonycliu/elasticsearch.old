package org.elasticsearch.autocancel.core.monitor;

import org.elasticsearch.autocancel.core.monitor.Monitor;
import org.elasticsearch.autocancel.core.utils.Cancellable;
import org.elasticsearch.autocancel.core.utils.CancellableGroup;
import org.elasticsearch.autocancel.core.utils.OperationRequest;
import org.elasticsearch.autocancel.core.monitor.CPUMonitor;
import org.elasticsearch.autocancel.core.monitor.MemoryMonitor;
import org.elasticsearch.autocancel.manager.MainManager;
import org.elasticsearch.autocancel.utils.Resource.ResourceType;
import org.elasticsearch.autocancel.utils.id.CancellableID;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

public class MainMonitor {
    
    private Queue<OperationRequest> monitorUpdateToCoreBuffer;

    private final Map<CancellableID, Cancellable> cancellables;

    private final Map<CancellableID, CancellableGroup> rootCancellableToCancellableGroup;

    private final MainManager mainManager;

    private Map<ResourceType, Monitor> monitors;

    public MainMonitor(MainManager mainManager, Map<CancellableID, Cancellable> cancellables, Map<CancellableID, CancellableGroup> rootCancellableToCancellableGroup) {
        this.monitorUpdateToCoreBuffer = new LinkedList<OperationRequest>();
        this.mainManager = mainManager;
        this.cancellables = cancellables;
        this.rootCancellableToCancellableGroup = rootCancellableToCancellableGroup;

        this.monitors = new HashMap<ResourceType, Monitor>();
        this.monitors.put(ResourceType.CPU, new CPUMonitor(this.mainManager));
        this.monitors.put(ResourceType.MEMORY, new MemoryMonitor(this.mainManager));

    }

    public void updateTasksResources() {
        this.mainManager.startNewVersion();
        for (Cancellable cancellable : this.cancellables.values()) {
            assert this.rootCancellableToCancellableGroup.containsKey(cancellable.getRootID()) : String.format("Ungrouped cancellable %d", cancellable.getID());
            // TODO: Problematic point: nullptr
            for (ResourceType resourceType : this.rootCancellableToCancellableGroup.get(cancellable.getRootID()).getResourceTypes()) {
                this.monitorUpdateToCoreBuffer.add(this.monitors.get(resourceType).updateResource(cancellable.getID()));
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
