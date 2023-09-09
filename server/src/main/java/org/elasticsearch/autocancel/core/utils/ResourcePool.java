package org.elasticsearch.autocancel.core.utils;

import org.elasticsearch.autocancel.utils.logger.Logger;
import org.elasticsearch.autocancel.utils.resource.CPUResource;
import org.elasticsearch.autocancel.utils.resource.MemoryResource;
import org.elasticsearch.autocancel.utils.resource.QueueResource;
import org.elasticsearch.autocancel.utils.resource.Resource;
import org.elasticsearch.autocancel.utils.resource.ResourceName;
import org.elasticsearch.autocancel.utils.resource.ResourceType;

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

    public Double getSlowdown(ResourceName resourceName) {
        Double slowDown = 0.0;
        if (!this.global) {
            if (this.resources.containsKey(resourceName)) {
                slowDown = this.resources.get(resourceName).getSlowdown();
            } else {
                Logger.systemWarn("Cannot find resource " + resourceName.toString());
            }
        }
        else {
            Logger.systemWarn("Global resource pool should use getContentionLevel instead of getSlowdown");
        }
        return slowDown;
    }

    public Double getContentionLevel(ResourceName resourceName) {
        Double contentionLevel = 0.0;
        if (this.global) {
            if (this.resources.containsKey(resourceName)) {
                contentionLevel = this.resources.get(resourceName).getContentionLevel();
            } else {
                Logger.systemWarn("Cannot find resource " + resourceName.toString());
            }
        }
        else {
            Logger.systemWarn("Only global resource pool can use getContentionLevel, use getSlowdown instead");
        }
        return contentionLevel;
    }

    public Double getResourceUsage(ResourceName resourceName) {
        Double resourceUsage = 0.0;
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
            entries.getValue().reset();
        }
    }

    public Set<ResourceName> getResourceNames() {
        return this.resources.keySet();
    }
}
