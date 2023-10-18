package org.elasticsearch.autocancel.utils;

import org.elasticsearch.autocancel.core.AutoCancelCoreHolder;
import org.elasticsearch.autocancel.core.AutoCancelInfoCenter;
import org.elasticsearch.autocancel.core.policy.BasePolicy;
import org.elasticsearch.autocancel.core.policy.MOOPolicy;
import org.elasticsearch.autocancel.utils.id.CancellableID;
import org.elasticsearch.autocancel.utils.logger.Logger;

public abstract class Policy {
    
    protected static final AutoCancelInfoCenter infoCenter = AutoCancelCoreHolder.getInfoCenter();

    public Policy() {
        
    }

    public abstract Boolean needCancellation();

    public abstract CancellableID getCancelTarget();

    final public static Policy getPolicyBySetting() {
        Policy policy = null;
        switch ((String) Settings.getSetting("default_policy")) {
            case "base_policy":
                policy = new BasePolicy();
                break;
            case "multi_objective_policy":
                policy = new MOOPolicy();
                break;
            default:
                Logger.systemWarn("Cannot find default policy by setting, fall back to base policy");
                policy = new BasePolicy();
                break;
        }
        assert policy != null : "Must have a policy, even default one";
        return policy;
    }

}
