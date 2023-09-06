package org.elasticsearch.autocancel.infrastructure.jvm;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.RuntimeMXBean;
import java.util.Map;

import org.elasticsearch.autocancel.infrastructure.ResourceReader;
import org.elasticsearch.autocancel.utils.id.ID;

public class JavaMemoryReader extends ResourceReader {
    @Override
    public Map<String, Object> readResource(ID id, Integer version) {
        return Map.of();
        // MemoryMXBean m = ManagementFactory.getMemoryMXBean();
        // RuntimeMXBean r = ManagementFactory.getRuntimeMXBean();
        // r.getVmName();
        // ManagementFactory.getMemoryPoolMXBeans().get(0);
        // ManagementFactory.getMemoryManagerMXBeans().get(0);
        // ManagementFactory.getGarbageCollectorMXBeans().get(0);
        // ManagementFactory.getPlatformMBeanServer();
    }
}
