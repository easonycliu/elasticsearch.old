package org.elasticsearch.autocancel.utils.resource;

import java.util.Map;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import org.elasticsearch.autocancel.utils.logger.Logger;
import org.elasticsearch.autocancel.utils.Settings;
import org.elasticsearch.autocancel.utils.id.ID;

public class CPUResource extends Resource {

    private Long totalSystemTime;

    private Long usedSystemTime;

    private Long usedSystemTimeDecay;

    private List<Double> cpuUsageThreads;

    private Long startGCTime;

    private Long currentGCTime;

    private Map<ID, CPUDataPoint> cpuDataPoints;

    private ThreadMXBean threadMXBean;

    public CPUResource() {
        super(ResourceType.CPU, ResourceName.CPU);
        this.totalSystemTime = 0L;
        this.usedSystemTime = 0L;
        this.usedSystemTimeDecay = 0L;
        this.cpuUsageThreads = new ArrayList<Double>();
        this.startGCTime = JVMHeapResource.getTotalGCTime();
        this.currentGCTime = this.startGCTime;
        this.cpuDataPoints = new HashMap<ID, CPUDataPoint>();
        this.threadMXBean = ManagementFactory.getThreadMXBean();
    }

    public CPUResource(ResourceName resourceName) {
        super(ResourceType.CPU, resourceName);
        this.totalSystemTime = 0L;
        this.usedSystemTime = 0L;
        this.usedSystemTimeDecay = 0L;
        this.cpuUsageThreads = new ArrayList<Double>();
        this.startGCTime = JVMHeapResource.getTotalGCTime();
        this.currentGCTime = this.startGCTime;
        this.cpuDataPoints = new HashMap<ID, CPUDataPoint>();
        this.threadMXBean = ManagementFactory.getThreadMXBean();
    }

