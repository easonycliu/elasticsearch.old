package org.elasticsearch.autocancel.infrastructure;

import java.util.HashMap;
import java.util.Map;

import org.elasticsearch.autocancel.utils.Resource.ResourceName;
import org.elasticsearch.autocancel.utils.id.ID;

public abstract class AbstractInfrastructure {
    private Map<ID, ResourceBatch> resourceMap;

    public AbstractInfrastructure() {
        this.resourceMap = new HashMap<ID, ResourceBatch>();
    }

    public Double getResource(ID id, ResourceName resourceName, Integer version) {
        if (this.outOfDate(id, version)) {
            this.updateResource(id, version);
        }
        Double resourceValue = this.getResourceValue(id, resourceName);
        return resourceValue;
    }

    private Boolean outOfDate(ID id, Integer version) {
        Boolean outOfDate;
        if (this.resourceMap.containsKey(id)) {
            if (!this.resourceMap.get(id).getVersion().equals(version)) {
                outOfDate = true;
            } else {
                outOfDate = false;
            }
        } else {
            outOfDate = true;
        }
        return outOfDate;
    }

    protected abstract void updateResource(ID id, Integer version);

    protected void setResourceBatch(ID id, ResourceBatch resourceBatch) {
        this.resourceMap.put(id, resourceBatch);
    }

    private Double getResourceValue(ID id, ResourceName resourceName) {
        Double resource;
        if (this.resourceMap.containsKey(id)) {
            resource = this.resourceMap.get(id).getResourceValue(resourceName);
        } else {
            resource = 0.0;
        }
        return resource;
    }
}
