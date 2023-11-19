package org.elasticsearch.autocancel.api;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.BiFunction;

import org.elasticsearch.autocancel.manager.MainManager;
import org.elasticsearch.autocancel.utils.id.CancellableID;
import org.elasticsearch.autocancel.utils.logger.Logger;

public class TaskTracker {

    private MainManager mainManager;

    private ConcurrentMap<Runnable, CancellableID> queueCancellable;

    private ConcurrentMap<CancellableID, TaskInfo> taskMap;

    private BiFunction<Object, Object, TaskInfo> taskInfoFunction;

    public TaskTracker(MainManager mainManager, BiFunction<Object, Object, TaskInfo> taskInfoFunction) {
        this.mainManager = mainManager;
        this.queueCancellable = new ConcurrentHashMap<Runnable, CancellableID>();
        this.taskMap = new ConcurrentHashMap<CancellableID, TaskInfo>();
        this.taskInfoFunction = taskInfoFunction;
    }

    public void stop() {
    }

    public void onTaskCreate(Object task, Object request) throws AssertionError {
        TaskInfo taskInfo = this.taskInfoFunction.apply(task, request);
        if (taskInfo != null) {
            this.taskMap.put(taskInfo.getTaskID(), taskInfo);

            this.mainManager.createCancellableIDOnCurrentJavaThreadID(
                taskInfo.getTaskID(),
                taskInfo.getIsCancellable(), 
                taskInfo.getName(), 
                taskInfo.getAction(), 
                taskInfo.getParentTaskID(), 
                taskInfo.getStartTimeNano(),
                taskInfo.getStartTime()
            );

            Logger.systemTrace("Created " + task.toString());
        }
    }

    public void onTaskExit(Object task) throws AssertionError {
        TaskInfo taskInfo = this.taskInfoFunction.apply(task, null);
        if (taskInfo != null) {
            this.taskMap.remove(taskInfo.getTaskID(), taskInfo);

            Logger.systemTrace("Exit " + task.toString());

            if (taskInfo.getTaskID().isValid()) {
                this.mainManager.destoryCancellableIDOnCurrentJavaThreadID(taskInfo.getTaskID());
            }
            else {
                Logger.systemWarn(String.format("Error parsing %s", task.toString()));
            }
        }
    }

    public void onTaskFinishInThread() throws AssertionError {
        CancellableID cid = this.mainManager.getCancellableIDOnCurrentJavaThreadID();
        if (cid.isValid()) {
            this.mainManager.unregisterCancellableIDOnCurrentJavaThreadID();
        }
        else {
            Logger.systemWarn("Should have a cancellable running on a thread but not found");
        }
    }

    public void onTaskQueueInThread(Runnable runnable) throws AssertionError {
        assert runnable != null : "Runable cannot be a null pointer.";

        CancellableID cid = this.mainManager.getCancellableIDOnCurrentJavaThreadID();

        if (cid.isValid()) {
            assert this.queueCancellable.put(runnable, cid) == null : "Duplicated runnable from threadpool";
        }
        else {
            Logger.systemTrace("Cannot found corresponding cancellable from current thread");
        }
    }

    public void onTaskStartInThread(Runnable runnable) throws AssertionError {
        assert runnable != null : "Runable cannot be a null pointer.";

        CancellableID cid = this.queueCancellable.remove(runnable);

        if (cid != null) {
            this.mainManager.registerCancellableIDOnCurrentJavaThreadID(cid);
        }
        else {
            Logger.systemTrace("Cannot found corresponding cancellable from runnable");
        }
    }

    public void addTaskWork(Long work) {
        this.mainManager.updateCancellableGroupWork(Map.of(
            "add_work", work
        ));
    }

    public void finishTaskWork(Long work) {
        this.mainManager.updateCancellableGroupWork(Map.of(
            "finish_work", work
        ));
    }

    public TaskInfo getTaskInfo(CancellableID cid) {
        return this.taskMap.get(cid);
    }
}
