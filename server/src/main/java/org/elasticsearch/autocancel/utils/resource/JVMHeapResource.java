package org.elasticsearch.autocancel.utils.resource;

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.util.List;
import java.util.Map;

public class JVMHeapResource extends MemoryResource {

    private List<GarbageCollectorMXBean> gcMXBeans;

    private Long startGCTime;
    
    public JVMHeapResource() {
        super();
        this.gcMXBeans = ManagementFactory.getGarbageCollectorMXBeans();
        this.startGCTime = this.getTotalGCTime();
    }

    @Override
    public Double getSlowdown(Map<String, Object> slowdownInfo) {
        Double slowdown = 0.0;
        Long startTime = (Long) slowdownInfo.get("start_time");
        if (startTime != null) {
            slowdown = Double.valueOf(this.getTotalGCTime() - this.startGCTime) / (System.currentTimeMillis() - startTime);
        }
        return slowdown;
    }

    private Long getTotalGCTime() {
        Long totalGCTime = 0L;
        for (GarbageCollectorMXBean gcMXBean : this.gcMXBeans) {
            totalGCTime += gcMXBean.getCollectionTime();
        }
        return totalGCTime;
    }

    @Override
    public String toString() {
        return super.toString() + String.format(", startGCTime %d", this.startGCTime);
    }
}
