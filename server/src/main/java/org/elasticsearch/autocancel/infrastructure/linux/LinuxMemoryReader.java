package org.elasticsearch.autocancel.infrastructure.linux;

import org.elasticsearch.autocancel.infrastructure.ResourceReader;
import org.elasticsearch.autocancel.utils.Resource.ResourceName;
import org.elasticsearch.autocancel.utils.id.ID;
import org.elasticsearch.autocancel.utils.logger.Logger;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Scanner;

public class LinuxMemoryReader extends ResourceReader {

    String jvmPID;

    public LinuxMemoryReader() {
        super();
        this.jvmPID = this.getJVMPID();
    }

    @Override
    public Double readResource(ID id, Integer version) {
        assert id instanceof LinuxThreadID : "Linux memory reader must recieve linux thread id";
        Long memoryUsingKB = Long.valueOf(0);
        // Read from /proc/[pid]/task/[tid]/status
        String fileName = String.format("/proc/%s/task/%d/smaps_rollup", this.jvmPID, ((LinuxThreadID) id).unwrap());

        try {
            Scanner scanner = new Scanner(new File(fileName));
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                if (line.contains("Pss:")) {
                    memoryUsingKB = Long.parseLong(line.split("[ ]+")[1]);
                    break;
                }
            }
        } catch (IOException e) {
            // assert false : String.format("Failed to open %s", fileName);
            Logger.systemWarn(String.format("Failed to open file %s: %s", fileName, e.getMessage()));
        }

        Long totalMemoryKB = this.getTotalMemory();
        assert totalMemoryKB != 0 && totalMemoryKB > memoryUsingKB : "Failed to read total memory: invalid value";

        return Double.valueOf(memoryUsingKB) / totalMemoryKB;
    }

    private Long getTotalMemory() {
        String memInfo = "/proc/meminfo";
        Long totalMemoryKB = Long.valueOf(0);

        try {
            Scanner scanner = new Scanner(new File(memInfo));
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                if (line.contains("MemTotal:")) {
                    totalMemoryKB = Long.parseLong(line.split("[ ]+")[1]);
                    break;
                }
            }
        } catch (IOException e) {
            assert false : String.format("Failed to open file %s: %s", memInfo, e.getMessage());
        }

        return totalMemoryKB;
    }
}
