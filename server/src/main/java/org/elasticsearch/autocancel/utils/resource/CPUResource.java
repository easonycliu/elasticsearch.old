package org.elasticsearch.autocancel.utils.resource;

import java.util.Map;
import java.lang.management.GarbageCollectorMXBean;
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

    private List<GarbageCollectorMXBean> gcMXBeans;

    private Long startGCTime;

    private Map<ID, CPUDataPoint> cpuDataPoints;

    private ThreadMXBean threadMXBean;

    public CPUResource() {
        super(ResourceType.CPU, ResourceName.CPU);
        this.totalSystemTime = 0L;
        this.usedSystemTime = 0L;
        this.usedSystemTimeDecay = 0L;
        this.cpuUsageThreads = new ArrayList<Double>();
        this.gcMXBeans = ManagementFactory.getGarbageCollectorMXBeans();
        this.startGCTime = this.getTotalGCTime();
        this.cpuDataPoints = new HashMap<ID, CPUDataPoint>();
        this.threadMXBean = ManagementFactory.getThreadMXBean();
    }

    public CPUResource(ResourceName resourceName) {
        super(ResourceType.CPU, resourceName);
        this.totalSystemTime = 0L;
        this.usedSystemTime = 0L;
        this.usedSystemTimeDecay = 0L;
        this.cpuUsageThreads = new ArrayList<Double>();
        this.gcMXBeans = ManagementFactory.getGarbageCollectorMXBeans();
        this.startGCTime = this.getTotalGCTime();
        this.cpuDataPoints = new HashMap<ID, CPUDataPoint>();
        this.threadMXBean = ManagementFactory.getThreadMXBean();
    }

    @Override
    public Double getSlowdown(Map<String, Object> slowdownInfo) {
        Double slowdown = 0.0;
        if (this.totalSystemTime > 0 && this.cpuDataPoints.size() > 0) {
            slowdown = 1.0 - 
            Double.valueOf(this.usedSystemTime + (this.getTotalGCTime() - this.startGCTime) * 1000000 * this.cpuDataPoints.size()) / 
            this.totalSystemTime;
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

        if (
            cpuTimeSystem != null &&
            cpuTimeThread != null &&
            threadID != null &&
            start != null
        ) {
            if (start) {
                if (this.cpuDataPoints.put(threadID, new CPUDataPoint(cpuTimeSystem, cpuTimeThread, threadID)) != null) {
                    Logger.systemWarn("Different runnables on the same thread simutanously");
                }
            }
            else {
                CPUDataPoint cpuDataPoint = this.cpuDataPoints.remove(threadID);
                if (cpuDataPoint != null) {
                    Long startTimeSystem = cpuDataPoint.getCPUTimeSystem();
                    Long startTimeThread = cpuDataPoint.getCPUTimeThread();
                    this.totalSystemTime += cpuTimeSystem - startTimeSystem;
                    this.usedSystemTime += cpuTimeThread - startTimeThread;
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
                threadID == null,
                start == null));
        }
    }

    @Override
    public void reset() {
        
    }

    @Override 
    public void refresh() {
        this.cpuUsageThreads.clear();
        Long currentSystemTime = System.nanoTime();
        AtomicLong totalSystemTimeAtomic = new AtomicLong(this.totalSystemTime);
        AtomicLong usedSystemTimeAtomic = new AtomicLong(this.usedSystemTime);
        this.cpuDataPoints.forEach((key, value) -> {
            totalSystemTimeAtomic.addAndGet(value.refreshCPUTimeSystem(currentSystemTime));
            usedSystemTimeAtomic.addAndGet(value.refreshCPUTimeThread(threadMXBean.getThreadCpuTime(value.getThreadID().toLong())));
        });
        Long usedSystemTimeTmp = usedSystemTimeAtomic.get();
        this.usedSystemTimeDecay =  (long) ((Double) Settings.getSetting("resource_usage_decay") * this.usedSystemTimeDecay + 
        (usedSystemTimeTmp - this.usedSystemTime));
        this.totalSystemTime = totalSystemTimeAtomic.get();
        this.usedSystemTime = usedSystemTimeTmp;
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

    class CPUDataPoint {

        private Long cpuTimeSystem;

        private Long cpuTimeThread;

        private final ID threadID;

        public CPUDataPoint(Long cpuTimeSystem, Long cpuTimeThread, ID threadID) {
            this.cpuTimeSystem = cpuTimeSystem;
            this.cpuTimeThread = cpuTimeThread;
            this.threadID = threadID;
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

        public ID getThreadID() {
            return this.threadID;
        }
    }
}
