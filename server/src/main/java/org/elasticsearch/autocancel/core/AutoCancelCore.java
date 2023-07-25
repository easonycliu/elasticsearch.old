package org.elasticsearch.autocancel.core;

import org.elasticsearch.autocancel.manager.MainManager;
import org.elasticsearch.autocancel.utils.Cancellable;
import org.elasticsearch.autocancel.utils.CancellableID;
import org.elasticsearch.autocancel.core.monitor.MainMonitor;

import java.util.Map;
import java.util.HashMap;
import java.lang.Thread;

public class AutoCancelCore {

    private MainManager mainManager;

    private MainMonitor mainMonitor;

    private Map<CancellableID, Cancellable> cancellables;

    public AutoCancelCore(MainManager mainManager) {
        this.mainManager = mainManager;
        this.cancellables = new HashMap<CancellableID, Cancellable>();
        this.mainMonitor = new MainMonitor(this.mainManager, this.cancellables);

    }

    // public Thread StartCoreOnNewThread() {

    // }
}
