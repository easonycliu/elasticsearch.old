package org.elasticsearch.autocancel.infrastructure;

import org.elasticsearch.autocancel.core.utils.ResourceUsage;
import org.elasticsearch.autocancel.utils.Resource.ResourceName;

import java.util.Map;
import java.util.HashMap;

public class ResourceBatch {

    private Integer version;

    private Map<ResourceName, ResourceUsage> resourceMap;

    public ResourceBatch(Integer version) {
        this.version = version;
        this.resourceMap = new HashMap<ResourceName, ResourceUsage>();
    }

    public void setResourceValue(ResourceName resourceName, Double value) {
        this.resourceMap.put(resourceName, new ResourceUsage(value));
    }

    public Double getResourceValue(ResourceName resourceName) {
        Double resource;
        if (this.resourceMap.containsKey(resourceName)) {
            resource = this.resourceMap.get(resourceName).getUsage();
        } else {
            resource = 0.0;
        }
        return resource;
    }

    public Integer getVersion() {
        return this.version;
    }
}
