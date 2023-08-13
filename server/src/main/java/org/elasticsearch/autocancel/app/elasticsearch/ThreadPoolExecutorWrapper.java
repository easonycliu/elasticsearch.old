package org.elasticsearch.autocancel.app.elasticsearch;

import org.elasticsearch.autocancel.app.elasticsearch.AutoCancel;
import org.elasticsearch.autocancel.utils.Syscall;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class ThreadPoolExecutorWrapper extends ThreadPoolExecutor {

    public ThreadPoolExecutorWrapper(int corePoolSize, 
    int maximumPoolSize, 
    long keepAliveTime, 
    TimeUnit unit, 
    BlockingQueue<Runnable> workQueue) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue);
    }

    public ThreadPoolExecutorWrapper(int corePoolSize,
    int maximumPoolSize,
    long keepAliveTime,
    TimeUnit unit,
    BlockingQueue<Runnable> workQueue,
    ThreadFactory threadFactory) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, threadFactory);
    }

    public ThreadPoolExecutorWrapper(int corePoolSize,
    int maximumPoolSize,
    long keepAliveTime,
    TimeUnit unit,
    BlockingQueue<Runnable> workQueue,
    RejectedExecutionHandler handler) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, handler);
    }

    public ThreadPoolExecutorWrapper(int corePoolSize,
    int maximumPoolSize,
    long keepAliveTime,
    TimeUnit unit,
    BlockingQueue<Runnable> workQueue,
    ThreadFactory threadFactory,
    RejectedExecutionHandler handler) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, threadFactory, handler);
    }

    protected void beforeExecute(Thread t, Runnable r) {
        assert t.equals(Thread.currentThread());
        this.checkThreadName(t);
        AutoCancel.onTaskStartInThread(r);
        super.beforeExecute(t, r);
    }

    protected void afterExecute(Runnable r, Throwable t) {
        super.afterExecute(r, t);
        AutoCancel.onTaskFinishInThread();
    }

    public void execute(Runnable command) {
        AutoCancel.onTaskQueueInThread(command);
        super.execute(command);
    }

    private void checkThreadName(Thread t) {
        String threadName = t.getName();
        if (!threadName.matches("(.*)(NativeTID:\\[)(.*)(\\])(.*)")) {
            t.setName(String.format("%s-NativeTID:[%d]", threadName, Syscall.gettid()));
        }
    }

}
