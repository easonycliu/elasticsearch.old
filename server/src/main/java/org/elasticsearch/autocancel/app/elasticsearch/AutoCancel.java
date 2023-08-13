package org.elasticsearch.autocancel.app.elasticsearch;

import org.elasticsearch.autocancel.manager.MainManager;
import org.elasticsearch.autocancel.app.elasticsearch.TaskWrapper;
import org.elasticsearch.autocancel.utils.ReleasableLock;
import org.elasticsearch.autocancel.utils.id.CancellableID;
import org.elasticsearch.autocancel.utils.id.JavaThreadID;

import java.util.ArrayList;
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

    private static TaskTracker taskTracker = new TaskTracker(AutoCancel.mainManager);

    public static void start() {
        AutoCancel.mainManager.start();
        started = true;
    }

    public static void stop() throws AssertionError {
        assert started : "You should start lib AutoCancel first.";
        AutoCancel.taskTracker.stop();
        AutoCancel.mainManager.stop();
    }

    public static void onTaskCreate(Object task) throws AssertionError {
        assert started : "You should start lib AutoCancel first.";
        
        AutoCancel.taskTracker.onTaskCreate(task);

    }

    public static void onTaskExit(Object task) throws AssertionError {
        assert started : "You should start lib AutoCancel first.";

        AutoCancel.taskTracker.onTaskExit(task);
        
    }

    public static void onTaskFinishInThread() throws AssertionError {
        assert started : "You should start lib AutoCancel first.";

        AutoCancel.taskTracker.onTaskFinishInThread();
    }

    public static void onTaskQueueInThread(Runnable runnable) throws AssertionError {
        assert started : "You should start lib AutoCancel first.";

        AutoCancel.taskTracker.onTaskQueueInThread(runnable);
    }

    public static void onTaskStartInThread(Runnable runnable) throws AssertionError {
        assert started : "You should start lib AutoCancel first.";
        
        AutoCancel.taskTracker.onTaskStartInThread(runnable);
    }

}