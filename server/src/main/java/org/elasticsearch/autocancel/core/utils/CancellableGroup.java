package org.elasticsearch.autocancel.core.utils;

import org.elasticsearch.autocancel.utils.id.CancellableID;
import org.elasticsearch.autocancel.utils.logger.Logger;
import org.elasticsearch.autocancel.utils.resource.Resource;
import org.elasticsearch.autocancel.utils.resource.ResourceName;
import org.elasticsearch.autocancel.utils.resource.ResourceType;
import org.elasticsearch.autocancel.utils.Settings;

import java.util.Map;
import java.util.Collection;
import java.util.HashMap;
import java.util.Set;

public class CancellableGroup {

    private static final Logger logger = new Logger("cancellable_group");

    private final Cancellable root;

    private Map<CancellableID, Cancellable> cancellables;

    private ResourcePool resourcePool;

    private Boolean isCancellable;

    private Boolean exited;

    private Long startTime;

    private Long startTimeNano;

    public CancellableGroup(Cancellable root) {
        root.setLevel(0);
        this.root = root;

        this.cancellables = new HashMap<CancellableID, Cancellable>();
        this.cancellables.put(root.getID(), root);
        this.resourcePool = new ResourcePool(false);

        // These are "built-in" monitored resources
        this.resourcePool.addResource(Resource.createResource(ResourceType.CPU, ResourceName.CPU));
        this.resourcePool.addResource(Resource.createResource(ResourceType.MEMORY, ResourceName.MEMORY));

        this.isCancellable = null;

        this.exited = false;

        this.startTime = 0L;

        this.startTimeNano = 0L;
    }

    public void exit() {
        this.exited = true;
    }

    public Boolean isExit() {
        return this.exited;
    }

    public Set<ResourceName> getResourceNames() {
        return this.resourcePool.getResourceNames();
    }

    public void refreshResourcePool() {
        CancellableGroup.logger.log("Root " + this.root.toString() + " used resource:");
        this.resourcePool.refreshResources(CancellableGroup.logger);
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
        System.out.println(String.format("%s has slowdown %f on resource %s", this.root.toString(), slowdown, resourceName));
        return slowdown;
    }

    public Long getResourceUsage(ResourceName resourceName) {
        Long resourceUsage = 0L;
        if (!this.isExit()) {
            resourceUsage = this.resourcePool.getResourceUsage(resourceName);
        }
        return resourceUsage;
    }

    public Boolean getIsCancellable() {
        assert this.isCancellable != null : "this.isCancellable hasn't been set yet";
        return this.isCancellable;
    }

    public void setIsCancellable(Boolean isCancellable) {
        this.isCancellable = isCancellable;
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

    public void putCancellable(Cancellable cancellable) {
        assert cancellable.getRootID().equals(this.root.getID())
                : String.format("Putting a cancellable with id %d into a wrong group with root cancellable id %d",
                        cancellable.getID(), this.root.getID());

        assert !this.cancellables.containsKey(cancellable.getID()) : String
                .format("Cancellable %d has been putted into this group %d", cancellable.getID(), this.root.getID());

        Integer level = this.getCancellableLevel(cancellable);
        cancellable.setLevel(level);

        this.cancellables.put(cancellable.getID(), cancellable);
    }

    public Collection<Cancellable> getChildCancellables() {
        return this.cancellables.values();
    }

    public Set<CancellableID> getChildCancellableIDs() {
        return this.cancellables.keySet();
    }

    public CancellableID getRootID() {
        return this.root.getID();
    }

    private Integer getCancellableLevel(Cancellable cancellable) {
        Integer level = 0;
        CancellableID tmp;
        Integer maxLevel = (Integer) Settings.getSetting("max_child_cancellable_level");
        do {
            tmp = cancellable.getParentID();
            if (!tmp.isValid()) {
                // In case someone use this function to calculate the level of root cancellable
                break;
            }
            level += 1;
            if (level > maxLevel) {
                // There must something wrong, untrack it
                // TODO: add warning
                level = -1;
                break;
            }
        } while (!tmp.equals(this.root.getID()));

        return level;
    }

    public static void cancellableGroupLog(String message) {
        CancellableGroup.logger.log(message);
    }
}
