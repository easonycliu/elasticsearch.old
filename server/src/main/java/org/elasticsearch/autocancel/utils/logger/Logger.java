package org.elasticsearch.autocancel.utils.logger;

import java.io.Closeable;
import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.elasticsearch.autocancel.utils.Settings;

// TODO: maybe we can connect to log4j (application related)
public class Logger implements Closeable {

    private static final String rootPath = (String) Settings.getSetting("path_to_logs");

    private static final String systemLogLevel = (String) Settings.getSetting("system_log_level");

    private static final Logger systemLogger = new Logger("system");
    
    private String fileBaseName;

    private Integer maxLine;

    private Integer currentLine;

    private FileWriter writer;

    public Logger(String fileBaseName) {
        this.fileBaseName = fileBaseName;
        this.maxLine = (Integer) Settings.getSetting("log_file_max_line");
        this.currentLine = 0;
        this.writer = this.createFileWriter();
    }

    synchronized public void log(String line) {
        this.checkCurrentLine();
        if (this.writer != null) {
            if (!line.endsWith("\n")) {
                line += "\n";
            }

            try {
                this.writer.append(line);
                this.currentLine += 1;
            }
            catch (Exception e) {
                System.out.print(String.format("Error occored in %s logger: %s", this.fileBaseName, e.toString()));
            }
        }
        else {
            System.out.print(String.format("Write is null"));
        }
    }

    public static void systemTrace(String line) {
        if (Logger.systemLogLevel.equals("TRACE")) {
            Logger.systemLogger.log(String.format("[TRACE@%d] %s", System.nanoTime(), line));
        }
    }

    public static void systemInfo(String line) {
        if (Logger.systemLogLevel.equals("TRACE") || Logger.systemLogLevel.equals("INFO")) {
            Logger.systemLogger.log(String.format("[INFO@%d] %s", System.nanoTime(), line));
        }
    }

    public static void systemWarn(String line) {
        // Whatever log level is, warning must output
        Logger.systemLogger.log(String.format("[WARN@%d] %s", System.nanoTime(), line));
    }

    @Override
    public void close() {
        if (this.writer != null) {
            try {
                this.writer.close();
            }
            catch (Exception e) {

            }
        }
    }

    private String getCurrentTimeString() {
        Date date = new Date();
        SimpleDateFormat dateFormat = new SimpleDateFormat("YYYY-MM-dd-HH-mm-ss");
        return dateFormat.format(date);
    }

    private String getCurrentDayString() {
        Date date = new Date();
        SimpleDateFormat dateFormat = new SimpleDateFormat("YYYY-MM-dd");
        return dateFormat.format(date);
    }

    private FileWriter createFileWriter() {
        FileWriter fileWriter;
        try {
            String currentDay = this.getCurrentDayString();
            Path directory = Paths.get(Logger.rootPath, currentDay);
            if (Files.notExists(directory)) {
                Files.createDirectory(directory);
            }
            
            fileWriter = new FileWriter(String.format("%s%s.log", Paths.get(directory.toString(), this.fileBaseName).toString(), this.getCurrentTimeString()), true);
        }
        catch (Exception e) {
            fileWriter = null;
        }
        return fileWriter;
    }

    private void checkCurrentLine() {
        if (this.writer != null) {
            if (this.currentLine >= this.maxLine) {
                try {
                    this.writer.close();
                }
                catch (Exception e) {

                }
                this.currentLine = 0;
                this.writer = this.createFileWriter();
            }
        }
    }
}
