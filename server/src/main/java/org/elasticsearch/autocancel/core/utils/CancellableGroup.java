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

    private Progress progressTracker;

    private Boolean isCancellable;

    private Boolean exited;

    private Long startTime;

    private Long startTimeNano;

    private Long exitTime;

    private Long exitTimeNano;

    public CancellableGroup(Cancellable root) {
        root.setLevel(0);
        this.root = root;

        this.cancellables = new HashMap<CancellableID, Cancellable>();
        this.cancellables.put(root.getID(), root);
        this.resourcePool = new ResourcePool(false);

        // These are "built-in" monitored resources
        this.resourcePool.addResource(Resource.createResource(ResourceType.CPU, ResourceName.CPU));
        this.resourcePool.addResource(Resource.createResource(ResourceType.MEMORY, ResourceName.MEMORY));

        this.progressTracker = new Progress();

        this.isCancellable = null;

        this.exited = false;

        this.startTime = 0L;

        this.startTimeNano = 0L;

        this.exitTime = 0L;

        this.exitTimeNano = 0L;
    }

    public void exit() {
        this.exited = true;
    }

    public Boolean isExit() {
        return this.exited;
    }

    public Boolean isExpired() {
        Boolean expired = false;
        if (!this.exitTimeNano.equals(0L) && System.nanoTime() - this.exitTimeNano > ((Long) Settings.getSetting("save_history_ms") * 1000000)) {
            expired = true;
        }
        return expired;
    }

    public Set<ResourceName> getResourceNames() {
        return this.resourcePool.getResourceNames();
    }

    public void refreshResourcePool(Map<String, Object> resourceRefreshInfo) {
        CancellableGroup.logger.log("Root " + this.root.toString() + " used resource:");
        this.resourcePool.refreshResources(resourceRefreshInfo, CancellableGroup.logger);
    }

    public void updateResource(ResourceType resourceType, ResourceName resourceName,
            Map<String, Object> resourceUpdateInfo) {
        if (!this.isExit()) {
            if (!this.resourcePool.isResourceExist(resourceName)) {
                this.resourcePool.addResource(Resource.createResource(resourceType, resourceName));
            }
            this.resourcePool.setResourceUpdateInfo(resourceName, resourceUpdateInfo);
        }
    }

    public void updateWork(Map<String, Object> workUpdateInfo) {
        if (!this.isExit()) {
            this.progressTracker.setWorkUpdateInfo(workUpdateInfo);
            System.out.println(String.format("Predict %s exit time %s", this.root.toString(), this.predictRemainTime()));
        }
    }

    public Double getResourceSlowdown(ResourceName resourceName) {
        Double slowdown = 0.0;
        Map<String, Object> cancellableGroupLevelInfo = Map.of(
            "start_time", this.startTime,
            "start_time_nano", this.startTimeNano,
            "exit_time", this.exitTime,
            "exit_time_nano", this.exitTimeNano
        );
        slowdown = this.resourcePool.getSlowdown(resourceName, cancellableGroupLevelInfo);
        // System.out.println(String.format("%s has slowdown %f on resource %s", this.root.toString(), slowdown, resourceName));
        return slowdown;
    }

    public Long getResourceUsage(ResourceName resourceName) {
        Long resourceUsage = 0L;
        resourceUsage = this.resourcePool.getResourceUsage(resourceName);
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

    public Long getExitTime() {
        assert this.exitTime != 0L;
        return this.exitTime;
    }

    public void setExitTime(Long exitTime) {
        assert this.exitTime == 0L : "Exit time has been set, don't set twice";
        this.exitTime = exitTime;
        System.out.println(String.format("Real %s exit time %s", this.root.toString(), this.exitTime));
    }

    public Long getExitTimeNano() {
        assert this.exitTimeNano != 0L;
        return this.exitTimeNano;
    }

    public void setExitTimeNano(Long exitTimeNano) {
        assert this.exitTimeNano == 0L : "Exit time nano has been set, don't set twice";
        this.exitTimeNano = exitTimeNano;
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

    public Long predictRemainTime() {
        assert !this.startTime.equals(0L) : "A task has to be started to predict remain time";
        Long remainTime = 0L;
        if (!this.isExit()) {
            // Which means the cancellable group has not exited yet
            // Or the remain time is natually 0
            remainTime = Double.valueOf(progressTracker.getProgress() * (System.currentTimeMillis() - this.startTime)).longValue();
        }
        return remainTime;
    }

    public Long predictRemainTimeNano() {
        assert !this.startTimeNano.equals(0L) : "A task has to be started to predict remain time";
        Long remainTimeNano = 0L;
        if (!this.isExit()) {
            // Which means the cancellable group has not exited yet
            // Or the remain time is natually 0
            Double progress = progressTracker.getProgress();
            Double remainWork = 1.0 - progress;
            remainTimeNano = Double.valueOf((remainWork / progress) * (System.nanoTime() - this.startTimeNano)).longValue();
        }
        return remainTimeNano;
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
