package org.elasticsearch.autocancel.app.elasticsearch;

import org.elasticsearch.autocancel.manager.MainManager;
import org.elasticsearch.autocancel.app.elasticsearch.TaskWrapper;
import org.elasticsearch.autocancel.utils.CancellableID;
import org.elasticsearch.autocancel.utils.JavaThreadID;
import org.elasticsearch.autocancel.utils.ReleasableLock;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import java.util.List;
import java.lang.Thread;

public class AutoCancel {

    private static Boolean started = false;

    private static MainManager mainManager = new MainManager();

    private static Map<CancellableID, List<Runnable>> cancellableIDToAsyncRunnables = new HashMap<CancellableID, List<Runnable>>();

    private static Map<CancellableID, TaskWrapper> cancellableIDToTask = new HashMap<CancellableID, TaskWrapper>();

    private static ReadWriteLock autoCancelReadWriteLock = new ReentrantReadWriteLock();

    private static ReleasableLock readLock = new ReleasableLock(autoCancelReadWriteLock.readLock());

    private static ReleasableLock writeLock = new ReleasableLock(autoCancelReadWriteLock.writeLock());

    public static void start() {
        mainManager.start();
        started = true;
    }

    public static void stop() throws AssertionError {
        assert started : "You should start lib AutoCancel first.";
        mainManager.stop();
    }

    public static void onTaskCreate(Object task) throws AssertionError {
        assert started : "You should start lib AutoCancel first.";

        TaskWrapper wrappedTask = new TaskWrapper(task);
        CancellableID cid = AutoCancel.mainManager.createCancellableIDOnCurrentJavaThreadID();

        try (ReleasableLock ignored = AutoCancel.writeLock.acquire()) {
            assert !cancellableIDToTask.containsKey(cid) : "Do not register one task twice.";
            cancellableIDToTask.put(cid, wrappedTask);
        }

    }

    public static void onTaskExit(Object task) throws AssertionError {
        assert started : "You should start lib AutoCancel first.";

        TaskWrapper wrappedTask = new TaskWrapper(task);
        CancellableID cid = null;

        try (ReleasableLock ignored = AutoCancel.readLock.acquire()) {
            for (Map.Entry<CancellableID, TaskWrapper> entry : AutoCancel.cancellableIDToTask.entrySet()) {
                if (entry.getValue().equals(wrappedTask)) {
                    cid = entry.getKey();
                    break;
                }
            }
        }

        assert cid != null : "Cannot exit an uncreated task.";

        try (ReleasableLock ignored = AutoCancel.writeLock.acquire()) {
            assert AutoCancel.cancellableIDToAsyncRunnables.containsKey(cid) &&
                    AutoCancel.cancellableIDToTask.containsKey(cid) : "Maps should contains the cid to be removed.";
            AutoCancel.removeCancellableIDFromMaps(cid);
        }

        AutoCancel.mainManager.destoryCancellableIDOnCurrentJavaThreadID(cid);
    }

    public static void onTaskFinishInThread() throws AssertionError {
        assert started : "You should start lib AutoCancel first.";

        AutoCancel.mainManager.unregisterCancellableIDOnCurrentJavaThreadID();
    }

    public static void onTaskQueueInThread(Runnable runnable) throws AssertionError {
        assert started : "You should start lib AutoCancel first.";
        assert runnable != null : "Runable cannot be a null pointer.";

        CancellableID cid = AutoCancel.mainManager.getCancellableIDOnCurrentJavaThreadID();

        assert !cid.equals(new CancellableID()) : "Task must be running before queuing into threadpool.";

        try (ReleasableLock ignored = AutoCancel.writeLock.acquire()) {
            if (AutoCancel.cancellableIDToAsyncRunnables.containsKey(cid)) {
                AutoCancel.cancellableIDToAsyncRunnables.get(cid).add(runnable);
            } else {
                AutoCancel.cancellableIDToAsyncRunnables.put(cid, Arrays.asList(runnable));
            }
        }

    }

    public static void onTaskStartInThread(Runnable runnable) throws AssertionError {
        assert started : "You should start lib AutoCancel first.";
        assert runnable != null : "Runable cannot be a null pointer.";

        CancellableID cid = null;

        try (ReleasableLock ignored = AutoCancel.readLock.acquire()) {
            for (Map.Entry<CancellableID, List<Runnable>> entry : AutoCancel.cancellableIDToAsyncRunnables.entrySet()) {
                if (entry.getValue().contains(runnable)) {
                    cid = entry.getKey();
                    break;
                }
            }
        }

        assert cid != null : "Cannot start a runnable out of excute entry.";

        AutoCancel.mainManager.registerCancellableIDOnCurrentJavaThreadID(cid);
    }

    private static void removeCancellableIDFromMaps(CancellableID cid) {
        AutoCancel.cancellableIDToAsyncRunnables.remove(cid);
        AutoCancel.cancellableIDToTask.remove(cid);
    }
}