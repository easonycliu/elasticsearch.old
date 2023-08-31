package org.elasticsearch.autocancel.utils.Resource;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.util.Map;

public class MemoryResource extends Resource {

    public Long heapMemoryUsed;

    public final Long heapMemoryMax;

    public MemoryResource() {
        super(ResourceType.MEMORY, ResourceName.MEMORY);

        this.heapMemoryUsed = 0L;
        this.heapMemoryMax = ManagementFactory.getMemoryMXBean().getHeapMemoryUsage().getMax();
    }

    public MemoryResource(ResourceName resourceName) {
        super(ResourceType.MEMORY, resourceName);

        this.heapMemoryUsed = 0L;
        this.heapMemoryMax = ManagementFactory.getMemoryMXBean().getHeapMemoryUsage().getMax();
    }

    @Override
    public Double getContentionLevel() {
        return Double.valueOf(this.heapMemoryUsed) / this.heapMemoryMax;
    }

    @Override
    public void setResourceUpdateInfo(Map<String, Object> resourceUpdateInfo) {
        this.heapMemoryUsed = ManagementFactory.getMemoryMXBean().getHeapMemoryUsage().getUsed();
    }

    @Override
    public void reset() {
        this.heapMemoryUsed = 0L;
    }

    @Override
    public String toString() {
        return String.format("Resource Type: %s, Name: %s, Heap memory used: %d, Heap memory max: %d", 
        this.getResourceType().toString(),
        this.getResourceName().toString(),
        this.heapMemoryUsed,
        this.heapMemoryMax);
    }
}
