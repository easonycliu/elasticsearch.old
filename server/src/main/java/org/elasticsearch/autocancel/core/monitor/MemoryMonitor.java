package org.elasticsearch.autocancel.core.monitor;

import org.elasticsearch.autocancel.core.monitor.Monitor;
import org.elasticsearch.autocancel.core.utils.OperationRequest;
import org.elasticsearch.autocancel.core.utils.OperationMethod;
import org.elasticsearch.autocancel.manager.MainManager;
import org.elasticsearch.autocancel.utils.Resource.ResourceType;
import org.elasticsearch.autocancel.utils.id.CancellableID;

public class MemoryMonitor implements Monitor {

    MainManager mainManager;

    public MemoryMonitor(MainManager mainManager) {
        this.mainManager = mainManager;
    }

    public OperationRequest updateResource(CancellableID cid) {
        OperationRequest request = new OperationRequest(OperationMethod.UPDATE, cid, ResourceType.MEMORY);
        request.addRequestParam("add_group_resource", this.getResource(cid));
        return request;
    }

    private Double getResource(CancellableID cid) {
        return this.mainManager.getSpecifiedTypeResource(cid, ResourceType.MEMORY);
    }
}
