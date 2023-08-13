package org.elasticsearch.autocancel.infrastructure;

import org.elasticsearch.autocancel.core.utils.ResourceUsage;
import org.elasticsearch.autocancel.utils.Resource.ResourceType;

import java.util.Map;
import java.util.HashMap;

public class ResourceBatch {
    
    private Integer version;

    private Map<ResourceType, ResourceUsage> resourceMap;

    public ResourceBatch(Integer version) {
        this.version = version;
        this.resourceMap = new HashMap<ResourceType, ResourceUsage>();
    }

    public void setResourceValue(ResourceType type, Double value) {
        this.resourceMap.put(type, new ResourceUsage(value));
    }

    public Double getResourceValue(ResourceType type) {
        Double resource;
        if (this.resourceMap.containsKey(type)) {
            resource = this.resourceMap.get(type).getUsage();
        }
        else {
            resource = 0.0;
        }
        return resource;
    }

    public Integer getVersion() {
        return this.version;
    }
}
