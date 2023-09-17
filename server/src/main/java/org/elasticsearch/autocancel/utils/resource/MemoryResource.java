package org.elasticsearch.autocancel.utils.resource;

import java.util.Map;

import org.elasticsearch.autocancel.utils.Settings;
import org.elasticsearch.autocancel.utils.logger.Logger;

public abstract class MemoryResource extends Resource {

    public Long usingMemory;

    public Long totalMemory;

    public MemoryResource() {
        super(ResourceType.MEMORY, ResourceName.MEMORY);

        this.totalMemory = 0L;
        this.usingMemory = 0L;
    }

    public MemoryResource(ResourceName resourceName) {
        super(ResourceType.MEMORY, resourceName);

        this.totalMemory = 0L;
        this.usingMemory = 0L;
    }

    @Override
    public Long getResourceUsage() {
        return this.usingMemory;
    }

    // Memory resource update info has keys:
    // total_memory
    // using_memory
    // reuse_memory
    @Override
    public void setResourceUpdateInfo(Map<String, Object> resourceUpdateInfo) {
        for (Map.Entry<String, Object> entry : resourceUpdateInfo.entrySet()) {
            switch (entry.getKey()) {
                case "total_memory":
                    this.totalMemory = (Long) entry.getValue();
                    break;
                case "using_memory":
                    this.usingMemory = (long) Math.ceil((Double) Settings.getSetting("resource_usage_decay") * this.usingMemory) + (Long) entry.getValue();
                    break;
                case "reuse_memory":
                    // TODO: Find a method to get reused memory
                    break;
                default:
                    Logger.systemWarn("Invalid info name " + entry.getKey() + " in resource type " + this.resourceType
                            + " ,name " + this.resourceName);
                    break;
            }
        }
    }

    @Override
    public void reset() {

    }

    @Override
    public String toString() {
        return String.format("Resource Type: %s, name: %s, total memory: %d, using memory: %d",
                this.getResourceType().toString(),
                this.getResourceName().toString(),
                this.totalMemory,
                this.usingMemory);
    }
}
