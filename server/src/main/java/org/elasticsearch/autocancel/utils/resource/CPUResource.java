package org.elasticsearch.autocancel.utils.resource;

import java.util.Map;

import org.elasticsearch.autocancel.utils.logger.Logger;

public class CPUResource extends Resource {

    public Long absoluteSystemTime;

    public Long totalSystemTime;

    public Long usedSystemTime;

    public CPUResource() {
        super(ResourceType.CPU, ResourceName.CPU);
        this.absoluteSystemTime = 0L;
        this.totalSystemTime = 0L;
        this.usedSystemTime = 0L;
    }

    public CPUResource(ResourceName resourceName) {
        super(ResourceType.CPU, resourceName);
        this.absoluteSystemTime = 0L;
        this.totalSystemTime = 0L;
        this.usedSystemTime = 0L;
    }

    @Override
    public Double getSlowdown() {
        Double slowdown = 0.0;
        if (this.totalSystemTime != 0L) {
            slowdown = 1.0 - Double.valueOf(this.usedSystemTime) / this.totalSystemTime;
        }
        return slowdown;
    }

    @Override
    public Double getResourceUsage() {
        Double resourceUsage = 0.0;
        if (this.absoluteSystemTime != 0L) {
            resourceUsage = Double.valueOf(this.usedSystemTime) / this.absoluteSystemTime;
        }
        return resourceUsage;
    }

    // CPU resource update info has keys:
    // cpu_time_system
    // cpu_time_thread
    @Override
    public void setResourceUpdateInfo(Map<String, Object> resourceUpdateInfo) {
        for (Map.Entry<String, Object> entry : resourceUpdateInfo.entrySet()) {
            switch (entry.getKey()) {
                case "cpu_time_system":
                    this.absoluteSystemTime = (Long) entry.getValue();
                    this.totalSystemTime += this.absoluteSystemTime;
                    break;
                case "cpu_time_thread":
                    this.usedSystemTime += (Long) entry.getValue();
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
        this.absoluteSystemTime = 0L;
        this.totalSystemTime = 0L;
        this.usedSystemTime = 0L;
    }

    @Override
    public String toString() {
        return String.format("Resource Type: %s, name: %s, absolute, total, used system time: %d, %d, %d",
                this.getResourceType().toString(),
                this.getResourceName().toString(),
                this.absoluteSystemTime,
                this.totalSystemTime,
                this.usedSystemTime);
    }
}
