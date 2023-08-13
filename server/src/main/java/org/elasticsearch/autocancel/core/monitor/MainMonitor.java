package org.elasticsearch.autocancel.core.monitor;

import org.elasticsearch.autocancel.core.monitor.Monitor;
import org.elasticsearch.autocancel.core.utils.OperationRequest;
import org.elasticsearch.autocancel.core.monitor.CPUMonitor;
import org.elasticsearch.autocancel.core.monitor.MemoryMonitor;
import org.elasticsearch.autocancel.manager.MainManager;
import org.elasticsearch.autocancel.utils.Cancellable;
import org.elasticsearch.autocancel.utils.Resource.ResourceType;
import org.elasticsearch.autocancel.utils.id.CancellableID;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;

public class MainMonitor {
    
    private Queue<OperationRequest> monitorUpdateToCoreBuffer;

    Map<CancellableID, Cancellable> cancellables;

    MainManager mainManager;

    Map<ResourceType, Monitor> monitors;

    public MainMonitor(MainManager mainManager, Map<CancellableID, Cancellable> cancellables) {
        this.monitorUpdateToCoreBuffer = new LinkedList<OperationRequest>();
        this.mainManager = mainManager;
        this.cancellables = cancellables;

        this.monitors = new HashMap<ResourceType, Monitor>();
        this.monitors.put(ResourceType.CPU, new CPUMonitor(this.mainManager));
        this.monitors.put(ResourceType.MEMORY, new MemoryMonitor(this.mainManager));

    }

    public void updateTasksResources() {
        this.mainManager.startNewVersion();
        for (Map.Entry<CancellableID, Cancellable> entry : cancellables.entrySet()) {
            for (ResourceType resourceType : entry.getValue().getResourceTypes()) {
                this.monitorUpdateToCoreBuffer.add(this.monitors.get(resourceType).updateResource(entry.getKey()));
            }
        }
    }

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
