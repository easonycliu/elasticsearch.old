package org.elasticsearch.autocancel.utils.resource;

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.util.List;

import org.elasticsearch.autocancel.utils.logger.Logger;

public class JVMHeapResource extends MemoryResource {

    private List<GarbageCollectorMXBean> gcMXBeans;

    private Long prevGCTime;

    private Long gcTime;

    private Long prevCPUTime;

    private Long cpuTime;
    
    public JVMHeapResource() {
        super();
        this.gcMXBeans = ManagementFactory.getGarbageCollectorMXBeans();
        this.prevGCTime = 0L;
        this.gcTime = this.getTotalGCTime();
        this.prevCPUTime = 0L;
        this.cpuTime = System.currentTimeMillis();
    }

    @Override
    public Double getSlowdown() {
        Logger.systemWarn("JVM heap resource can't calculate slowdown for a single cancellable group");
        return 0.0;
    }

    @Override
    public Double getContentionLevel() {
        Double contentionLevel = 0.0;
        contentionLevel = Double.valueOf(gcTime - prevGCTime) / (cpuTime - prevCPUTime);

        return contentionLevel;
    }

    private Long getTotalGCTime() {
        Long totalGCTime = 0L;
        for (GarbageCollectorMXBean gcMXBean : this.gcMXBeans) {
            totalGCTime += gcMXBean.getCollectionTime();
        }
        return totalGCTime;
    }

    @Override
    public void reset() {
        super.reset();
        Long tmpGCTime = this.getTotalGCTime();

        // GC is not triggered frequently, so most of the time this.gcTime == tmpGCTime
        // Then it's slowdown is zero
        // sometime this.gcTime get updated, then at that moment the slowdown appoarches 1
        if (tmpGCTime.equals(this.gcTime)) {
            this.cpuTime = System.currentTimeMillis();
        }
        else {
            this.prevGCTime = this.gcTime;
            this.gcTime = tmpGCTime;
            this.prevCPUTime = this.cpuTime;
            this.cpuTime = System.currentTimeMillis();
        }
    }

    @Override
    public String toString() {
        return super.toString() + String.format(", prevGCTime %d, gcTime: %d, prevCPUTime: %d, cpuTime: %d", 
                                                this.prevGCTime,
                                                this.gcTime,
                                                this.prevCPUTime,
                                                this.cpuTime);
    }
}
