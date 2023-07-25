package org.elasticsearch.autocancel.utils;

import org.elasticsearch.autocancel.core.utils.ResourceUsage;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class Cancellable {

    private Map<ResourceType, ResourceUsage> resourceMap;

    public Cancellable() {
        this.resourceMap = new HashMap<ResourceType, ResourceUsage>();
    }

    public Set<ResourceType> getResourceTypes() {
        return resourceMap.keySet();
    }

    public void setResourceUsage(ResourceType type, Double usage) {
        this.resourceMap.get(type).setUsage(usage);
    }
}
