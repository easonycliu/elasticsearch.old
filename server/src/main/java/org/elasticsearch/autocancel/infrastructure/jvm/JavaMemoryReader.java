package org.elasticsearch.autocancel.infrastructure.jvm;

import java.lang.management.ManagementFactory;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeSet;

import org.elasticsearch.autocancel.infrastructure.ResourceReader;
import org.elasticsearch.autocancel.utils.id.ID;
import org.elasticsearch.autocancel.utils.id.JavaThreadID;
import org.elasticsearch.autocancel.utils.logger.Logger;

public class JavaMemoryReader extends ResourceReader {
	private com.sun.management.ThreadMXBean sunThreadMXBean;

	private Map<JavaThreadID, Long> javaThreadHeapUsing;

	private Long totalMemory;

	private Integer version;

	public JavaMemoryReader() {
		java.lang.management.ThreadMXBean javaThreadMXBean = ManagementFactory.getThreadMXBean();
		if (javaThreadMXBean instanceof com.sun.management.ThreadMXBean) {
			this.sunThreadMXBean = (com.sun.management.ThreadMXBean) javaThreadMXBean;
			if (this.sunThreadMXBean.isThreadAllocatedMemorySupported()) {
				if (!this.sunThreadMXBean.isThreadAllocatedMemoryEnabled()) {
					this.sunThreadMXBean.setThreadAllocatedMemoryEnabled(true);
				}
			} else {
				Logger.systemWarn(
						"Unsupported method getThreadAllocatedBytes() in class com.sun.management.ThreadMXBean");
			}
		} else {
			this.sunThreadMXBean = null;
			Logger.systemWarn("Unsupported class com.sun.management.ThreadMXBean");
		}
		this.javaThreadHeapUsing = new HashMap<JavaThreadID, Long>();
		this.totalMemory = ManagementFactory.getMemoryMXBean().getHeapMemoryUsage().getMax();
		this.version = 0;
	}

	@Override
	public Map<String, Object> readResource(ID id, Integer version) {
		assert id instanceof JavaThreadID : "Java Memory reader must recieve java thread id";
		if (this.outOfDate(version)) {
			this.refresh(version);
		}
		Long usingMemory = 0L;
		if (this.javaThreadHeapUsing.containsKey((JavaThreadID) id)) {
			usingMemory = this.javaThreadHeapUsing.get((JavaThreadID) id);
		}
		return Map.of("total_memory", this.totalMemory, "using_memory", usingMemory);
	}

	private Boolean outOfDate(Integer version) {
		return !this.version.equals(version);
	}

	private void refresh(Integer version) {
		// update version
		this.version = version;

		// update all working threads
		long[] threads = this.sunThreadMXBean.getAllThreadIds();
		TreeSet<Long> threadSet = new TreeSet<Long>();
		for (long thread : threads) {
			threadSet.add(thread);
		}

		this.javaThreadHeapUsing.replaceAll((key, value) -> {
			if (threadSet.contains(key.unwrap())) {
				return value;
			} else {
				return 0L;
			}
		});

		for (long thread : threads) {
			JavaThreadID jid = new JavaThreadID(thread);
			if (this.javaThreadHeapUsing.containsKey(jid)) {
				Long heapUsing = this.sunThreadMXBean.getThreadAllocatedBytes(jid.unwrap());
				this.javaThreadHeapUsing.computeIfPresent(
						jid, (key, value) -> { return Math.max(0, heapUsing - value); });
			} else {
				this.javaThreadHeapUsing.put(
						jid, Math.max(0, this.sunThreadMXBean.getThreadAllocatedBytes(jid.unwrap())));
			}
		}
	}
}
