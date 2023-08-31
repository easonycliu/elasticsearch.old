package org.elasticsearch.autocancel.utils.Resource;

import java.util.Map;
import java.lang.management.ManagementFactory;

public class CPUResource extends Resource {

    public Double systemLoadAverage;

    public CPUResource() {
        super(ResourceType.CPU, ResourceName.CPU);
        this.systemLoadAverage = 0.0;
    }

    public CPUResource(ResourceName resourceName) {
        super(ResourceType.CPU, resourceName);
        this.systemLoadAverage = 0.0;
    }

    @Override
    public Double getContentionLevel() {
        return systemLoadAverage;
    }

    @Override
    public void setResourceUpdateInfo(Map<String, Object> resourceUpdateInfo) {
        this.systemLoadAverage = ManagementFactory.getOperatingSystemMXBean().getSystemLoadAverage();
    }

    @Override
    public void reset() {
        this.systemLoadAverage = 0.0;
    }

    @Override
    public String toString() {
        return String.format("Resource Type: %s, Name: %s, System load average: %f", 
        this.getResourceType().toString(),
        this.getResourceName().toString(),
        this.systemLoadAverage);
    }
}
