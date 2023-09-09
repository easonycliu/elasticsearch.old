package org.elasticsearch.autocancel.utils.resource;

import java.util.Arrays;
import java.util.Map;

import org.elasticsearch.autocancel.utils.Settings;
import org.elasticsearch.autocancel.utils.logger.Logger;

import java.util.List;

public abstract class Resource {

    protected final ResourceType resourceType;

    protected final ResourceName resourceName;

    protected static final List<String> acceptedInfoKeywords = Arrays.asList(
            "wait_time", "occupy_time");

    public Resource(ResourceType resourceType, ResourceName resourceName) {
        this.resourceType = resourceType;
        this.resourceName = resourceName;
    }

    public abstract Double getSlowdown();

    public abstract Double getContentionLevel();

    public abstract Double getResourceUsage();

    public ResourceName getResourceName() {
        return this.resourceName;
    }

    public ResourceType getResourceType() {
        return this.resourceType;
    }

    final public static Resource createResource(ResourceType type, ResourceName name) {
        Resource resource = null;
        switch (type) {
            case CPU:
                resource = new CPUResource(name);
                break;
            case MEMORY:
                if (name.equals(ResourceName.MEMORY)) {
                    if ((String)((Map<?, ?>)Settings.getSetting("monitor_physical_resources")).get("MEMORY") == "JVM") {
                        resource = new JVMHeapResource();
                    }
                    else {
                        resource = new EvictableMemoryResource();
                    }
                }
                else {
                    resource = new EvictableMemoryResource(name);
                }
                break;
            case QUEUE:
                resource = new QueueResource(name);
                break;
            case NULL:
            default:
                Logger.systemWarn("Invalid resource type " + type + " when creating resource");
        }
        return resource;
    }

    public abstract void setResourceUpdateInfo(Map<String, Object> resourceUpdateInfo);

    public abstract void reset();

    public abstract String toString();
}
