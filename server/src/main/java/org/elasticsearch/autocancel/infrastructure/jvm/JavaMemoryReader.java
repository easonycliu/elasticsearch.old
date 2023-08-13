package org.elasticsearch.autocancel.infrastructure.jvm;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.RuntimeMXBean;

import org.elasticsearch.autocancel.infrastructure.ResourceReader;
import org.elasticsearch.autocancel.utils.id.ID;

public class JavaMemoryReader extends ResourceReader {
    @Override
    public Double readResource(ID id, Integer version) {
        return 0.0;
        // MemoryMXBean m = ManagementFactory.getMemoryMXBean();
        // RuntimeMXBean r = ManagementFactory.getRuntimeMXBean();
        // r.getVmName();
        // ManagementFactory.getMemoryPoolMXBeans().get(0);
        // ManagementFactory.getMemoryManagerMXBeans().get(0);
        // ManagementFactory.getGarbageCollectorMXBeans().get(0);
        // ManagementFactory.getPlatformMBeanServer();
    }
}
