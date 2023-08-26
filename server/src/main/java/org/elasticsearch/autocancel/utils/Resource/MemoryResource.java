package org.elasticsearch.autocancel.utils.Resource;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.util.Map;

public class MemoryResource extends Resource {

    public MemoryResource() {
        super(ResourceType.MEMORY, ResourceName.MEMORY);
    }

    public MemoryResource(ResourceName resourceName) {
        super(ResourceType.MEMORY, resourceName);
    }

    @Override
    public Double getContentionLevel() {
        MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();
        MemoryUsage heapMemoryUsage = memoryMXBean.getHeapMemoryUsage();
        return Double.valueOf(heapMemoryUsage.getUsed()) / heapMemoryUsage.getMax();
    }

    @Override
    public void setResourceUpdateInfo(Map<String, Object> resourceUpdateInfo) {

    }

    @Override
    public void reset() {

    }
}
