package org.elasticsearch.autocancel.infrastructure;

import java.lang.management.ManagementFactory;
import java.util.Map;

import org.elasticsearch.autocancel.utils.id.ID;

public abstract class ResourceReader {
	public abstract Map<String, Object> readResource(ID id, Integer version);

	public String getJVMPID() {
		String name = ManagementFactory.getRuntimeMXBean().getName();
		String pid = name.split("@")[0];
		return pid;
	}
}
