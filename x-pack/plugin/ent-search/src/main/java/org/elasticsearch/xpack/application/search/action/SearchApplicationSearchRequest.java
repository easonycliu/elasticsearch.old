/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */

package org.elasticsearch.xpack.application.search.action;

import org.elasticsearch.action.ActionRequest;
import org.elasticsearch.action.ActionRequestValidationException;
import org.elasticsearch.action.IndicesRequest;
import org.elasticsearch.action.support.IndicesOptions;
import org.elasticsearch.common.Strings;
import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.io.stream.StreamOutput;
import org.elasticsearch.core.Nullable;
import org.elasticsearch.tasks.CancellableTask;
import org.elasticsearch.tasks.BaseCancellableTask;
import org.elasticsearch.tasks.Task;
import org.elasticsearch.tasks.TaskId;
import org.elasticsearch.xcontent.ConstructingObjectParser;
import org.elasticsearch.xcontent.ParseField;
import org.elasticsearch.xcontent.XContentParser;

import java.io.IOException;
import java.util.Map;
import java.util.Objects;

import static org.elasticsearch.action.ValidateActions.addValidationError;
import static org.elasticsearch.xcontent.ConstructingObjectParser.constructorArg;

public class SearchApplicationSearchRequest extends ActionRequest implements IndicesRequest {

    private static final ParseField QUERY_PARAMS_FIELD = new ParseField("params");
    private final String name;

    private static final ConstructingObjectParser<SearchApplicationSearchRequest, String> PARSER = new ConstructingObjectParser<>(
        "query_params",
        false,
        (params, searchAppName) -> {
            @SuppressWarnings("unchecked")
            final Map<String, Object> queryParams = (Map<String, Object>) params[0];
            return new SearchApplicationSearchRequest(searchAppName, queryParams);
        }
    );

    static {
        PARSER.declareObject(constructorArg(), (p, c) -> p.map(), QUERY_PARAMS_FIELD);
    }

    private final Map<String, Object> queryParams;

    public SearchApplicationSearchRequest(StreamInput in) throws IOException {
        super(in);
        this.name = in.readString();
        this.queryParams = in.readMap();
    }

    public SearchApplicationSearchRequest(String name) {
        this(name, Map.of());
    }

    public SearchApplicationSearchRequest(String name, @Nullable Map<String, Object> queryParams) {
        this.name = Objects.requireNonNull(name, "Application name must be specified");
        this.queryParams = Objects.requireNonNullElse(queryParams, Map.of());
    }

    public static SearchApplicationSearchRequest fromXContent(String name, XContentParser contentParser) {
        return PARSER.apply(contentParser, name);
    }

    public String name() {
        return name;
    }

    public Map<String, Object> queryParams() {
        return queryParams;
    }

    @Override
    public ActionRequestValidationException validate() {
        ActionRequestValidationException validationException = null;

        if (Strings.isEmpty(name)) {
            validationException = addValidationError("Search Application name is missing", validationException);
        }

        return validationException;
    }

    @Override
    public Task createTask(long id, String type, String action, TaskId parentTaskId, Map<String, String> headers) {
        return new BaseCancellableTask(id, type, action, getDescription(), parentTaskId, headers);
    }

    @Override
    public void writeTo(StreamOutput out) throws IOException {
        super.writeTo(out);
        out.writeString(name);
        out.writeGenericMap(queryParams);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SearchApplicationSearchRequest request = (SearchApplicationSearchRequest) o;
        return Objects.equals(name, request.name) && Objects.equals(queryParams, request.queryParams);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, queryParams);
    }

    @Override
    public String[] indices() {
        return new String[] { name };
    }

    @Override
    public IndicesOptions indicesOptions() {
        return IndicesOptions.strictNoExpandForbidClosed();
    }
}
