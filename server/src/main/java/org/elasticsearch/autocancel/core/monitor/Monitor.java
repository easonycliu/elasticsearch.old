package org.elasticsearch.autocancel.core.monitor;

import org.elasticsearch.autocancel.utils.CancellableID;

public interface Monitor {

    public void updateResource(CancellableID cid);
}
