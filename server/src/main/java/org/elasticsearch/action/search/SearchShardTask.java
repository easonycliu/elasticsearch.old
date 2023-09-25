/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0 and the Server Side Public License, v 1; you may not use this file except
 * in compliance with, at your election, the Elastic License 2.0 or the Server
 * Side Public License, v 1.
 */

package org.elasticsearch.action.search;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.elasticsearch.search.fetch.ShardFetchSearchRequest;
import org.elasticsearch.search.internal.ShardSearchRequest;
import org.elasticsearch.tasks.CancellableTask;
import org.elasticsearch.tasks.BaseCancellableTask;
import org.elasticsearch.tasks.TaskId;

import java.util.Map;

/**
 * Task storing information about a currently running search shard request.
 * See {@link ShardSearchRequest}, {@link ShardFetchSearchRequest}, ...
 */
public class SearchShardTask extends BaseCancellableTask {

    public SearchShardTask(long id, String type, String action, String description, TaskId parentTaskId, Map<String, String> headers) {
        super(id, type, action, description, parentTaskId, headers);
    }

    @Override
    protected void onCancelled() {
        super.onCancelled();
    }

    @Override
    public boolean shouldCancelChildrenOnCancellation() {
        return false;
    }
}
