package org.elasticsearch.autocancel.core.utils;

import org.elasticsearch.autocancel.utils.Resource.ResourceName;
import org.elasticsearch.autocancel.utils.logger.Logger;
import org.elasticsearch.autocancel.utils.Resource.Resource;

import java.util.Map;
import java.util.HashMap;

public class ResourcePool {

    private Map<ResourceName, Resource> resources;

    public ResourcePool() {
        this.resources = new HashMap<ResourceName, Resource>();
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

    public Double getContentionLevel(ResourceName resourceName) {
        Double contentionLevel = 0.0;
        if (this.resources.containsKey(resourceName)) {
            contentionLevel = this.resources.get(resourceName).getContentionLevel();
        } else {
            Logger.systemWarn("Cannot find resource " + resourceName.toString());
        }
        return contentionLevel;
    }

    public void setResourceUpdateInfo(ResourceName resourceName, Map<String, Object> resourceUpdateInfo) {
        if (this.resources.containsKey(resourceName)) {
            this.resources.get(resourceName).setResourceUpdateInfo(resourceUpdateInfo);
        } else {
            Logger.systemWarn("Cannot find resource " + resourceName.toString());
        }
    }
}
