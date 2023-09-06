package org.elasticsearch.autocancel.infrastructure.jvm;

import org.elasticsearch.autocancel.infrastructure.ResourceReader;
import org.elasticsearch.autocancel.infrastructure.CPUTimeInfo;
import org.elasticsearch.autocancel.utils.id.ID;
import org.elasticsearch.autocancel.utils.id.JavaThreadID;
import org.elasticsearch.autocancel.utils.resource.ResourceName;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.HashMap;
import java.util.Map;

public class JavaCPUReader extends ResourceReader {

    private Map<JavaThreadID, CPUTimeInfo> javaThreadCPUTime;

    private CPUTimeInfo systemCPUTime;

    private Integer version;

    private ThreadMXBean threadMXBean;

    public JavaCPUReader() {
        super();
        this.javaThreadCPUTime = new HashMap<JavaThreadID, CPUTimeInfo>();
        this.systemCPUTime = new CPUTimeInfo();
        this.version = 0;
        this.threadMXBean = ManagementFactory.getThreadMXBean();
    }

    @Override
    public Map<String, Object> readResource(ID id, Integer version) {
        assert id instanceof JavaThreadID : "Java CPU reader must recieve java thread id";
        if (this.outOfDate(version)) {
            this.refresh(version);
        }
        Map<String, Object> cpuUpdateInfo = null;
        if (this.javaThreadCPUTime.containsKey((JavaThreadID) id)) {
            CPUTimeInfo cpuTimeInfo = this.javaThreadCPUTime.get((JavaThreadID) id);
            if (cpuTimeInfo.comparable(this.systemCPUTime)) {
                cpuUpdateInfo = Map.of("cpu_time_system", this.systemCPUTime.diffCPUTime(), 
                "cpu_time_thread", cpuTimeInfo.diffCPUTime());
            }
        }

        if (cpuUpdateInfo == null) {
            cpuUpdateInfo = Map.of();
        }

        return cpuUpdateInfo;
    }

    private Boolean outOfDate(Integer version) {
        return !this.version.equals(version);
    }

    private void refresh(Integer version) {
        // update version
        this.version = version;

        // update system cpu time
        this.systemCPUTime.update(version, System.nanoTime());

        // update all working threads
        // if there is a dead threads, do not update it, then its version will not be
        // comparable with system cpu time, thus its utilization will be 0.0.
        long[] threads = this.threadMXBean.getAllThreadIds();
        for (long thread : threads) {
            JavaThreadID jid = new JavaThreadID(thread);
            if (this.javaThreadCPUTime.containsKey(jid)) {
                CPUTimeInfo cpuTimeInfo = this.javaThreadCPUTime.get(jid);
                cpuTimeInfo.update(version, this.threadMXBean.getThreadCpuTime(thread));
            } else {
                this.javaThreadCPUTime.put(jid, new CPUTimeInfo(version, this.threadMXBean.getThreadCpuTime(thread)));
            }
        }
    }

}
