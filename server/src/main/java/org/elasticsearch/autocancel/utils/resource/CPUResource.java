package org.elasticsearch.autocancel.utils.resource;

import java.util.Map;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.elasticsearch.autocancel.utils.logger.Logger;

public class CPUResource extends Resource {

    private Long absoluteSystemTime;

    private Long totalSystemTime;

    private Long usedSystemTime;

    private List<Double> cpuUsageThreads;

    public CPUResource() {
        super(ResourceType.CPU, ResourceName.CPU);
        this.absoluteSystemTime = 0L;
        this.totalSystemTime = 0L;
        this.usedSystemTime = 0L;
        this.cpuUsageThreads = new ArrayList<Double>();
    }

    public CPUResource(ResourceName resourceName) {
        super(ResourceType.CPU, resourceName);
        this.absoluteSystemTime = 0L;
        this.totalSystemTime = 0L;
        this.usedSystemTime = 0L;
        this.cpuUsageThreads = new ArrayList<Double>();
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
    public Double getContentionLevel() {
        Double standard = 0.0;
        Double meanCPUUsage = this.cpuUsageThreads.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        Double sumOfPow = this.cpuUsageThreads.stream().mapToDouble((item) -> { 
            return Math.pow(item - meanCPUUsage, 2);
        }).sum();
        
        if (this.cpuUsageThreads.size() > 1) {
            standard = Math.sqrt(sumOfPow / (this.cpuUsageThreads.size() - 1));
        }
        
        return standard;
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
    // cpu_usage_thread
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
                case "cpu_usage_thread":
                    this.cpuUsageThreads.add((Double) entry.getValue());
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
        this.cpuUsageThreads.clear();
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
