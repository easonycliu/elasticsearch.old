package org.elasticsearch.autocancel.utils.resource;

import java.util.Map;

import org.elasticsearch.autocancel.utils.logger.Logger;

public class QueueResource extends Resource {

    private Integer triedTasks;

    private Long totalWaitTime;

    private Long totalOccupyTime;

    private Long prevNanoTime;

    public QueueResource(ResourceName resourceName) {
        super(ResourceType.QUEUE, resourceName);
        this.triedTasks = 0;
        this.totalWaitTime = 0L;
        this.totalOccupyTime = 0L;
        this.prevNanoTime = System.nanoTime();
    }

    @Override
    public Double getSlowdown() {
        Double slowdown = 0.0;
        slowdown = Double.valueOf(totalWaitTime) / (System.nanoTime() - this.prevNanoTime);

        return slowdown;
    }

    @Override
    public Double getContentionLevel() {
        Double contentionLevel = 0.0;
        contentionLevel = Double.valueOf(totalWaitTime) / (System.nanoTime() - this.prevNanoTime);

        return contentionLevel;
    }

    @Override
    public Double getResourceUsage() {
        return Double.valueOf(totalOccupyTime) / (System.nanoTime() - this.prevNanoTime);
    }

    // Queue resource update info has keys:
    // wait_time
    // occupy_time
    @Override
    public void setResourceUpdateInfo(Map<String, Object> resourceUpdateInfo) {
        for (Map.Entry<String, Object> entry : resourceUpdateInfo.entrySet()) {
            switch (entry.getKey()) {
                case "wait_time":
                    this.triedTasks += 1;
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
        this.triedTasks = 0;
        this.totalWaitTime = 0L;
        this.totalOccupyTime = 0L;
        this.prevNanoTime = System.nanoTime();
    }

    @Override
    public String toString() {
        return String.format("Resource Type: %s, Name: %s, Tried tasks: %d, Total wait time: %d",
                this.getResourceType().toString(),
                this.getResourceName().toString(),
                this.triedTasks,
                this.totalWaitTime);
    }
}
