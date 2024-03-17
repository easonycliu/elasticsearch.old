package org.elasticsearch.autocancel.core.utils;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.elasticsearch.autocancel.utils.id.CancellableID;
import org.elasticsearch.autocancel.utils.resource.ResourceName;
import org.elasticsearch.autocancel.utils.resource.ResourceType;

// TODO: Find a better representaion
public class OperationRequest {
	private OperationMethod operation;

	private Map<String, Object> params;

	private final Long nanoTime;

	private static final List<String> acceptedBasicInfoKeywords =
			Arrays.asList("cancellable_id", "parent_cancellable_id", "resource_name", "resource_type");

	public OperationRequest(OperationMethod operation, Map<String, Object> basicInfo) {
		this.operation = operation;
		// The iterate order of LinkedHashMap is same as input order
		this.params = new LinkedHashMap<String, Object>();
		this.nanoTime = System.nanoTime();
		this.params.put("basic_info", basicInfo);
	}

	public void addRequestParam(String key, Object value) {
		this.params.put(key, value);
	}

	public OperationMethod getOperation() {
		return this.operation;
	}

	public CancellableID getCancellableID() {
		CancellableID cid = (CancellableID) this.getBasicInfoParam("cancellable_id");
		if (cid == null) {
			cid = new CancellableID();
		}
		return cid;
	}

	public CancellableID getParentCancellableID() {
		// This has to be null if the is key is unset because invalid value indicates the
		// cancellable has no parent
		CancellableID parentCID = (CancellableID) this.getBasicInfoParam("parent_cancellable_id");
		return parentCID;
	}

	public ResourceName getResourceName() {
		ResourceName resourceName = (ResourceName) this.getBasicInfoParam("resource_name");
		if (resourceName == null) {
			resourceName = ResourceName.NULL;
		}
		return resourceName;
	}

	public ResourceType getResourceType() {
		ResourceType resourceType = (ResourceType) this.getBasicInfoParam("resource_type");
		if (resourceType == null) {
			resourceType = ResourceType.NULL;
		}
		return resourceType;
	}

	private Object getBasicInfoParam(String key) {
		assert OperationRequest.acceptedBasicInfoKeywords.contains(key) : key + " is not accepted";
		Object basicInfoValue = null;
		if (((Map<?, ?>) this.params.get("basic_info")).containsKey(key)) {
			basicInfoValue = ((Map<?, ?>) this.params.get("basic_info")).get(key);
		}
		return basicInfoValue;
	}

	public Map<String, Object> getParams() {
		return this.params;
	}

	@Override
	public String toString() {
		String strRequest = String.format("Time: %d, %s. ", this.nanoTime, this.operation.toString());
		for (Map.Entry<String, Object> entry : this.params.entrySet()) {
			strRequest = strRequest + String.format("%s: %s; ", entry.getKey(), entry.getValue().toString());
		}
		return strRequest;
	}
}
