package org.elasticsearch.autocancel.utils;

import org.elasticsearch.autocancel.core.AutoCancelCoreHolder;
import org.elasticsearch.autocancel.core.AutoCancelInfoCenter;
import org.elasticsearch.autocancel.utils.id.CancellableID;

public abstract class Policy {
    
    protected final AutoCancelInfoCenter infoCenter;

    public Policy() {
        this.infoCenter = AutoCancelCoreHolder.getInfoCenter();
    }

    public abstract Boolean needCancellation();

    public abstract CancellableID getCancelTarget();

}
