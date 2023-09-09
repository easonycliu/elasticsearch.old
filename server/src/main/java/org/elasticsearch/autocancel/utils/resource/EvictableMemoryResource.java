package org.elasticsearch.autocancel.utils.resource;

import org.elasticsearch.autocancel.utils.logger.Logger;

public class EvictableMemoryResource extends MemoryResource {

    public EvictableMemoryResource() {
        super();
    }

    public EvictableMemoryResource(ResourceName name) {
        super(name);
    }

    @Override
    public Double getSlowdown() {
        Double slowdown = 0.0;
        // TODO: implement it

        return slowdown;
    }

    @Override
    public Double getContentionLevel() {
        Double contentionLevel = 0.0;
        // TODO: implement it

        return contentionLevel;
    }

}
