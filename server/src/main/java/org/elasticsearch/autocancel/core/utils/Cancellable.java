package org.elasticsearch.autocancel.core.utils;

import org.elasticsearch.autocancel.core.utils.ResourceUsage;
import org.elasticsearch.autocancel.utils.id.CancellableID;
import org.elasticsearch.autocancel.utils.resource.Resource;
import org.elasticsearch.autocancel.utils.resource.ResourceName;
import org.elasticsearch.autocancel.utils.resource.ResourceType;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/*
 * root cancellable: id = id, parentID = -1, rootID = id
 * others: id = id, parentID = parentID, rootID = rootID
 */
public class Cancellable {

    private final CancellableID id;

    private final CancellableID parentID;

    private final CancellableID rootID;

    private Integer level;

    private String name;

    private String action;

    private Boolean exited;

    private Long startTime;

    private Long startTimeNano;

    private ResourcePool resourcePool;

    public Cancellable(CancellableID id, CancellableID parentID, CancellableID rootID) {
        this.id = id;
        this.parentID = parentID;
        this.rootID = rootID;
        // Whose level set to -1 will never be tracked
        // Call set level before tracking
        this.level = -1;
        this.name = "Anonymous";
        this.action = "Unknown";

        this.exited = false;
        
        this.startTime = 0L;
        this.startTimeNano = 0L;

        this.resourcePool = new ResourcePool(false);

        // These are "built-in" monitored resources
        this.resourcePool.addResource(Resource.createResource(ResourceType.CPU, ResourceName.CPU));
        this.resourcePool.addResource(Resource.createResource(ResourceType.MEMORY, ResourceName.MEMORY));
    }

    public void exit() {
        this.exited = true;
    }

    public Boolean isExit() {
        return this.exited;
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAction() {
        return this.action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public Integer getLevel() {
        return this.level;
    }

    public void setLevel(Integer level) {
        this.level = level;
    }

    public CancellableID getID() {
        return this.id;
    }

    public CancellableID getParentID() {
        return this.parentID;
    }

    public CancellableID getRootID() {
        return this.rootID;
    }

    public Boolean isRoot() {
        return !this.parentID.isValid();
    }

    public Long getStartTime() {
        assert this.startTime != 0L;
        return this.startTime;
    }

    public void setStartTime(Long startTime) {
        assert this.startTime == 0L : "Start time has been set, don't set twice";
        this.startTime = startTime;
    }

    public Long getStartTimeNano() {
        assert this.startTimeNano != 0L;
        return this.startTimeNano;
    }

    public void setStartTimeNano(Long startTimeNano) {
        assert this.startTimeNano == 0L : "Start time nano has been set, don't set twice";
        this.startTimeNano = startTimeNano;
    }

    public void refreshResourcePool() {
        this.resourcePool.refreshResources(null);
    }

    public void updateResource(ResourceType resourceType, ResourceName resourceName,
            Map<String, Object> resourceUpdateInfo) {
        if (!this.resourcePool.isResourceExist(resourceName)) {
            this.resourcePool.addResource(Resource.createResource(resourceType, resourceName));
        }
        this.resourcePool.setResourceUpdateInfo(resourceName, resourceUpdateInfo);
    }

    public Double getResourceSlowdown(ResourceName resourceName) {
        Double slowdown = 0.0;
        if (!this.isExit()) {
            Map<String, Object> cancellableGroupLevelInfo = Map.of(
                "start_time", this.startTime,
                "start_time_nano", this.startTimeNano
            );
            slowdown = this.resourcePool.getSlowdown(resourceName, cancellableGroupLevelInfo);
        }
        return slowdown;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Cancellable && ((Cancellable) o).getID().equals(this.id);
    }

    @Override
    public int hashCode() {
        return this.id.hashCode();
    }

    @Override
    public String toString() {
        return this.id.toString() + " Parent " + this.parentID.toString() + " Root " + this.rootID.toString() + " name " + this.name;
    }
}
