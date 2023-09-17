package org.elasticsearch.autocancel.utils.resource;

import java.util.Map;
import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.HashSet;

import org.elasticsearch.autocancel.utils.logger.Logger;
import org.elasticsearch.autocancel.utils.Settings;
import org.elasticsearch.autocancel.utils.id.ID;

public class CPUResource extends Resource {

    private Long usedSystemTime;

    private Long usedSystemTimeDecay;

    private List<Double> cpuUsageThreads;

    private Set<ID> existedThreadID;

    private List<GarbageCollectorMXBean> gcMXBeans;

    private Long startGCTime;

    public CPUResource() {
        super(ResourceType.CPU, ResourceName.CPU);
        this.usedSystemTime = 0L;
        this.usedSystemTimeDecay = 0L;
        this.cpuUsageThreads = new ArrayList<Double>();
        this.existedThreadID = new HashSet<ID>();
        this.gcMXBeans = ManagementFactory.getGarbageCollectorMXBeans();
        this.startGCTime = this.getTotalGCTime();
    }

    public CPUResource(ResourceName resourceName) {
        super(ResourceType.CPU, resourceName);
        this.usedSystemTime = 0L;
        this.usedSystemTimeDecay = 0L;
        this.cpuUsageThreads = new ArrayList<Double>();
        this.existedThreadID = new HashSet<ID>();
        this.gcMXBeans = ManagementFactory.getGarbageCollectorMXBeans();
        this.startGCTime = this.getTotalGCTime();
    }

    @Override
    public Double getSlowdown(Map<String, Object> slowdownInfo) {
        Double slowdown = 0.0;
        Long startTimeNano = (Long) slowdownInfo.get("start_time_nano");
        if (startTimeNano != null && this.existedThreadID.size() > 0) {
            slowdown = 1.0 - Double.valueOf(this.usedSystemTime + (this.getTotalGCTime() - this.startGCTime) * 1000000 * this.existedThreadID.size()) / ((System.nanoTime() - startTimeNano) * this.existedThreadID.size());
        }
        return slowdown;
    }

    // @Override
    // public Double getContentionLevel() {
    //     Double standard = 0.0;
    //     Double meanCPUUsage = this.cpuUsageThreads.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
    //     Double sumOfPow = this.cpuUsageThreads.stream().mapToDouble((item) -> { 
    //         return Math.pow(item - meanCPUUsage, 2);
    //     }).sum();
        
    //     if (this.cpuUsageThreads.size() > 1) {
    //         standard = Math.sqrt(sumOfPow / (this.cpuUsageThreads.size() - 1));
    //     }
        
    //     return standard;
    // }

    @Override
    public Long getResourceUsage() {
        return this.usedSystemTimeDecay;
    }

    // CPU resource update info has keys:
    // cpu_time_system
    // cpu_time_thread
    // thread_id
    // cpu_usage_thread
    @Override
    public void setResourceUpdateInfo(Map<String, Object> resourceUpdateInfo) {
        for (Map.Entry<String, Object> entry : resourceUpdateInfo.entrySet()) {
            switch (entry.getKey()) {
                case "cpu_time_system":
                    break;
                case "cpu_time_thread":
                    this.usedSystemTimeDecay = (long) Math.ceil((Double) Settings.getSetting("resource_usage_decay") * this.usedSystemTimeDecay) + (Long) entry.getValue();
                    this.usedSystemTime += (Long) entry.getValue();
                    break;
                case "thread_id":
                    if (!this.existedThreadID.contains((ID) entry.getValue())) {
                        this.existedThreadID.add((ID) entry.getValue());
                    }
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
        this.cpuUsageThreads.clear();
    }

    @Override
    public String toString() {
        return String.format("Resource Type: %s, name: %s, used system time: %d, used system time decay: %d",
                this.getResourceType().toString(),
                this.getResourceName().toString(),
                this.usedSystemTime,
                this.usedSystemTimeDecay);
    }

    // This is exactly the same method as JVMHeapResource
    // TODO: Don't right the same code twice
    private Long getTotalGCTime() {
        Long totalGCTime = 0L;
        for (GarbageCollectorMXBean gcMXBean : this.gcMXBeans) {
            totalGCTime += gcMXBean.getCollectionTime();
        }
        return totalGCTime;
    }
}
