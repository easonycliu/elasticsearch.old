package org.elasticsearch.action.support.replication;

import org.elasticsearch.tasks.CancellableTask;
import org.elasticsearch.tasks.TaskCancelledException;
import org.elasticsearch.tasks.TaskId;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.core.Nullable;

import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;

public class CancellableReplicationTask extends ReplicationTask implements CancellableTask {

    private volatile String reason;
    private volatile boolean isCancelled;
    private final ConcurrentLinkedQueue<CancellationListener> listeners = new ConcurrentLinkedQueue<>();
    
    public CancellableReplicationTask(long id, String type, String action, String description, TaskId parentTaskId, Map<String, String> headers) {
        super(id, type, action, description, parentTaskId, headers);
    }

    /**
     * This method is called by the task manager when this task is cancelled.
     */
    public final void cancel(String reason) {
        assert reason != null;
        synchronized (this) {
            if (this.isCancelled) {
                return;
            }
            this.isCancelled = true;
            this.reason = reason;
        }
        listeners.forEach(CancellationListener::onCancelled);
        onCancelled();
    }

    /**
     * Returns whether this task's children need to be cancelled too. {@code true} is a reasonable response even for tasks that have no
     * children, since child tasks might be added in future and it'd be easy to forget to update this, but returning {@code false} saves
     * a bit of computation in the task manager.
     */
    public boolean shouldCancelChildrenOnCancellation() {
        return true;
    }

    /**
     * Return whether the task is cancelled. If testing this flag to decide whether to throw a {@link TaskCancelledException}, consider
     * using {@link #ensureNotCancelled} or {@link #notifyIfCancelled} instead: these methods construct an exception that automatically
     * includes the cancellation reason.
     */
    public final boolean isCancelled() {
        return isCancelled;
    }

    /**
     * The reason the task was cancelled or null if it hasn't been cancelled. May also be null if the task was just cancelled since we don't
     * set the reason and the cancellation flag atomically.
     */
    @Nullable
    public final String getReasonCancelled() {
        return reason;
    }

    /**
     * This method adds a listener that needs to be notified if this task is cancelled.
     */
    public final void addListener(CancellableTask.CancellationListener listener) {
        synchronized (this) {
            if (this.isCancelled == false) {
                listeners.add(listener);
            }
        }
        if (isCancelled) {
            listener.onCancelled();
        }
    }

    /**
     * Called after the task is cancelled so that it can take any actions that it has to take.
     */
    protected void onCancelled() {}

    /**
     * Throws a {@link TaskCancelledException} if this task has been cancelled, otherwise does nothing.
     */
    public final synchronized void ensureNotCancelled() {
        if (isCancelled()) {
            throw getTaskCancelledException();
        }
    }

    /**
     * Notifies the listener of failure with a {@link TaskCancelledException} if this task has been cancelled, otherwise does nothing.
     * @return {@code true} if the task is cancelled and the listener was notified, otherwise {@code false}.
     */
    public final <T> boolean notifyIfCancelled(ActionListener<T> listener) {
        if (isCancelled == false) {
            return false;
        }
        final TaskCancelledException taskCancelledException;
        synchronized (this) {
            taskCancelledException = getTaskCancelledException();
        } // NB releasing the mutex before notifying the listener
        listener.onFailure(taskCancelledException);
        return true;
    }

    private TaskCancelledException getTaskCancelledException() {
        assert Thread.holdsLock(this);
        assert isCancelled;
        assert reason != null;
        return new TaskCancelledException("task cancelled [" + reason + ']');
    }
}
