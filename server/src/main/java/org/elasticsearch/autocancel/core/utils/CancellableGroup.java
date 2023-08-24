package org.elasticsearch.autocancel.core.utils;

import org.elasticsearch.autocancel.core.utils.Cancellable;
import org.elasticsearch.autocancel.utils.Resource.ResourceType;
import org.elasticsearch.autocancel.utils.id.CancellableID;
import org.elasticsearch.autocancel.utils.Settings;

import java.util.Map;
import java.util.Collection;
import java.util.HashMap;
import java.util.Set;
import java.util.List;

public class CancellableGroup {

    private final Cancellable root;
    
    private Map<CancellableID, Cancellable> cancellables;

    private Map<ResourceType, ResourceUsage> resourceMap;

    private Boolean isCancellable;

    private Boolean exited;

    public CancellableGroup(Cancellable root) {
        root.setLevel(0);
        this.root = root;

        this.cancellables = new HashMap<CancellableID, Cancellable>();
        this.cancellables.put(root.getID(), root);
        this.resourceMap = new HashMap<ResourceType, ResourceUsage>();
        
        // These are "built-in" monitored resources
        this.resourceMap.put(ResourceType.CPU, new ResourceUsage());
        this.resourceMap.put(ResourceType.MEMORY, new ResourceUsage());

        this.isCancellable = null;

        this.exited = false;
    }

    public void exit() {
        this.exited = true;
    }

    public Boolean isExit() {
        return this.exited;
    }

    public Set<ResourceType> getResourceTypes() {
        return this.resourceMap.keySet();
    }

    public void setResourceUsage(ResourceType type, Double usage) {
        if (this.resourceMap.containsKey(type)) {
            this.resourceMap.get(type).setUsage(usage);
        }
        else {
            this.resourceMap.put(type, new ResourceUsage(usage));
        }
    }

    public void addResourceUsage(ResourceType type, Double usageAdd) {
        if (this.resourceMap.containsKey(type)) {
            Double previousUsage = this.resourceMap.get(type).getUsage();
            this.resourceMap.get(type).setUsage(previousUsage + usageAdd);
        }
        else {
            this.resourceMap.put(type, new ResourceUsage(usageAdd));
        }
    }

    public Double getResourceUsage(ResourceType type) {
        Double usage = null;
        if (this.resourceMap.containsKey(type)) {
            usage = this.resourceMap.get(type).getUsage();
        }
        else {
            usage = 0.0;
        }
        return usage;
    }

    public Boolean getIsCancellable() {
        assert this.isCancellable != null : "this.isCancellable hasn't been set yet";
        return this.isCancellable;
    }

    public void setIsCancellable(Boolean isCancellable) {
        this.isCancellable = isCancellable;
    }

    public void putCancellable(Cancellable cancellable) {
        assert cancellable.getRootID().equals(this.root.getID()) : 
            String.format("Putting a cancellable with id %d into a wrong group with root cancellable id %d", cancellable.getID(), this.root.getID());

        assert !this.cancellables.containsKey(cancellable.getID()) : 
            String.format("Cancellable %d has been putted into this group %d", cancellable.getID(), this.root.getID());

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
        } while(!tmp.equals(this.root.getID()));

        return level;
    }
    
}
