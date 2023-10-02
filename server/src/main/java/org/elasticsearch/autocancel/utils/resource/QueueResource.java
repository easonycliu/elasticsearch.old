package org.elasticsearch.autocancel.utils.resource;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import org.elasticsearch.autocancel.utils.id.ID;
import org.elasticsearch.autocancel.utils.logger.Logger;

public class QueueResource extends Resource {

    private Map<QueueEvent, Long> totalEventTime;

    private Set<ID> cancellableIDSet;

    private Map<QueueEvent, List<Long>> queueEventDataPoints;

    public QueueResource(ResourceName resourceName) {
        super(ResourceType.QUEUE, resourceName);
        this.totalEventTime = new HashMap<QueueEvent, Long>();
        for (QueueEvent event : QueueEvent.values()) {
            this.totalEventTime.put(event, 0L);
        }
        // Initiate them to zero to avoid negative value when start and end in a same refresh interval
        this.cancellableIDSet = new HashSet<ID>();
        this.queueEventDataPoints = new HashMap<QueueEvent, List<Long>>();
        for (QueueEvent event : QueueEvent.values()) {
            this.queueEventDataPoints.put(event, new ArrayList<Long>());
        }
    }

    @Override
    public Double getSlowdown(Map<String, Object> slowdownInfo) {
        Double slowdown = 0.0;
        Long startTime = (Long) slowdownInfo.get("start_time_nano");
        Long currentTime = System.nanoTime();
        if (startTime != null && this.cancellableIDSet.size() > 0) {
            slowdown = Double.valueOf(this.totalEventTime.get(QueueEvent.QUEUE)) / 
            ((currentTime - startTime) * this.cancellableIDSet.size());
        }

        return slowdown;
    }

    @Override
    public Long getResourceUsage() {
        return this.totalEventTime.get(QueueEvent.OCCUPY);
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
                    try {
                        Long startTime = dataPointValue.remove(0);
                        this.totalEventTime.computeIfPresent(event, (timeKey, timeValue) -> {
                            Long addValue = cpuTimeSystem - startTime;
                            timeValue += addValue;
                            return timeValue;
                        });
                    }
                    catch (IndexOutOfBoundsException e) {
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
        Long currentSystemTime = System.nanoTime();
        for (Map.Entry<QueueEvent, List<Long>> entry : this.queueEventDataPoints.entrySet()) {
            AtomicLong eventTime = new AtomicLong(this.totalEventTime.get(entry.getKey()));
            entry.getValue().replaceAll((startTime) -> {
                Long addValue = currentSystemTime - startTime;
                eventTime.addAndGet(addValue);
                return currentSystemTime;
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
