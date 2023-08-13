package org.elasticsearch.autocancel.core.monitor;

import org.elasticsearch.autocancel.core.utils.OperationRequest;
import org.elasticsearch.autocancel.utils.id.CancellableID;

public interface Monitor {

    public OperationRequest updateResource(CancellableID cid);
}
