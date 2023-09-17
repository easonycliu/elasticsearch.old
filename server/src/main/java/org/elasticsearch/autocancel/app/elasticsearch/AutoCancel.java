package org.elasticsearch.autocancel.app.elasticsearch;

import org.elasticsearch.autocancel.manager.MainManager;
import org.elasticsearch.autocancel.app.elasticsearch.TaskWrapper;
import org.elasticsearch.autocancel.utils.ReleasableLock;
import org.elasticsearch.autocancel.utils.id.CancellableID;
import org.elasticsearch.autocancel.utils.id.JavaThreadID;
import org.elasticsearch.autocancel.utils.logger.Logger;
import org.elasticsearch.autocancel.utils.resource.ResourceType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.BiConsumer;
import java.util.List;
import java.lang.Thread;

public class AutoCancel {
    
    private static Boolean started = false;

    private static MainManager mainManager = new MainManager();

    private static TaskTracker taskTracker = new TaskTracker(AutoCancel.mainManager);

    private static Resource resourceTracker = new Resource(AutoCancel.mainManager);

    private static Boolean warnNotStarted = true;

    private static Control controller = null;

    public static void start(BiConsumer<Long, String> canceller) {
        AutoCancel.mainManager.start(null);
        AutoCancel.controller = new Control(AutoCancel.mainManager, cid -> AutoCancel.taskTracker.getTaskIDFromCancellableID(cid), canceller);
    }

    public static void doStart() {
        started = true;
        Logger.systemInfo("AutoCancel started.");
    }

    public static void stop() {
        AutoCancel.taskTracker.stop();
        AutoCancel.mainManager.stop();
    }

    public static void onTaskCreate(Object task, Boolean isCancellable) {
        if (AutoCancel.started) {
            AutoCancel.taskTracker.onTaskCreate(task, isCancellable);
        }
        else if (warnNotStarted) {
            Logger.systemWarn("You should start lib AutoCancel first.");
            AutoCancel.warnNotStarted = false;
        }
    }

    public static void onTaskExit(Object task) {
        if (AutoCancel.started) {
            AutoCancel.taskTracker.onTaskExit(task);
        }
        else if (warnNotStarted) {
            Logger.systemWarn("You should start lib AutoCancel first.");
            AutoCancel.warnNotStarted = false;
        }
    }

    public static void onTaskFinishInThread() {
        if (AutoCancel.started) {
            AutoCancel.taskTracker.onTaskFinishInThread();
        }
        else if (warnNotStarted) {
            Logger.systemWarn("You should start lib AutoCancel first.");
            AutoCancel.warnNotStarted = false;
        }
    }

    public static void onTaskQueueInThread(Runnable runnable) {
        if (AutoCancel.started) {
            AutoCancel.taskTracker.onTaskQueueInThread(runnable);
        }
        else if (warnNotStarted) {
            Logger.systemWarn("You should start lib AutoCancel first.");
            AutoCancel.warnNotStarted = false;
        }
    }

    public static void onTaskStartInThread(Runnable runnable) {
        if (AutoCancel.started) {
            AutoCancel.taskTracker.onTaskStartInThread(runnable);
        }
        else if (warnNotStarted) {
            Logger.systemWarn("You should start lib AutoCancel first.");
            AutoCancel.warnNotStarted = false;
        }
    }

    public static void addMemoryResourceUsage(String name, Long usingMemory, Long totalMemory) {
        if (AutoCancel.started) {
            AutoCancel.resourceTracker.addResourceUsage(ResourceType.MEMORY, name, Map.of("using_memory", usingMemory, "total_memory", totalMemory));
        }
        else if (warnNotStarted) {
            Logger.systemWarn("You should start lib AutoCancel first.");
            AutoCancel.warnNotStarted = false;
        }
    }

    public static void addCPUResourceUsage(String name, Long cpuTimeSystem, Long cpuTimeThread) {
        if (AutoCancel.started) {
            Double threadCPUUsage = 0.0;
            if (cpuTimeSystem != 0L) {
                threadCPUUsage = Double.valueOf(cpuTimeThread) / cpuTimeSystem;
            }
            AutoCancel.resourceTracker.addResourceUsage(ResourceType.CPU, name, Map.of("cpu_time_system", cpuTimeSystem, 
            "cpu_time_thread", cpuTimeThread,
            "cpu_usage_thread", threadCPUUsage));
        }
        else if (warnNotStarted) {
            Logger.systemWarn("You should start lib AutoCancel first.");
            AutoCancel.warnNotStarted = false;
        }
    }

    public static Long startResourceWait(String name) {
        Long timestamp = 0L;
        if (AutoCancel.started) {
            timestamp = AutoCancel.resourceTracker.startResourceEvent(name, "wait");
        }
        else if (warnNotStarted) {
            Logger.systemWarn("You should start lib AutoCancel first.");
            AutoCancel.warnNotStarted = false;
        }
        return timestamp;
    }

    public static void endResourceWait(String name, Long timestamp) {
        if (AutoCancel.started) {
            AutoCancel.resourceTracker.endResourceEvent(name, "wait", timestamp);
        }
        else if (warnNotStarted) {
            Logger.systemWarn("You should start lib AutoCancel first.");
            AutoCancel.warnNotStarted = false;
        }
    }

    public static Long onLockWait(String name) {
        Long timestamp = -1L;
        if (AutoCancel.started) {
            timestamp = AutoCancel.resourceTracker.onLockWait(name);
        }
        else if (warnNotStarted) {
            Logger.systemWarn("You should start lib AutoCancel first.");
            AutoCancel.warnNotStarted = false;
        }
        return timestamp;
    }

    public static Long onLockGet(String name, Long timestamp) {
        Long nextTimestamp = -1L;
        if (AutoCancel.started) {
            nextTimestamp = AutoCancel.resourceTracker.onLockGet(name, timestamp);
        }
        else if (warnNotStarted) {
            Logger.systemWarn("You should start lib AutoCancel first.");
            AutoCancel.warnNotStarted = false;
        }
        return nextTimestamp;
    }

    public static void onLockRelease(String name, Long timestamp) {
        if (AutoCancel.started) {
            AutoCancel.resourceTracker.onLockRelease(name, timestamp);
        }
        else if (warnNotStarted) {
            Logger.systemWarn("You should start lib AutoCancel first.");
            AutoCancel.warnNotStarted = false;
        }
    }

    public static void cancel(CancellableID cid) {
        if (AutoCancel.started) {
            AutoCancel.controller.cancel(cid);
        }
        else if (warnNotStarted) {
            Logger.systemWarn("You should start lib AutoCancel first.");
            AutoCancel.warnNotStarted = false;
        }
    }

}