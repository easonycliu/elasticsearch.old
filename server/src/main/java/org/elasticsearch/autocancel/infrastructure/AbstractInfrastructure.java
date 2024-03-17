package org.elasticsearch.autocancel.infrastructure;

import java.util.HashMap;
import java.util.Map;

import org.elasticsearch.autocancel.utils.id.ID;
import org.elasticsearch.autocancel.utils.resource.ResourceName;

public abstract class AbstractInfrastructure {
	private Map<ID, ResourceBatch> resourceMap;

	public AbstractInfrastructure() {
		this.resourceMap = new HashMap<ID, ResourceBatch>();
	}

	public Map<String, Object> getResource(ID id, ResourceName resourceName, Integer version) {
		if (this.outOfDate(id, version)) {
			this.updateResource(id, version);
		}
		Map<String, Object> resourceUpdateInfo = this.getResourceValue(id, resourceName);
		return resourceUpdateInfo;
	}

	private Boolean outOfDate(ID id, Integer version) {
		Boolean outOfDate;
		if (this.resourceMap.containsKey(id)) {
			if (!this.resourceMap.get(id).getVersion().equals(version)) {
				outOfDate = true;
			} else {
				outOfDate = false;
			}
		} else {
			outOfDate = true;
		}
		return outOfDate;
	}

	protected abstract void updateResource(ID id, Integer version);

	protected void setResourceBatch(ID id, ResourceBatch resourceBatch) {
		this.resourceMap.put(id, resourceBatch);
	}

	private Map<String, Object> getResourceValue(ID id, ResourceName resourceName) {
		Map<String, Object> resourceUpdateInfo;
		if (this.resourceMap.containsKey(id)) {
			resourceUpdateInfo = this.resourceMap.get(id).getResourceValue(resourceName);
		} else {
			resourceUpdateInfo = Map.of();
		}
		return resourceUpdateInfo;
	}
}
