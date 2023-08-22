package org.elasticsearch.autocancel.core.utils;

import org.elasticsearch.autocancel.core.utils.OperationMethod;
import org.elasticsearch.autocancel.utils.id.CancellableID;
import org.elasticsearch.autocancel.utils.Resource.ResourceType;

import java.util.Map;
import java.util.HashMap;

// TODO: Find a better representaion
public class OperationRequest {
    OperationMethod operation;

    CancellableID target;

    ResourceType resourceType;

    Map<String, Object> params;

    Long nanoTime;

    public OperationRequest(OperationMethod operation, CancellableID target) {
        this.operation = operation;
        this.target = target;
        this.resourceType = ResourceType.NULL;
        this.params = new HashMap<String, Object>();
        this.nanoTime = System.nanoTime();
    }

    public OperationRequest(OperationMethod operation, CancellableID target, ResourceType resourceType) {
        this.operation = operation;
        this.target = target;
        this.resourceType = resourceType;
        this.params = new HashMap<String, Object>();
        this.nanoTime = System.nanoTime();
    }

    public OperationRequest(OperationMethod operation, CancellableID target, ResourceType resourceType, Map<String, Object> params) {
        this.operation = operation;
        this.target = target;
        this.resourceType = resourceType;
        this.params = params;
        this.nanoTime = System.nanoTime();
    }

    public void addRequestParam(String key, Object value) {
        this.params.put(key, value);
    }

    public void addRequestParam(Map<String, Object> params) {
        this.params.putAll(params);
    }

    public OperationMethod getOperation() {
        return this.operation;
    }

    public CancellableID getTarget() {
        return this.target;
    }

    public ResourceType getResourceType() {
        return this.resourceType;
    }

    public Map<String, Object> getParams() {
        return this.params;
    }

    @Override
    public String toString() {
        String strRequest = String.format("Time: %d, %s %s %s. ", this.nanoTime, this.operation.toString(), this.target.toString(), this.resourceType.toString());
        for (Map.Entry<String, Object> entry : this.params.entrySet()) {
            strRequest = strRequest + String.format("%s: %s; ", entry.getKey(), entry.getValue().toString());
        }
        return strRequest;
    }
}
