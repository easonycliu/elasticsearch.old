package org.elasticsearch.autocancel.utils.resource;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.elasticsearch.autocancel.utils.Settings;
import org.elasticsearch.autocancel.utils.id.ID;

public class EvictableMemoryResource extends MemoryResource {

    private Long totalEvictTime;

    private Set<ID> cancellableIDSet;

    public EvictableMemoryResource() {
        super();
        this.totalEvictTime = 0L;
        this.cancellableIDSet = new HashSet<ID>();
    }

    public EvictableMemoryResource(ResourceName name) {
        super(name);
        this.totalEvictTime = 0L;
        this.cancellableIDSet = new HashSet<ID>();
    }

    @Override
    public Double getSlowdown(Map<String, Object> slowdownInfo) {
        Double slowdown = 0.0;
        Long startTimeNano = (Long) slowdownInfo.get("start_time_nano");
        if (startTimeNano != null) {
            slowdown = Double.valueOf(this.totalEvictTime) / ((System.nanoTime() - startTimeNano) * this.cancellableIDSet.size());
        }
        return slowdown;
    }

    // Memory resource update info has more keys:
    // evict_time
    @Override
    public void setResourceUpdateInfo(Map<String, Object> resourceUpdateInfo) {
        ID cid = (ID) resourceUpdateInfo.get("cancellable_id");
        if (cid != null) {
            this.cancellableIDSet.add(cid);
            this.totalEvictTime = (long) ((double) Settings.getSetting("resource_usage_decay") * 
            (Long) resourceUpdateInfo.getOrDefault("evict_time", 0L))
                + this.totalEvictTime;
            super.setResourceUpdateInfo(resourceUpdateInfo);
        }
    }
}
