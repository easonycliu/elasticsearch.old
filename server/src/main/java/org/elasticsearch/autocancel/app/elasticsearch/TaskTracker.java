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
import org.elasticsearch.autocancel.utils.logger.Logger;

public class TaskTracker {

    private MainManager mainManager;

    private Map<CancellableID, List<Runnable>> cancellableIDToAsyncRunnables;

    private BidiMap<CancellableID, TaskWrapper.TaskID> cancellableIDTaskIDBiMap;

    private Map<TaskWrapper.TaskID, List<Object>> danglingTaskBuffer;
    
    private ReadWriteLock autoCancelReadWriteLock;

    private ReleasableLock readLock;

    private ReleasableLock writeLock;

    private Log log;

    public TaskTracker(MainManager mainManager) {
        this.mainManager = mainManager;

        this.cancellableIDToAsyncRunnables = new HashMap<CancellableID, List<Runnable>>();

        this.cancellableIDTaskIDBiMap = new DualHashBidiMap<CancellableID, TaskWrapper.TaskID>();

        this.danglingTaskBuffer = new HashMap<TaskWrapper.TaskID, List<Object>>();

        this.autoCancelReadWriteLock = new ReentrantReadWriteLock();

        this.readLock = new ReleasableLock(autoCancelReadWriteLock.readLock());

        this.writeLock = new ReleasableLock(autoCancelReadWriteLock.writeLock());

        this.log = new Log(this.mainManager);
    }

    public void stop() {
        this.log.stop();
    }

    public void onTaskCreate(Object task, Boolean isCancellable) throws AssertionError {        
        TaskWrapper wrappedTask = new TaskWrapper(task);

        CancellableID parentCancellableID = null;

        if (wrappedTask.getParentTaskID().isValid()) {
            try (ReleasableLock ignored = this.writeLock.acquire()) {
                if (this.cancellableIDTaskIDBiMap.containsValue(wrappedTask.getParentTaskID())) {
                    parentCancellableID = this.cancellableIDTaskIDBiMap.getKey(wrappedTask.getParentTaskID());
                }
                else {
                    if (wrappedTask.getTaskID().equals(wrappedTask.getParentTaskID())) {
                        // It IS root task
                        parentCancellableID = new CancellableID();
                    }
                    if (wrappedTask.getTaskID().compareTo(wrappedTask.getParentTaskID()) < 0) {
                        // parent task id is bigger than task id
                        // which means currently parent task hasn't been created yet
                        if (this.danglingTaskBuffer.containsKey(wrappedTask.getParentTaskID())) {
                            this.danglingTaskBuffer.get(wrappedTask.getParentTaskID()).add(task);
                        }
                        else {
                            this.danglingTaskBuffer.put(wrappedTask.getParentTaskID(), new ArrayList<>(Arrays.asList(task)));
                        }
                        // leave parentCancellableID to null so it will skip the next step
                        // once its parentCancellableID has been registered, it will be created
                    }
                    else {
                        assert false : "Can't find parent task of " + task.toString();
                    }
                }
            }
        }
        else {
            parentCancellableID = new CancellableID();
        }

        if (parentCancellableID != null) {
            CancellableID cid = this.mainManager.createCancellableIDOnCurrentJavaThreadID(true, task.toString(), wrappedTask.getAction(), parentCancellableID);

            try (ReleasableLock ignored = this.writeLock.acquire()) {
                assert !this.cancellableIDTaskIDBiMap.containsKey(cid) : "Do not register one task twice.";

                this.cancellableIDTaskIDBiMap.put(cid, wrappedTask.getTaskID());
            }

            Logger.systemTrace("Created " + task.toString());

            // handling dangling child cancellables
            if (!parentCancellableID.isValid()) {
                List<Object> danglingTasks = null;
                try (ReleasableLock ignore = this.writeLock.acquire()) {
                    danglingTasks = this.danglingTaskBuffer.get(wrappedTask.getTaskID());
                    if (danglingTasks != null) {
                        this.danglingTaskBuffer.remove(wrappedTask.getTaskID());
                    }
                }
                if (danglingTasks != null) {
                    for (Object danglingTask : danglingTasks) {
                        // All children's isCancellable is the same as their parent's
                        AutoCancel.onTaskCreate(danglingTask, isCancellable);
                    }
                }
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

        Logger.systemTrace("Exit " + task.toString());

        try (ReleasableLock ignored = this.readLock.acquire()) {
            cid = this.cancellableIDTaskIDBiMap.getKey(wrappedTask.getTaskID());
        }

        if (cid != null) {
            try (ReleasableLock ignored = this.writeLock.acquire()) {
                assert this.cancellableIDTaskIDBiMap.containsKey(cid) : "Maps should contains the cid to be removed.";
                this.removeCancellableIDFromMaps(cid);
            }

            this.mainManager.destoryCancellableIDOnCurrentJavaThreadID(cid);

            this.log.logCancellableJavaThreadIDInfo(cid, task);
        }
        else {
            Logger.systemWarn("Cannot find " + task.toString() + " , check whether it has exited before.");
        }
    }

    public void onTaskFinishInThread() throws AssertionError {
        CancellableID cid = this.mainManager.getCancellableIDOnCurrentJavaThreadID();
        if (!cid.isValid()) {
            // task has exited
            // TODO: maybe there is a better way to identify the status
            return;
        }

        this.mainManager.unregisterCancellableIDOnCurrentJavaThreadID();
    }

    public void onTaskQueueInThread(Runnable runnable) throws AssertionError {
        assert runnable != null : "Runable cannot be a null pointer.";

        CancellableID cid = this.mainManager.getCancellableIDOnCurrentJavaThreadID();

        // assert cid.isValid() : "Task must be running before queuing into threadpool.";
        if (!cid.isValid()) {
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

    public TaskWrapper.TaskID getTaskIDFromCancellableID(CancellableID cid) {
        return this.cancellableIDTaskIDBiMap.get(cid);
    }

    private void removeCancellableIDFromMaps(CancellableID cid) {
        this.cancellableIDToAsyncRunnables.remove(cid);
        this.cancellableIDTaskIDBiMap.remove(cid);
    }
}
