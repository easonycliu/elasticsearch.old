package org.elasticsearch.autocancel.core.monitor;

import java.util.List;

import org.elasticsearch.autocancel.core.utils.OperationRequest;
import org.elasticsearch.autocancel.utils.id.CancellableID;

public interface Monitor {

    public List<OperationRequest> updateResource(CancellableID cid);
}
