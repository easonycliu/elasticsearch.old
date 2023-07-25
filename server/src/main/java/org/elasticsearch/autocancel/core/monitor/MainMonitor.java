package org.elasticsearch.autocancel.core.monitor;

import org.elasticsearch.autocancel.core.monitor.Monitor;
import org.elasticsearch.autocancel.core.monitor.CPUMonitor;
import org.elasticsearch.autocancel.core.monitor.MemoryMonitor;
import org.elasticsearch.autocancel.manager.MainManager;
import org.elasticsearch.autocancel.utils.Cancellable;
import org.elasticsearch.autocancel.utils.CancellableID;
import org.elasticsearch.autocancel.utils.ResourceType;

import java.util.HashMap;
import java.util.Map;

public class MainMonitor {

    Map<CancellableID, Cancellable> cancellables;

    MainManager mainManager;

    Map<ResourceType, Monitor> monitors;

    public MainMonitor(MainManager mainManager, Map<CancellableID, Cancellable> cancellables) {
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
                this.monitors.get(resourceType).updateResource(entry.getKey());
            }
        }
    }
}
