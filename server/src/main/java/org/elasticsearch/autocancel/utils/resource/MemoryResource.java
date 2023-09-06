package org.elasticsearch.autocancel.utils.resource;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.util.Map;

import org.elasticsearch.autocancel.utils.logger.Logger;

public class MemoryResource extends Resource {

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
    public Double getSlowdown() {
        return 0.0;
    }

    @Override
    public Double getResourceUsage() {
        Double resourceUsage = 0.0;
        if (this.totalMemory != 0L) {
            resourceUsage = Double.valueOf(this.totalMemory) / this.totalMemory;
        }
        return resourceUsage;
    }

    // Memory resource update info has keys:
    // total_memory
    // using_memory
    @Override
    public void setResourceUpdateInfo(Map<String, Object> resourceUpdateInfo) {
        for (Map.Entry<String, Object> entry : resourceUpdateInfo.entrySet()) {
            switch (entry.getKey()) {
                case "total_memory":
                    this.totalMemory = (Long) entry.getValue();
                    break;
                case "using_memory":
                    this.usingMemory += (Long) entry.getValue();
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
        this.totalMemory = 0L;
        this.usingMemory = 0L;
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
