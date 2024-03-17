package org.elasticsearch.autocancel.api;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.util.Map;

import org.elasticsearch.autocancel.manager.MainManager;
import org.elasticsearch.autocancel.utils.id.JavaThreadID;
import org.elasticsearch.autocancel.utils.resource.QueueEvent;
import org.elasticsearch.autocancel.utils.resource.ResourceType;

public class Resource {
	private final MainManager mainManager;

	private final ThreadMXBean threadMXBean;

	public Resource(MainManager mainManager) {
		this.mainManager = mainManager;
		this.threadMXBean = ManagementFactory.getThreadMXBean();
	}

	public void startCPUUsing(String name) {
		Long cpuTimeSystem = System.nanoTime();
		Long cpuTimeThread = threadMXBean.getCurrentThreadCpuTime();
		this.mainManager.updateCancellableGroupResource(ResourceType.CPU, name,
				Map.of("cpu_time_system", cpuTimeSystem, "cpu_time_thread", cpuTimeThread, "thread_id",
						new JavaThreadID(Thread.currentThread().getId()), "start", true));
	}

	public void endCPUUsing(String name) {
		Long cpuTimeThread = threadMXBean.getCurrentThreadCpuTime();
		Long cpuTimeSystem = System.nanoTime();
		this.mainManager.updateCancellableGroupResource(ResourceType.CPU, name,
				Map.of("cpu_time_system", cpuTimeSystem, "cpu_time_thread", cpuTimeThread, "thread_id",
						new JavaThreadID(Thread.currentThread().getId()), "start", false));
	}

	public void addMemoryUsage(String name, Long evictTime, Long totalMemory, Long usingMemory, Long reuseMemory) {
		this.mainManager.updateCancellableGroupResource(ResourceType.MEMORY, name,
				Map.of("total_memory", totalMemory, "using_memory", usingMemory, "reuse_memory", reuseMemory,
						"evict_time", evictTime));
	}

	public void startQueueEvent(String name, QueueEvent event) {
		this.mainManager.updateCancellableGroupResource(
				ResourceType.QUEUE, name, Map.of("cpu_time_system", System.nanoTime(), "event", event, "start", true));
	}

	public void endQueueEvent(String name, QueueEvent event) {
		this.mainManager.updateCancellableGroupResource(
				ResourceType.QUEUE, name, Map.of("cpu_time_system", System.nanoTime(), "event", event, "start", false));
	}
}
