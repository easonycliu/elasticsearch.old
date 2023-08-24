package org.elasticsearch.autocancel.core.utils;

import org.elasticsearch.autocancel.core.utils.ResourceUsage;
import org.elasticsearch.autocancel.utils.Resource.ResourceType;
import org.elasticsearch.autocancel.utils.id.CancellableID;

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

    public Cancellable(CancellableID id, CancellableID parentID, CancellableID rootID) {
        this.id = id;
        this.parentID = parentID;
        this.rootID = rootID;
        // Whose level set to -1 will never be tracked
        // Call set level before tracking
        this.level = -1;
        this.name = "Anonymous";
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
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
        return this.id.toString() + " Parent " + this.parentID.toString() + " Root " + this.rootID.toString();
    }
}
