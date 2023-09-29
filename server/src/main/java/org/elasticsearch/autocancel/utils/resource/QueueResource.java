package org.elasticsearch.autocancel.utils.resource;

import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.HashSet;

import org.elasticsearch.autocancel.utils.id.ID;
import org.elasticsearch.autocancel.utils.logger.Logger;

public class QueueResource extends Resource {

    private Map<QueueEvent, Long> totalEventTime;

    private Long currentSystemTime;

    private Long prevSystemTime;

    private Set<ID> cancellableIDSet;

    private Map<QueueEvent, Queue<Long>> queueEventDataPoints;

    public QueueResource(ResourceName resourceName) {
        super(ResourceType.QUEUE, resourceName);
        this.totalEventTime = new HashMap<QueueEvent, Long>();
        // Initiate them to zero to avoid negative value when start and end in a same refresh interval
        this.currentSystemTime = 0L;
        this.prevSystemTime = 0L;
        this.cancellableIDSet = new HashSet<ID>();
        this.queueEventDataPoints = new HashMap<QueueEvent, Queue<Long>>();
        for (QueueEvent event : QueueEvent.values()) {
            this.queueEventDataPoints.put(event, new ArrayDeque<Long>());
        }
    }

    @Override
    public Double getSlowdown(Map<String, Object> slowdownInfo) {
        Double slowdown = 0.0;
        Long startTime = (Long) slowdownInfo.get("start_time_nano");
        Long currentTime = System.nanoTime();
        if (startTime != null && this.cancellableIDSet.size() > 0) {
            slowdown = Double.valueOf(this.totalEventTime.getOrDefault(QueueEvent.QUEUE, 0L)) / 
            ((currentTime - startTime) * this.cancellableIDSet.size());
        }

        return slowdown;
    }

    @Override
    public Long getResourceUsage() {
        return this.totalEventTime.getOrDefault(QueueEvent.OCCUPY, 0L);
    }

    // Queue resource update info has keys:
    // cpu_time_system
    // event
    // start
    @Override
    public void setResourceUpdateInfo(Map<String, Object> resourceUpdateInfo) {
        Long cpuTimeSystem = (Long) resourceUpdateInfo.get("cpu_time_system");
        QueueEvent event = (QueueEvent) resourceUpdateInfo.get("event");
        Boolean start = (Boolean) resourceUpdateInfo.get("start");
        ID cid = (ID) resourceUpdateInfo.get("cancellable_id");
        
        if (
            cpuTimeSystem != null &&
            event != null &&
            start != null &&
            cid != null
        ) {
            this.queueEventDataPoints.computeIfPresent(event, (dataPointKey, dataPointValue) -> {
                if (start) {
                    dataPointValue.add(cpuTimeSystem);
                    if (event.equals(QueueEvent.QUEUE)) {
                        this.cancellableIDSet.add(cid);
                    }
                }
                else {
                    Long startTime = dataPointValue.poll();
                    if (startTime != null) {
                        this.totalEventTime.computeIfPresent(event, (timeKey, timeValue) -> {
                            timeValue += cpuTimeSystem - Math.max(startTime, this.currentSystemTime);
                            return timeValue;
                        });
                    }
                    else {
                        Logger.systemWarn(String.format("Unmatched start - end events in queue resource"));
                    }
                }
                return dataPointValue;
            });
        }
        else {
            Logger.systemWarn(String.format("Is null for cpu_time_system: %b, event: %b, start: %b", 
                cpuTimeSystem == null, 
                event == null, 
                start == null));
        }
    }

    @Override
    public void reset() {

    }

    @Override 
    public void refresh() {
        this.prevSystemTime = this.currentSystemTime;
        this.currentSystemTime = System.nanoTime();
        for (Map.Entry<QueueEvent, Queue<Long>> entry : this.queueEventDataPoints.entrySet()) {
            AtomicLong eventTime = new AtomicLong(this.totalEventTime.getOrDefault(entry.getKey(), 0L));
            entry.getValue().forEach((startTime) -> {
                eventTime.addAndGet(this.currentSystemTime - Math.max(this.prevSystemTime, startTime));
            });
            this.totalEventTime.put(entry.getKey(), eventTime.get());
        }
    }

    @Override
    public String toString() {
        return String.format("Resource Type: %s, Name: %s, Total wait time: %d, Total occupy time: %d",
                this.getResourceType().toString(),
                this.getResourceName().toString(),
                this.totalEventTime.getOrDefault(QueueEvent.QUEUE, 0L),
                this.totalEventTime.getOrDefault(QueueEvent.OCCUPY, 0L));
    }
}
