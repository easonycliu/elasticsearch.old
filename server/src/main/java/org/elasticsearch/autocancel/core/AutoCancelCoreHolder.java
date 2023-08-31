package org.elasticsearch.autocancel.core;

public class AutoCancelCoreHolder {
    
    private static final AutoCancelCore autoCancelCore = new AutoCancelCore();

    public static AutoCancelCore getAutoCancelCore() {
        return AutoCancelCoreHolder.autoCancelCore;
    }

    public static AutoCancelInfoCenter getInfoCenter() {
        return AutoCancelCoreHolder.autoCancelCore.getInfoCenter();
    }
}
