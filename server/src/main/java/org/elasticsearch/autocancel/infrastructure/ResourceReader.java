package org.elasticsearch.autocancel.infrastructure;

import org.elasticsearch.autocancel.utils.id.ID;

import java.lang.management.ManagementFactory;

public abstract class ResourceReader {
    public abstract Double readResource(ID id, Integer version);

    public String getJVMPID() {
        String name = ManagementFactory.getRuntimeMXBean().getName();
        String pid = name.split("@")[0];
        return pid;
    }
}
