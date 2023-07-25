package org.elasticsearch.autocancel.utils;

import org.elasticsearch.autocancel.utils.CancellableID;

import java.util.concurrent.atomic.AtomicLong;

public class CancellableIDGenerator {

    private final static AtomicLong cancellableIDGenerator = new AtomicLong();

    public CancellableID generate() {
        return new CancellableID(cancellableIDGenerator.incrementAndGet());
    }
}
