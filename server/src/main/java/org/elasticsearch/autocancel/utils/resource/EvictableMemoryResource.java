package org.elasticsearch.autocancel.utils.resource;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.elasticsearch.autocancel.utils.Settings;
import org.elasticsearch.autocancel.utils.id.ID;

public class EvictableMemoryResource extends MemoryResource {

    private Long totalEvictTime;

    private Set<ID> cancellableIDSet;

    private Long currentTimeNano;

    public EvictableMemoryResource() {
        super();
        this.totalEvictTime = 0L;
        this.cancellableIDSet = new HashSet<ID>();
        this.currentTimeNano = System.nanoTime();
    }

    public EvictableMemoryResource(ResourceName name) {
        super(name);
        this.totalEvictTime = 0L;
        this.cancellableIDSet = new HashSet<ID>();
        this.currentTimeNano = System.nanoTime();
    }

    @Override
    public Double getSlowdown(Map<String, Object> slowdownInfo) {
        Double slowdown = 0.0;
        Long startTimeNano = (Long) slowdownInfo.get("start_time_nano");
        if (startTimeNano != null) {
            slowdown = Double.valueOf(this.totalEvictTime) / (this.currentTimeNano - startTimeNano);
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
            this.totalEvictTime += (Long) resourceUpdateInfo.getOrDefault("evict_time", 0L);
            super.setResourceUpdateInfo(resourceUpdateInfo);
        }
    }

    @Override
    public void refresh(Map<String, Object> refreshInfo) {
        super.refresh(refreshInfo);
        this.currentTimeNano = System.nanoTime();
    }
}
