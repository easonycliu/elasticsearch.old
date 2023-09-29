package org.elasticsearch.autocancel.core.utils;

import org.elasticsearch.autocancel.utils.logger.Logger;
import org.elasticsearch.autocancel.utils.resource.Resource;
import org.elasticsearch.autocancel.utils.resource.ResourceName;

import java.util.Map;
import java.util.HashMap;
import java.util.Set;

public class ResourcePool {

    private Map<ResourceName, Resource> resources;

    private final Boolean global;

    public ResourcePool(Boolean global) {
        this.resources = new HashMap<ResourceName, Resource>();
        this.global = global;
    }

    public void addResource(Resource resource) {
        if (!this.resources.containsKey(resource.getResourceName())) {
            this.resources.put(resource.getResourceName(), resource);
        } else {
            Logger.systemWarn(
                    "Resource " + resource.getResourceName().toString() + " has added to resource pool, skip");
        }
    }

    public Boolean isResourceExist(ResourceName resourceName) {
        return this.resources.containsKey(resourceName);
    }

    public Double getSlowdown(ResourceName resourceName, Map<String, Object> cancellableGroupLevelInfo) {
        Double slowDown = 0.0;
        if (!this.global) {
            if (this.resources.containsKey(resourceName)) {
                slowDown = this.resources.get(resourceName).getSlowdown(cancellableGroupLevelInfo);
            } else {
                Logger.systemWarn("Cannot find resource " + resourceName.toString());
            }
        }
        else {
            Logger.systemWarn("Global resource pool shouldn't use getSlowdown");
        }
        return slowDown;
    }

    public Long getResourceUsage(ResourceName resourceName) {
        Long resourceUsage = 0L;
        if (this.resources.containsKey(resourceName)) {
            resourceUsage = this.resources.get(resourceName).getResourceUsage();
        }
        else {
            Logger.systemWarn("Cannot find resource " + resourceName.toString());
        }
        return resourceUsage;
    }

    public void setResourceUpdateInfo(ResourceName resourceName, Map<String, Object> resourceUpdateInfo) {
        if (this.resources.containsKey(resourceName)) {
            this.resources.get(resourceName).setResourceUpdateInfo(resourceUpdateInfo);
        } else {
            Logger.systemWarn("Cannot find resource " + resourceName.toString());
        }
    }

    public void refreshResources(Logger logger) {
        for (Map.Entry<ResourceName, Resource> entries : this.resources.entrySet()) {
            if (logger != null) {
                logger.log(entries.getValue().toString());
            }
            entries.getValue().refresh();
        }
    }

    public Set<ResourceName> getResourceNames() {
        return this.resources.keySet();
    }
}
