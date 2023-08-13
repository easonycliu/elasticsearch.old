package org.elasticsearch.autocancel.utils;

import org.elasticsearch.autocancel.core.utils.ResourceUsage;
import org.elasticsearch.autocancel.utils.Resource.ResourceType;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class Cancellable {
    
    private Map<ResourceType, ResourceUsage> resourceMap;

    private String name;

    private Boolean isCancellable;

    public Cancellable() {
        this.resourceMap = new HashMap<ResourceType, ResourceUsage>();
        this.name = "Anonymous";
        this.isCancellable = null;
    }

    public Set<ResourceType> getResourceTypes() {
        return this.resourceMap.keySet();
    }

    public void setResourceUsage(ResourceType type, Double usage) {
        if (this.resourceMap.containsKey(type)) {
            this.resourceMap.get(type).setUsage(usage);
        }
        else {
            this.resourceMap.put(type, new ResourceUsage(usage));
        }
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Boolean getIsCancellable() {
        assert this.isCancellable != null : "this.isCancellable hasn't been set yet";
        return this.isCancellable;
    }

    public void setIsCancellable(Boolean isCancellable) {
        this.isCancellable = isCancellable;
    }
}
