package org.elasticsearch.autocancel.infrastructure.linux;

import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

import java.util.List;
import java.util.Arrays;

import org.elasticsearch.autocancel.infrastructure.CPUTimeInfo;
import org.elasticsearch.autocancel.infrastructure.ResourceReader;
import org.elasticsearch.autocancel.utils.id.ID;
import org.elasticsearch.autocancel.infrastructure.linux.LinuxThreadID;

public class LinuxCPUReader extends ResourceReader {
    
    private Map<LinuxThreadID, CPUTimeInfo> linuxThreadCPUTime;

    private CPUTimeInfo systemCPUTime;

    private Integer version;

    String jvmPID;

    public LinuxCPUReader() {
        super();
        this.linuxThreadCPUTime = new HashMap<LinuxThreadID, CPUTimeInfo>();
        this.systemCPUTime = new CPUTimeInfo();
        this.version = 0;
        this.jvmPID = this.getJVMPID();
    }

    @Override
    public Double readResource(ID id, Integer version) {
        assert id instanceof LinuxThreadID : "Linux CPU reader must recieve linux thread id";
        if (this.outOfDate(version)) {
            this.refresh(version);
        }
        Double cpuResourceUsage = 0.0;
        if (this.linuxThreadCPUTime.containsKey((LinuxThreadID) id)) {
            CPUTimeInfo cpuTimeInfo = this.linuxThreadCPUTime.get((LinuxThreadID) id);
            if (cpuTimeInfo.comparable(this.systemCPUTime)) {
                cpuResourceUsage = Double.valueOf(cpuTimeInfo.diffCPUTime()) / this.systemCPUTime.diffCPUTime();
            }
        }

        return cpuResourceUsage;
    }

    private Boolean outOfDate(Integer version) {
        return !this.version.equals(version);
    }

    private void refresh(Integer version) {
        // update version
        this.version = version;

        // update system cpu time
        this.systemCPUTime.update(version, this.getSystemCPUTime());

        // update all working threads
        // if there is a dead threads, do not update it, then its version will not be comparable with system cpu time, thus its utilization will be 0.0.
        String threadFileRoot = String.format("/proc/%s/task", this.jvmPID);
        List<File> threadFiles = Arrays.asList(new File(threadFileRoot).listFiles());

        for (File threadFile : threadFiles) {
            if (threadFile.isDirectory() && threadFile.getName().matches("^[0-9]+$")) {
                LinuxThreadID linuxThreadID = new LinuxThreadID(Long.valueOf(threadFile.getName()));
                if (this.linuxThreadCPUTime.containsKey(linuxThreadID)) {
                    CPUTimeInfo cpuTimeInfo = this.linuxThreadCPUTime.get(linuxThreadID);
                    cpuTimeInfo.update(version, this.getThreadCPUTime(linuxThreadID));
                }
                else {
                    this.linuxThreadCPUTime.put(linuxThreadID, new CPUTimeInfo(version, this.getThreadCPUTime(linuxThreadID)));
                }
            }
        }
    }

    private Long getSystemCPUTime() {
        String cpuInfo = "/proc/stat";
        Long cpuTime = Long.valueOf(0);
        
        try {
            Scanner scanner = new Scanner(new File(cpuInfo));
            if (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                List<String> items = Arrays.asList(line.split("[ ]+"));
                assert items.get(0) == "cpu" : "CPU stat format error.";
                Integer itemNum = items.size();
                for (Integer i = 1; i < itemNum; ++i) {
                    cpuTime += Long.valueOf(items.get(i));
                }
            }
        }
        catch (IOException e) {
            assert false : String.format("Failed to open file %s", cpuInfo);
        }

        return cpuTime;
    }

    private Long getThreadCPUTime(LinuxThreadID linuxThreadID) {
        String threadInfo = String.format("/proc/%s/task/%d/stat", this.jvmPID, linuxThreadID.unwrap());
        Long threadCPUTime = Long.valueOf(0);
        
        try {
            Scanner scanner = new Scanner(new File(threadInfo));
            if (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                List<String> items = Arrays.asList(line.split("[ ]+"));
                assert items.size() > 14 : "Thread CPU stat format error.";
                threadCPUTime = Long.valueOf(items.get(13)) + Long.valueOf(items.get(14));
            }
        }
        catch (IOException e) {
            assert false : String.format("Failed to open file %s", threadInfo);
        }

        return threadCPUTime;
    }

}
