package org.elasticsearch.autocancel.core.monitor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.elasticsearch.autocancel.core.monitor.Monitor;
import org.elasticsearch.autocancel.core.utils.OperationRequest;
import org.elasticsearch.autocancel.core.utils.OperationMethod;
import org.elasticsearch.autocancel.manager.MainManager;
import org.elasticsearch.autocancel.utils.id.CancellableID;
import org.elasticsearch.autocancel.utils.resource.ResourceName;
import org.elasticsearch.autocancel.utils.resource.ResourceType;

public class MemoryMonitor implements Monitor {

    MainManager mainManager;

    public MemoryMonitor(MainManager mainManager) {
        this.mainManager = mainManager;
    }

    public List<OperationRequest> updateResource(CancellableID cid) {
        List<Map<String, Object>> resourceUpdateInfos = this.getResource(cid);
        List<OperationRequest> requests = new ArrayList<OperationRequest>();
        for (Map<String, Object> resourceUpdateInfo : resourceUpdateInfos) {
            OperationRequest request = new OperationRequest(OperationMethod.UPDATE,
                    Map.of("cancellable_id", cid, "resource_name", ResourceName.MEMORY, "resource_type", ResourceType.MEMORY));
            request.addRequestParam("update_group_resource", resourceUpdateInfo);
            requests.add(request);
        }
        return requests;
    }

    private List<Map<String, Object>> getResource(CancellableID cid) {
        return this.mainManager.getSpecifiedResource(cid, ResourceName.MEMORY);
    }
}
