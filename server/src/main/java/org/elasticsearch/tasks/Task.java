/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0 and the Server Side Public License, v 1; you may not use this file except
 * in compliance with, at your election, the Elastic License 2.0 or the Server
 * Side Public License, v 1.
 */

package org.elasticsearch.tasks;

import org.elasticsearch.action.ActionResponse;
import org.elasticsearch.cluster.node.DiscoveryNode;
import org.elasticsearch.common.io.stream.NamedWriteable;
import org.elasticsearch.xcontent.ToXContent;
import org.elasticsearch.xcontent.ToXContentObject;

import java.io.IOException;
import java.util.Map;
import java.util.Set;

/**
 * Current task information
 */
public interface Task {

    /**
     * The request header to mark tasks with specific ids
     */
    public static final String X_OPAQUE_ID_HTTP_HEADER = "X-Opaque-Id";

    /**
     * The request header which is contained in HTTP request. We parse trace.id from it and store it in thread context.
     * TRACE_PARENT once parsed in RestController.tryAllHandler is not preserved
     * has to be declared as a header copied over from http request.
     * May also be used internally when APM is enabled.
     */
    public static final String TRACE_PARENT_HTTP_HEADER = "traceparent";

    /**
     * A request header that indicates the origin of the request from Elastic stack. The value will stored in ThreadContext
     * and emitted to ES logs
     */
    public static final String X_ELASTIC_PRODUCT_ORIGIN_HTTP_HEADER = "X-elastic-product-origin";

    public static final String TRACE_STATE = "tracestate";

    /**
     * Used internally to pass the apm trace context between the nodes
     */
    public static final String APM_TRACE_CONTEXT = "apm.local.context";

    /**
     * Parsed part of traceparent. It is stored in thread context and emitted in logs.
     * Has to be declared as a header copied over for tasks.
     */
    public static final String TRACE_ID = "trace.id";

    public static final String TRACE_START_TIME = "trace.starttime";
    public static final String TRACE_PARENT = "traceparent";

    public static final Set<String> HEADERS_TO_COPY = Set.of(
        X_OPAQUE_ID_HTTP_HEADER,
        TRACE_PARENT_HTTP_HEADER,
        TRACE_ID,
        X_ELASTIC_PRODUCT_ORIGIN_HTTP_HEADER
    );

    /**
     * Build a version of the task status you can throw over the wire and back
     * to the user.
     *
     * @param localNodeId
     *            the id of the node this task is running on
     * @param detailed
     *            should the information include detailed, potentially slow to
     *            generate data?
     */
    public TaskInfo taskInfo(String localNodeId, boolean detailed);


    /**
     * Returns task id
     */
    public long getId();

    /**
     * Returns task channel type (netty, transport, direct)
     */
    public String getType();

    /**
     * Returns task action
     */
    public String getAction();

    /**
     * Generates task description
     */
    public String getDescription();

    /**
     * Returns the task's start time as a wall clock time since epoch ({@link System#currentTimeMillis()} style).
     */
    public long getStartTime();

    /**
     * Returns the task's start time in nanoseconds ({@link System#nanoTime()} style).
     */
    public long getStartTimeNanos();

    /**
     * Returns id of the parent task or NO_PARENT_ID if the task doesn't have any parent tasks
     */
    public TaskId getParentTaskId();

    /**
     * Build a status for this task or null if this task doesn't have status.
     * Since most tasks don't have status this defaults to returning null. While
     * this can never perform IO it might be a costly operation, requiring
     * collating lists of results, etc. So only use it if you need the value.
     */
    public Status getStatus();

    @Override
    public String toString();

    /**
     * Report of the internal status of a task. These can vary wildly from task
     * to task because each task is implemented differently but we should try
     * to keep each task consistent from version to version where possible.
     * That means each implementation of {@linkplain Task.Status#toXContent}
     * should avoid making backwards incompatible changes to the rendered
     * result. But if we change the way a request is implemented it might not
     * be possible to preserve backwards compatibility. In that case, we
     * <b>can</b> change this on version upgrade but we should be careful
     * because some statuses (reindex) have become defacto standardized because
     * they are used by systems like Kibana.
     */
    public interface Status extends ToXContentObject, NamedWriteable {}

    /**
     * Returns stored task header associated with the task
     */
    public String getHeader(String header);

    public Map<String, String> headers();

    public TaskResult result(DiscoveryNode node, Exception error) throws IOException;

    public TaskResult result(DiscoveryNode node, ActionResponse response) throws IOException;
}
