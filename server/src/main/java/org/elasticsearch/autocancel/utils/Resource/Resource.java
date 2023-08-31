package org.elasticsearch.autocancel.utils.Resource;

import java.util.Arrays;
import java.util.Map;
import java.util.List;

public abstract class Resource {

    private final ResourceType resourceType;

    private final ResourceName resourceName;

    protected static final List<String> acceptedInfoKeywords = Arrays.asList(
            "wait_time", "occupy_time");

    public Resource(ResourceType resourceType, ResourceName resourceName) {
        this.resourceType = resourceType;
        this.resourceName = resourceName;
    }

    public abstract Double getContentionLevel();

    public ResourceName getResourceName() {
        return this.resourceName;
    }

    public ResourceType getResourceType() {
        return this.resourceType;
    }

    public abstract void setResourceUpdateInfo(Map<String, Object> resourceUpdateInfo);

    public abstract void reset();

    public abstract String toString();
}
