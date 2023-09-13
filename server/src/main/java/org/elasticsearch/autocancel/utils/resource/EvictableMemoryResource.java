package org.elasticsearch.autocancel.utils.resource;

import org.elasticsearch.autocancel.utils.logger.Logger;

import java.util.Map;

public class EvictableMemoryResource extends MemoryResource {

    public EvictableMemoryResource() {
        super();
    }

    public EvictableMemoryResource(ResourceName name) {
        super(name);
    }

    @Override
    public Double getSlowdown(Map<String, Object> slowdownInfo) {
        Double slowdown = 0.0;
        // TODO: implement it

        return slowdown;
    }

}
