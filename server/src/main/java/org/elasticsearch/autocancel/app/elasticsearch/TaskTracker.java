package org.elasticsearch.autocancel.app.elasticsearch;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.apache.commons.collections4.BidiMap;
import org.apache.commons.collections4.bidimap.TreeBidiMap;
import org.apache.commons.collections4.bidimap.DualHashBidiMap;

import org.elasticsearch.autocancel.manager.MainManager;
import org.elasticsearch.autocancel.utils.ReleasableLock;
import org.elasticsearch.autocancel.utils.id.CancellableID;

public class TaskTracker {

    private MainManager mainManager;

    private Map<CancellableID, List<Runnable>> cancellableIDToAsyncRunnables;

    private BidiMap<CancellableID, TaskWrapper.TaskID> cancellableIDTaskIDBiMap;
    
    private ReadWriteLock autoCancelReadWriteLock;

    private ReleasableLock readLock;

    private ReleasableLock writeLock;

    private Log log;

    public TaskTracker(MainManager mainManager) {
        this.mainManager = mainManager;

        this.cancellableIDToAsyncRunnables = new HashMap<CancellableID, List<Runnable>>();

        this.cancellableIDTaskIDBiMap = new DualHashBidiMap<CancellableID, TaskWrapper.TaskID>();

        this.autoCancelReadWriteLock = new ReentrantReadWriteLock();

        this.readLock = new ReleasableLock(autoCancelReadWriteLock.readLock());

        this.writeLock = new ReleasableLock(autoCancelReadWriteLock.writeLock());

        this.log = new Log(this.mainManager);
    }

    public void stop() {
        this.log.stop();
    }

    public void onTaskCreate(Object task) throws AssertionError {        
        TaskWrapper wrappedTask = new TaskWrapper(task);

        CancellableID parentCancellableID = null;

        if (wrappedTask.getParentTaskID().isValid()) {
            try (ReleasableLock ignored = this.readLock.acquire()) {
                assert this.cancellableIDTaskIDBiMap.containsValue(wrappedTask.getParentTaskID()) : "Can't find parent task";
                parentCancellableID = this.cancellableIDTaskIDBiMap.getKey(wrappedTask.getParentTaskID());
            }
        }
        else {
            parentCancellableID = new CancellableID();
        }

        if (parentCancellableID != null) {
            CancellableID cid = this.mainManager.createCancellableIDOnCurrentJavaThreadID(true, task.toString(), parentCancellableID);

            System.out.println("Created " + cid.toString() + " for " + wrappedTask.getTaskID().toString());

            try (ReleasableLock ignored = this.writeLock.acquire()) {
                assert !this.cancellableIDTaskIDBiMap.containsKey(cid) : "Do not register one task twice.";

                this.cancellableIDTaskIDBiMap.put(cid, wrappedTask.getTaskID());
            }
        }
        else {
            // Some task will exit before its child task create
            // TODO: Find a method to handle it
            System.out.println(String.format("Parent cancellable id not found: Time: %d, %s, Parent %s", System.currentTimeMillis(), wrappedTask.getTaskID().toString(), wrappedTask.getParentTaskID().toString()));
        }

    }

    public void onTaskExit(Object task) throws AssertionError {
        TaskWrapper wrappedTask = new TaskWrapper(task);
        CancellableID cid = null;

        try (ReleasableLock ignored = this.readLock.acquire()) {
            cid = this.cancellableIDTaskIDBiMap.getKey(wrappedTask.getTaskID());
        }

        System.out.println("Exit " + wrappedTask.getTaskID().toString());
        System.out.println("Exit " + cid.toString());

        assert cid != null : "Cannot exit an uncreated task.";

        try (ReleasableLock ignored = this.writeLock.acquire()) {
            assert this.cancellableIDTaskIDBiMap.containsKey(cid) : "Maps should contains the cid to be removed.";
            if (!this.cancellableIDToAsyncRunnables.containsKey(cid)) {
                // task has not been created when runnable starts on the first thread
                // TODO: maybe there is a better way to identify the status
            }
            this.removeCancellableIDFromMaps(cid);
        }

        this.mainManager.destoryCancellableIDOnCurrentJavaThreadID(cid);

        this.log.logCancellableJavaThreadIDInfo(cid, task);
    }

    public void onTaskFinishInThread() throws AssertionError {
        CancellableID cid = this.mainManager.getCancellableIDOnCurrentJavaThreadID();
        if (cid.equals(new CancellableID())) {
            // task has exited
            // TODO: maybe there is a better way to identify the status
            return;
        }

        this.mainManager.unregisterCancellableIDOnCurrentJavaThreadID();
    }

    public void onTaskQueueInThread(Runnable runnable) throws AssertionError {
        assert runnable != null : "Runable cannot be a null pointer.";

        CancellableID cid = this.mainManager.getCancellableIDOnCurrentJavaThreadID();

        // assert !cid.equals(new CancellableID()) : "Task must be running before queuing into threadpool.";
        if (cid.equals(new CancellableID())) {
            // task has not been created yet
            // TODO: maybe there is a better way to identify the status
            return;
        }

        try (ReleasableLock ignored = this.writeLock.acquire()) {
            if (this.cancellableIDToAsyncRunnables.containsKey(cid)) {
                this.cancellableIDToAsyncRunnables.get(cid).add(runnable);
            }
            else {
                this.cancellableIDToAsyncRunnables.put(cid, new ArrayList<Runnable>(Arrays.asList(runnable)));
            }
        }

    }

    public void onTaskStartInThread(Runnable runnable) throws AssertionError {
        assert runnable != null : "Runable cannot be a null pointer.";

        CancellableID cid = null;

        try (ReleasableLock ignored = this.readLock.acquire()) {
            for (Map.Entry<CancellableID, List<Runnable>> entry : this.cancellableIDToAsyncRunnables.entrySet()) {
                if (entry.getValue().contains(runnable)) {
                    cid = entry.getKey();
                    break;
                }
            }
        }

        // assert cid != null : "Cannot start a runnable out of excute entry.";
        if (cid == null) {
            // task has not been created yet
            // TODO: maybe there is a better way to identify the status
            return;
        }

        this.mainManager.registerCancellableIDOnCurrentJavaThreadID(cid);
    }

    private void removeCancellableIDFromMaps(CancellableID cid) {
        this.cancellableIDToAsyncRunnables.remove(cid);
        this.cancellableIDTaskIDBiMap.remove(cid);
    }
}
