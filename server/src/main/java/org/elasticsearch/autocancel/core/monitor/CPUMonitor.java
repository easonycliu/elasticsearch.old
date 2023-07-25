package org.elasticsearch.autocancel.core.monitor;

import org.elasticsearch.autocancel.core.monitor.Monitor;
import org.elasticsearch.autocancel.manager.MainManager;
import org.elasticsearch.autocancel.utils.CancellableID;

public class CPUMonitor implements Monitor {

    MainManager mainManager;

    public CPUMonitor(MainManager mainManager) {
        this.mainManager = mainManager;

    }

    public void updateResource(CancellableID cid) {

    }

    private Double getResource(CancellableID cid) {
        return 0.0;
    }
}
