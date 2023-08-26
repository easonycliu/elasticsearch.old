package org.elasticsearch.autocancel.app.elasticsearch;

import org.elasticsearch.autocancel.manager.MainManager;
import org.elasticsearch.autocancel.utils.Settings;
import org.elasticsearch.autocancel.utils.Resource.ResourceType;
import org.elasticsearch.autocancel.utils.logger.Logger;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.BiFunction;
import java.util.List;

public class Resource {

    private final MainManager mainManager;

    private static final Map<String, BiFunction<String, StackTraceElement, Boolean>> lockInfoParser = Map.of(
        "file_name", (name, stackTraceElement) -> stackTraceElement.getFileName().equals(name),
        "line_number", (name, stackTraceElement) -> name.matches("^[0-9]+$") && stackTraceElement.getLineNumber() == Integer.valueOf(name),
        "class_name", (name, stackTraceElement) -> stackTraceElement.getClassName().equals(name),
        "method_name", (name, stackTraceElement) -> stackTraceElement.getMethodName().equals(name)
    );


    private final Map<String, ConcurrentMap<String, Long>> resourceEventStartTime = Map.of(
        "wait", new ConcurrentHashMap<String, Long>(),
        "occupy", new ConcurrentHashMap<String, Long>()
    );

    public Resource(MainManager mainManager) {
        this.mainManager = mainManager;
    }
    
    public void addResourceUsage(String name, Double value) {
        this.mainManager.updateCancellableGroup(name, value);
    }

    private void addResourceEventDuration(String name, String event, Double value) {
        switch (event) {
            case "wait": 
                this.mainManager.updateResource(ResourceType.LOCK, name, Map.of("wait_time", value));
                break;
            case "occupy":
                this.addResourceUsage(name, value);
                break;
            default:
                break;
        }
    }

    public void startResourceEvent(String name, String event) {
        if (this.resourceEventStartTime.containsKey(event)) {
            if (this.resourceEventStartTime.get(event).containsKey(name)) {
                Long startTime = this.resourceEventStartTime.get(event).get(name);
                if (startTime.equals(-1L)) {
                    this.resourceEventStartTime.get(event).put(name, System.nanoTime());
                }
                else {
                    System.out.println("Resource " + event + " hasn't finished last time.");
                    // TODO: do something more
                }
            }
            else {
                this.resourceEventStartTime.get(event).put(name, System.nanoTime());
            }
        }
        else {
            Logger.systemWarn("Event " + event + " not supported, candidates: " + this.resourceEventStartTime.keySet().toString());
        }
    }

    public void endResourceEvent(String name, String event) {
        if (this.resourceEventStartTime.get(event).containsKey(name)) {
            Long startTime = this.resourceEventStartTime.get(event).get(name);
            if (startTime.equals(-1L)) {
                this.resourceEventStartTime.get(event).put(name, System.nanoTime());
                System.out.println("Resource " + event + " has finished, don't finish it again.");
                // TODO: do something more
            }
            else {
                Long duration = System.nanoTime() - startTime;
                this.addResourceEventDuration(name, event, Double.valueOf(duration));
            }
        }
        else {
            System.out.println("Resource " + event + " didn't start yet.");
            // TODO: do something more
        }
    }

    public void onLockWait(String name) {
        StackTraceElement[] stackTraceElements = Thread.currentThread().getStackTrace();
        // getStackTrace() <-- Resource.onLockWait() <-- Autocancel.onLockWait() <-- ReleasableLock.acquire() <-- Target <-- ...
        // to reach the target, it must have at least 5 elements
        // position sensitive! Do not use it in the plase other than ReleasableLock.acquire()
        // if want to track the lock waiting time of other types of locks, using startResourceWait(String name)

        // if (stackTraceElements.length >= 5) {
        //     if (this.isMonitorTarget(stackTraceElements[4])) {
        //         this.startResourceEvent(name, "wait");
        //     }
        // }
    }

    public void onLockGet(String name) {
        StackTraceElement[] stackTraceElements = Thread.currentThread().getStackTrace();
        // if (stackTraceElements.length >= 5) {
        //     if (this.isMonitorTarget(stackTraceElements[4])) {
        //         this.endResourceEvent(name, "wait");
        //         this.startResourceEvent(name, "occupy");
        //     }
        // }
    }

    public void onLockRelease(String name) {
        StackTraceElement[] stackTraceElements = Thread.currentThread().getStackTrace();
        // if (stackTraceElements.length >= 5) {
        //     if (this.isMonitorTarget(stackTraceElements[4])) {
        //         this.endResourceEvent(name, "occupy");
        //     }
        // }
    }

    private Boolean isMonitorTarget(StackTraceElement stackTraceElement) {
        List<?> monitorLocks = (List<?>) Settings.getSetting("monitor_locks");
        
        for (Object monitorLock : monitorLocks) {
            Boolean monitorTarget = true;
            for (Map.Entry<?, ?> entries : ((Map<?, ?>) monitorLock).entrySet()) {
                if (Resource.lockInfoParser.containsKey((String) entries.getKey())) {
                    if (!Resource.lockInfoParser.get((String) entries.getKey()).apply((String) entries.getValue(), stackTraceElement)) {
                        monitorTarget = false;
                        break;
                    }
                }
                else {
                    Logger.systemWarn("Do not support " + ((String) entries.getKey()) + ". Here are supported keys: " + Resource.lockInfoParser.keySet().toString());
                    monitorTarget = false;
                    break;
                }
            }
            if (monitorTarget) {
                // find one matches all location requirements
                return true;
            }
        }

        return false;
    }
}