    @Override
    public Double getSlowdown(Map<String, Object> slowdownInfo) {
        Double slowdown = 0.0;
        if (this.totalSystemTime > 0) {
            slowdown = 1.0 - 
            Double.valueOf(this.usedSystemTime + 
            Math.max(this.currentGCTime - this.startGCTime, 0) * 1000000 * this.cpuDataPoints.size()) / 
            this.totalSystemTime;
            if (this.usedSystemTime > this.totalSystemTime) {
                System.out.println(String.format("Find abnormal slowdown in cpu, used system time: %d, total system time: %d", this.usedSystemTime, this.totalSystemTime));
            }
            // GC Time + used system time may larger than total system time
            // TODO: Find a better way to handle GC time
            slowdown = Math.max(slowdown, 0.0);
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
        Long cpuTimeSystem = (Long) resourceUpdateInfo.get("cpu_time_system");
        Long cpuTimeThread = (Long) resourceUpdateInfo.get("cpu_time_thread");
        ID threadID = (ID) resourceUpdateInfo.get("thread_id");
        Boolean start = (Boolean) resourceUpdateInfo.get("start");
        Double cpuUsageThread = (Double) resourceUpdateInfo.get("cpu_usage_thread");

        if (cpuUsageThread != null) {
            this.cpuUsageThreads.add(cpuUsageThread);
        }

        if (start == null) {
            if (
                cpuTimeSystem != null &&
                cpuTimeThread != null
            ) {
                // this.totalSystemTime += cpuTimeSystem;
                // this.usedSystemTime += cpuTimeThread;
                // this.usedSystemTimeDecay =  (long) ((Double) Settings.getSetting("resource_usage_decay") * this.usedSystemTimeDecay + cpuTimeThread);
            }
        }
        else {
            if (
                cpuTimeSystem != null &&
                cpuTimeThread != null &&
                threadID != null
            ) {
                if (start) {
                    if (this.cpuDataPoints.put(threadID, new CPUDataPoint(cpuTimeSystem, cpuTimeThread)) != null) {
                        Logger.systemWarn("Different runnables on the same thread simutanously");
                    }
                }
                else {
                    CPUDataPoint cpuDataPoint = this.cpuDataPoints.remove(threadID);
                    if (cpuDataPoint != null) {
                        Long prevCPUTimeSystem = cpuDataPoint.getCPUTimeSystem();
                        Long prevCPUTimeThread = cpuDataPoint.getCPUTimeThread();
                        Long startTimeSystem = cpuDataPoint.getStartSystemTime();
                        Long startTimeThread = cpuDataPoint.getStartThreadTime();
                        this.totalSystemTime += cpuTimeSystem - prevCPUTimeSystem;
                        this.usedSystemTime += cpuTimeThread - prevCPUTimeThread;
                        if (cpuTimeThread - startTimeThread > cpuTimeSystem - startTimeSystem) {
                            System.out.println(String.format("Find abnormal metrics in setResourceUpdateInfo, thread used system time: %d, thread total system time: %d", cpuTimeThread - startTimeThread, cpuTimeSystem - startTimeSystem));
                        }
                    }
                    else {
                        Logger.systemWarn(String.format("Unmatched start - end events in cpu resource"));
                    }
                }
            }
            else {
                Logger.systemWarn(String.format("Is null for cpu_time_system: %b, cpu_time_thread: %b, thread_id: %b, start: %b", 
                    cpuTimeSystem == null, 
                    cpuTimeThread == null, 
                    threadID == null));
            }
        }
        
        
    }

    @Override
    public void reset() {
        
    }

    @Override 
    public void refresh(Map<String, Object> refreshInfo) {
        this.cpuUsageThreads.clear();
        AtomicLong totalSystemTimeAtomic = new AtomicLong(this.totalSystemTime);
        AtomicLong usedSystemTimeAtomic = new AtomicLong(this.usedSystemTime);
        this.cpuDataPoints.forEach((key, value) -> {
            Long cpuTimeThreadAdd = threadMXBean.getThreadCpuTime(key.toLong());
            Long cpuTimeSystemAdd = System.nanoTime();
            if (cpuTimeThreadAdd > cpuTimeSystemAdd) {
                System.out.println(String.format("Find abnormal in refresh, cpu time thread add: %d, cpu time system add: %d", cpuTimeThreadAdd, cpuTimeSystemAdd));
            }
            usedSystemTimeAtomic.addAndGet(value.refreshCPUTimeThread(cpuTimeThreadAdd));
            totalSystemTimeAtomic.addAndGet(value.refreshCPUTimeSystem(cpuTimeSystemAdd));
        });
        Long usedSystemTimeTmp = usedSystemTimeAtomic.get();
        this.usedSystemTimeDecay =  (long) ((Double) Settings.getSetting("resource_usage_decay") * this.usedSystemTimeDecay + 
        (usedSystemTimeTmp - this.usedSystemTime));
        this.totalSystemTime = totalSystemTimeAtomic.get();
        this.usedSystemTime = usedSystemTimeTmp;
        
        this.currentGCTime = (Long) refreshInfo.getOrDefault("current_gc_time", this.currentGCTime);
    }

    @Override
    public String toString() {
        return String.format("Resource Type: %s, name: %s, used system time: %d, used system time decay: %d",
                this.getResourceType().toString(),
                this.getResourceName().toString(),
                this.usedSystemTime,
                this.usedSystemTimeDecay);
    }

    class CPUDataPoint {

        private final Long startSystemTime;

        private final Long startThreadTime;

        private Long cpuTimeSystem;

        private Long cpuTimeThread;

        public CPUDataPoint(Long cpuTimeSystem, Long cpuTimeThread) {
            this.startSystemTime = cpuTimeSystem;
            this.startThreadTime = cpuTimeThread;
            this.cpuTimeSystem = cpuTimeSystem;
            this.cpuTimeThread = cpuTimeThread;
        }

        public Long getCPUTimeSystem() {
            return this.cpuTimeSystem;
        }

        public Long refreshCPUTimeSystem(Long cpuTimeSystem) {
            Long addValue = 0L;
            if (cpuTimeSystem > 0) {
                addValue = cpuTimeSystem - this.cpuTimeSystem;
                this.cpuTimeSystem = cpuTimeSystem;
            }
            return addValue;
        }

        public Long getCPUTimeThread() {
            return this.cpuTimeThread;
        }

        public Long refreshCPUTimeThread(Long cpuTimeThread) {
            Long addValue = 0L;
            if (cpuTimeThread > 0) {
                addValue = cpuTimeThread - this.cpuTimeThread;
                this.cpuTimeThread = cpuTimeThread;
            }
            return addValue;
        }

        public Long getStartSystemTime() {
            return this.startSystemTime;
        }

        public Long getStartThreadTime() {
            return this.startThreadTime;
        }
    }
}
