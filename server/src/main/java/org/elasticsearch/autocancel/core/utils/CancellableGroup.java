package org.elasticsearch.autocancel.core.utils;

import org.elasticsearch.autocancel.app.elasticsearch.AutoCancel;
import org.elasticsearch.autocancel.core.utils.Cancellable;
import org.elasticsearch.autocancel.utils.id.CancellableID;
import org.elasticsearch.autocancel.utils.logger.Logger;
import org.elasticsearch.autocancel.utils.resource.CPUResource;
import org.elasticsearch.autocancel.utils.resource.MemoryResource;
import org.elasticsearch.autocancel.utils.resource.Resource;
import org.elasticsearch.autocancel.utils.resource.ResourceName;
import org.elasticsearch.autocancel.utils.resource.ResourceType;
import org.elasticsearch.autocancel.utils.Settings;

import java.util.Map;
import java.util.Collection;
import java.util.HashMap;
import java.util.Set;
import java.util.List;

public class CancellableGroup {

    private static final Logger logger = new Logger("cancellable_group");

    private final Cancellable root;

    private Map<CancellableID, Cancellable> cancellables;

    private ResourcePool resourcePool;

    private Boolean isCancellable;

    private Boolean exited;

    public CancellableGroup(Cancellable root) {
        root.setLevel(0);
        this.root = root;

        this.cancellables = new HashMap<CancellableID, Cancellable>();
        this.cancellables.put(root.getID(), root);
        this.resourcePool = new ResourcePool();

        // These are "built-in" monitored resources
        this.resourcePool.addResource(new CPUResource());
        this.resourcePool.addResource(new MemoryResource());

        this.isCancellable = null;

        this.exited = false;
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
            this.resourcePool.addResource(resourceType, resourceName);
        }
        this.resourcePool.setResourceUpdateInfo(resourceName, resourceUpdateInfo);
    }

    public Double getResourceSlowdown(ResourceName resourceName) {
        return this.resourcePool.getSlowdown(resourceName);
    }

    public Double getResourceUsage(ResourceName resourceName) {
        return this.resourcePool.getResourceUsage(resourceName);
    }

    public Boolean getIsCancellable() {
        assert this.isCancellable != null : "this.isCancellable hasn't been set yet";
        return this.isCancellable;
    }

    public void setIsCancellable(Boolean isCancellable) {
        this.isCancellable = isCancellable;
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
}
