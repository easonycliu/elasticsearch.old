package org.elasticsearch.autocancel.utils.Resource;

import java.util.Map;

public class LockResource extends Resource {

    private Integer triedTasks;

    private Long totalWaitTime;

    public LockResource(ResourceName resourceName) {
        super(ResourceType.LOCK, resourceName);
    }

    @Override
    public Double getContentionLevel() {
        return 0.0;
    }

    @Override
    public void setResourceUpdateInfo(Map<String, Object> resourceUpdateInfo) {
        Long waitTime = (Long) resourceUpdateInfo.get("wait_time");
        if (waitTime != null) {
            this.triedTasks += 1;
            this.totalWaitTime += waitTime;
        }
    }

    @Override
    public void reset() {
        this.triedTasks = 0;
        this.totalWaitTime = 0L;
    }
}
