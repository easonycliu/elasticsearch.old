package org.elasticsearch.autocancel.infrastructure;

import java.util.HashMap;
import java.util.Map;

import org.elasticsearch.autocancel.utils.resource.ResourceName;

public class ResourceBatch {
	private Integer version;

	private Map<ResourceName, Map<String, Object>> resourceUpdateInfos;

	public ResourceBatch(Integer version) {
		this.version = version;
		this.resourceUpdateInfos = new HashMap<ResourceName, Map<String, Object>>();
	}

	public void setResourceValue(ResourceName resourceName, Map<String, Object> resourceUpdateInfo) {
		this.resourceUpdateInfos.put(resourceName, resourceUpdateInfo);
	}

	public Map<String, Object> getResourceValue(ResourceName resourceName) {
		Map<String, Object> resourceUpdateInfo;
		if (this.resourceUpdateInfos.containsKey(resourceName)) {
			resourceUpdateInfo = this.resourceUpdateInfos.get(resourceName);
		} else {
			resourceUpdateInfo = Map.of();
		}
		return resourceUpdateInfo;
	}

	public Integer getVersion() {
		return this.version;
	}
}
