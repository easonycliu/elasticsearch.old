/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0 and the Server Side Public License, v 1; you may not use this file except
 * in compliance with, at your election, the Elastic License 2.0 or the Server
 * Side Public License, v 1.
 */

package org.elasticsearch.tasks;

import org.elasticsearch.action.ActionListener;
import org.elasticsearch.core.Nullable;

import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * A task that can be cancelled
 */
public interface CancellableTask extends Task {

    /**
     * This method is called by the task manager when this task is cancelled.
     */
    public void cancel(String reason);

    /**
     * Returns whether this task's children need to be cancelled too. {@code true} is a reasonable response even for tasks that have no
     * children, since child tasks might be added in future and it'd be easy to forget to update this, but returning {@code false} saves
     * a bit of computation in the task manager.
     */
    public boolean shouldCancelChildrenOnCancellation();

    /**
     * Return whether the task is cancelled. If testing this flag to decide whether to throw a {@link TaskCancelledException}, consider
     * using {@link #ensureNotCancelled} or {@link #notifyIfCancelled} instead: these methods construct an exception that automatically
     * includes the cancellation reason.
     */
    public boolean isCancelled();

    /**
     * The reason the task was cancelled or null if it hasn't been cancelled. May also be null if the task was just cancelled since we don't
     * set the reason and the cancellation flag atomically.
     */
    @Nullable
    public String getReasonCancelled();

    /**
     * This method adds a listener that needs to be notified if this task is cancelled.
     */
    public void addListener(CancellationListener listener);

    /**
     * Throws a {@link TaskCancelledException} if this task has been cancelled, otherwise does nothing.
     */
    public void ensureNotCancelled();

    /**
     * Notifies the listener of failure with a {@link TaskCancelledException} if this task has been cancelled, otherwise does nothing.
     * @return {@code true} if the task is cancelled and the listener was notified, otherwise {@code false}.
     */
    public <T> boolean notifyIfCancelled(ActionListener<T> listener);

    /**
     * This interface is implemented by any class that needs to react to the cancellation of this task.
     */
    public interface CancellationListener {
        void onCancelled();
    }
}
