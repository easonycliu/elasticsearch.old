package org.elasticsearch.autocancel.utils.resource;

import java.util.Map;

import org.elasticsearch.autocancel.utils.logger.Logger;

public class QueueResource extends Resource {

    private Long totalWaitTime;

    private Long totalOccupyTime;

    public QueueResource(ResourceName resourceName) {
        super(ResourceType.QUEUE, resourceName);
        this.totalWaitTime = 0L;
        this.totalOccupyTime = 0L;
    }

    @Override
    public Double getSlowdown(Map<String, Object> slowdownInfo) {
        Double slowdown = 0.0;
        Long startTime = (Long) slowdownInfo.get("start_time");
        if (startTime != null) {
            slowdown = Double.valueOf(this.totalWaitTime) / (System.nanoTime() - startTime);
        }

        return slowdown;
    }

    @Override
    public Long getResourceUsage() {
        return this.totalOccupyTime;
    }

    // Queue resource update info has keys:
    // wait_time
    // occupy_time
    @Override
    public void setResourceUpdateInfo(Map<String, Object> resourceUpdateInfo) {
        for (Map.Entry<String, Object> entry : resourceUpdateInfo.entrySet()) {
            switch (entry.getKey()) {
                case "wait_time":
                    this.totalWaitTime += (Long) entry.getValue();
                    break;
                case "occupy_time":
                    this.totalOccupyTime += (Long) entry.getValue();
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

    }

    @Override
    public String toString() {
        return String.format("Resource Type: %s, Name: %s, Total wait time: %d, Total occupy time: %d",
                this.getResourceType().toString(),
                this.getResourceName().toString(),
                this.totalWaitTime,
                this.totalOccupyTime);
    }
}
