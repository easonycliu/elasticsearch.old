package org.elasticsearch.autocancel.core.policy;

import java.io.FileWriter;
import java.nio.file.Paths;

import org.elasticsearch.autocancel.utils.Settings;

public class CancelLogger {
    
    private static final String rootPath = (String) Settings.getSetting("path_to_logs");

    private static final FileWriter writer;

    private static Boolean started = false;

    private static Integer experimentTime = 0;

    static {
        FileWriter tmpWriter = null;
        try {
            tmpWriter = new FileWriter(String.format("%s.csv", Paths.get(CancelLogger.rootPath, System.getProperty("autocancel.log")).toString()));
        }
        catch (Exception e) {
            System.out.println(e.getMessage());
        }
        finally {
            writer = tmpWriter;
        }
    }

    public static void experimentStart() {
        if (!CancelLogger.started) {
            CancelLogger.experimentTime += 1;
            CancelLogger.started = true;
        }
    }

    public static void experimentStop() {
        CancelLogger.started = false;
    }

    public static void logExperimentHeader() {
        try {
            CancelLogger.writer.append(String.format("%s,%s,%s,%s\n", "Times", "Throughput", "Cancel", "Recover"));
        }
        catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    public static void logExperimentInfo(Double throughput, Boolean cancel, Boolean recover) {
        try {
            CancelLogger.writer.append(String.format("%d,%f,%b,%b\n", CancelLogger.experimentTime, throughput, cancel, recover));
            CancelLogger.writer.flush();
        }
        catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }
}
