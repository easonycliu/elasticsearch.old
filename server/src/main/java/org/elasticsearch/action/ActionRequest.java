/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0 and the Server Side Public License, v 1; you may not use this file except
 * in compliance with, at your election, the Elastic License 2.0 or the Server
 * Side Public License, v 1.
 */

package org.elasticsearch.action;

import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.io.stream.StreamOutput;
import org.elasticsearch.transport.TransportRequest;
import org.elasticsearch.rest.RestRequest;

import java.io.IOException;

public abstract class ActionRequest extends TransportRequest {

    private RestRequest restRequest;

    public ActionRequest() {
        super();
        this.restRequest = null;
        // this does not set the listenerThreaded API, if needed, its up to the caller to set it
        // since most times, we actually want it to not be threaded...
        // this.listenerThreaded = request.listenerThreaded();
    }

    public ActionRequest(StreamInput in) throws IOException {
        super(in);
        this.restRequest = null;
    }

    public abstract ActionRequestValidationException validate();

    /**
     * Should this task store its result after it has finished?
     */
    public boolean getShouldStoreResult() {
        return false;
    }

    @Override
    public void writeTo(StreamOutput out) throws IOException {
        super.writeTo(out);
    }

    public void setRestRequest(RestRequest restRequest) {
        this.restRequest = restRequest;
    }

    public RestRequest getRestRequest() {
        return this.restRequest;
    }
}
