package org.elasticsearch.autocancel.core;

import org.elasticsearch.autocancel.core.performance.Performance;
import org.elasticsearch.autocancel.core.utils.Cancellable;
import org.elasticsearch.autocancel.core.utils.CancellableGroup;
import org.elasticsearch.autocancel.core.utils.ResourcePool;
import org.elasticsearch.autocancel.core.utils.ResourceUsage;
import org.elasticsearch.autocancel.utils.id.CancellableID;
import org.elasticsearch.autocancel.utils.resource.ResourceName;

import java.util.Map;
import java.util.HashMap;
import java.util.Set;

public class AutoCancelInfoCenter {
    
    private final Map<CancellableID, CancellableGroup> rootCancellableToCancellableGroup;

    private final Map<CancellableID, Cancellable> cancellables;

    private final ResourcePool systemResourcePool;

    private final Performance performanceMetrix;

    public AutoCancelInfoCenter(Map<CancellableID, CancellableGroup> rootCancellableToCancellableGroup,
                                Map<CancellableID, Cancellable> cancellables,
                                ResourcePool systemResourcePool,
                                Performance performanceMetrix) {
        this.rootCancellableToCancellableGroup = rootCancellableToCancellableGroup;
        this.cancellables = cancellables;
        this.systemResourcePool = systemResourcePool;
        this.performanceMetrix = performanceMetrix;
    }

    public Integer getFinishedTaskNumber() {
        return this.performanceMetrix.getFinishedTaskNumber();
    }

    public Double getResourceContentionLevel(ResourceName resourceName) {
        return this.systemResourcePool.getContentionLevel(resourceName);
    }

    public Map<ResourceName, Double> getContentionLevel() {
        Set<ResourceName> resourceNames = this.systemResourcePool.getResourceNames();
        Map<ResourceName, Double> resourceContentionLevel = new HashMap<ResourceName, Double>();
        for (ResourceName resourceName : resourceNames) {
            resourceContentionLevel.put(resourceName, this.systemResourcePool.getContentionLevel(resourceName));
        }
        return resourceContentionLevel;
    }

    public Map<CancellableID, Double> getCancellableGroupResourceUsage(ResourceName resourceName) {
        Map<CancellableID, Double> cancellableGroupResourceUsage = new HashMap<CancellableID, Double>();
        for (Map.Entry<CancellableID, CancellableGroup> entry : this.rootCancellableToCancellableGroup.entrySet()) {
            cancellableGroupResourceUsage.put(entry.getKey(), entry.getValue().getResourceUsage(resourceName));
        }
        return cancellableGroupResourceUsage;
    }
}
