package org.elasticsearch.autocancel.core;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.elasticsearch.autocancel.core.performance.Performance;
import org.elasticsearch.autocancel.core.utils.Cancellable;
import org.elasticsearch.autocancel.core.utils.CancellableGroup;
import org.elasticsearch.autocancel.core.utils.ResourcePool;
import org.elasticsearch.autocancel.utils.Policy;
import org.elasticsearch.autocancel.utils.id.CancellableID;
import org.elasticsearch.autocancel.utils.resource.ResourceName;

public class AutoCancelInfoCenter {
	private final Map<CancellableID, CancellableGroup> rootCancellableToCancellableGroup;

	private final Map<CancellableID, Cancellable> cancellables;

	private final ResourcePool systemResourcePool;

	private final Performance performanceMetrix;

	public AutoCancelInfoCenter(Map<CancellableID, CancellableGroup> rootCancellableToCancellableGroup,
			Map<CancellableID, Cancellable> cancellables, ResourcePool systemResourcePool,
			Performance performanceMetrix) {
		this.rootCancellableToCancellableGroup = rootCancellableToCancellableGroup;
		this.cancellables = cancellables;
		this.systemResourcePool = systemResourcePool;
		this.performanceMetrix = performanceMetrix;
	}

	public Integer getFinishedTaskNumber() {
		return this.performanceMetrix.getFinishedTaskNumber();
	}

	public Double getResourceContentionLevel(ResourceName resourceName) {
		Long cancellableGroupNumber = 0L;
		Double averageSlowdown = 0.0;
		for (CancellableGroup cancellableGroup : this.rootCancellableToCancellableGroup.values()) {
			Double cancellableGroupSlowdown = cancellableGroup.getResourceSlowdown(resourceName);
			averageSlowdown = (averageSlowdown * cancellableGroupNumber + cancellableGroupSlowdown)
					/ (cancellableGroupNumber + 1);
			cancellableGroupNumber += 1;
		}
		return averageSlowdown;
	}

	public Map<ResourceName, Double> getContentionLevel() {
		Set<ResourceName> resourceNames = this.systemResourcePool.getResourceNames();
		Map<ResourceName, Double> resourceContentionLevel = new HashMap<ResourceName, Double>();
		for (ResourceName resourceName : resourceNames) {
			resourceContentionLevel.put(resourceName, this.getResourceContentionLevel(resourceName));
		}
		return resourceContentionLevel;
	}

	public Map<CancellableID, Long> getCancellableGroupResourceUsage(ResourceName resourceName) {
		Map<CancellableID, Long> cancellableGroupResourceUsage = new HashMap<CancellableID, Long>();
		for (Map.Entry<CancellableID, CancellableGroup> entry : this.rootCancellableToCancellableGroup.entrySet()) {
			cancellableGroupResourceUsage.put(entry.getKey(), entry.getValue().getResourceUsage(resourceName));
		}
		return cancellableGroupResourceUsage;
	}

	public Map<CancellableID, Map<ResourceName, Long>> getCancellableGroupUsage() {
		Map<CancellableID, Map<ResourceName, Long>> cancellableGroupUsage =
				new HashMap<CancellableID, Map<ResourceName, Long>>();
		for (Map.Entry<CancellableID, CancellableGroup> entry : this.rootCancellableToCancellableGroup.entrySet()) {
			Set<ResourceName> resourceNames = entry.getValue().getResourceNames();
			Map<ResourceName, Long> resourceUsage = new HashMap<ResourceName, Long>();
			for (ResourceName resourceName : resourceNames) {
				resourceUsage.put(resourceName, entry.getValue().getResourceUsage(resourceName));
			}
			cancellableGroupUsage.put(entry.getKey(), resourceUsage);
		}
		return cancellableGroupUsage;
	}

	public Map<CancellableID, Double> getUnifiedCancellableGroupResourceUsage(ResourceName resourceName) {
		Map<CancellableID, Long> cancellableGroupResourceUsage = new HashMap<CancellableID, Long>();
		Long cancellableGroupResourceUsageSum = 0L;
		for (Map.Entry<CancellableID, CancellableGroup> entry : this.rootCancellableToCancellableGroup.entrySet()) {
			Long usage = entry.getValue().getResourceUsage(resourceName);
			cancellableGroupResourceUsage.put(entry.getKey(), usage);
			cancellableGroupResourceUsageSum += usage;
		}
		final Long finalCancellableGroupResourceUsageSum = cancellableGroupResourceUsageSum;
		return cancellableGroupResourceUsage.entrySet().stream().collect(Collectors.toMap(e -> e.getKey(), e -> {
			if (finalCancellableGroupResourceUsageSum.equals(0L)) {
				return 0.0;
			} else {
				return Double.valueOf(e.getValue()) / finalCancellableGroupResourceUsageSum;
			}
		}));
	}

	public Map<CancellableID, Map<ResourceName, Double>> getUnifiedCancellableGroupUsage() {
		Map<CancellableID, Map<ResourceName, Long>> cancellableGroupUsage =
				new HashMap<CancellableID, Map<ResourceName, Long>>();
		Map<ResourceName, Long> cancellableGroupUsageSum = new HashMap<ResourceName, Long>();
		for (Map.Entry<CancellableID, CancellableGroup> entry : this.rootCancellableToCancellableGroup.entrySet()) {
			Set<ResourceName> resourceNames = entry.getValue().getResourceNames();
			Map<ResourceName, Long> resourceUsage = new HashMap<ResourceName, Long>();
			for (ResourceName resourceName : resourceNames) {
				Long usage = entry.getValue().getResourceUsage(resourceName);
				resourceUsage.put(resourceName, usage);
				cancellableGroupUsageSum.merge(resourceName, usage, Long::sum);
			}
			cancellableGroupUsage.put(entry.getKey(), resourceUsage);
		}
		return cancellableGroupUsage.entrySet().stream().collect(Collectors.toMap(cancellableGroupUsageElement
				-> cancellableGroupUsageElement.getKey(),
				cancellableGroupUsageElement
				-> cancellableGroupUsageElement.getValue().entrySet().stream().collect(Collectors.toMap(
						resourceUsageElement -> resourceUsageElement.getKey(), resourceUsageElement -> {
							Long sum = cancellableGroupUsageSum.get(resourceUsageElement.getKey());
							if (sum.equals(0L)) {
								return 0.0;
							} else {
								return Double.valueOf(resourceUsageElement.getValue()) / sum;
							}
						}))));
	}

	public Map<CancellableID, Double> getCancellableGroupResourceBenefit(ResourceName resourceName) {
		Map<CancellableID, Double> cancellableGroupResourceBenefit = new HashMap<CancellableID, Double>();
		Map<CancellableID, Double> unifiedCancellableGroupResourceUsage =
				this.getUnifiedCancellableGroupResourceUsage(resourceName);
		Map<CancellableID, Long> cancellableGroupRemainTime = this.getCancellableGroupRemainTime();
		Set<CancellableID> availableCancellableGroup =
				new HashSet<CancellableID>(unifiedCancellableGroupResourceUsage.keySet());
		availableCancellableGroup.retainAll(cancellableGroupRemainTime.keySet());
		for (CancellableID cid : availableCancellableGroup) {
			cancellableGroupResourceBenefit.put(
					cid, unifiedCancellableGroupResourceUsage.get(cid) * cancellableGroupRemainTime.get(cid));
		}
		return cancellableGroupResourceBenefit;
	}

	public Map<CancellableID, Map<ResourceName, Double>> getCancellableGroupBenefit() {
		Map<CancellableID, Map<ResourceName, Double>> cancellableGroupBenefit =
				new HashMap<CancellableID, Map<ResourceName, Double>>();
		Map<CancellableID, Map<ResourceName, Double>> unifiedCancellableGroupUsage =
				this.getUnifiedCancellableGroupUsage();
		Map<CancellableID, Long> cancellableGroupRemainTime = this.getCancellableGroupRemainTime();
		Set<CancellableID> availableCancellableGroup =
				new HashSet<CancellableID>(unifiedCancellableGroupUsage.keySet());
		availableCancellableGroup.retainAll(cancellableGroupRemainTime.keySet());
		for (CancellableID cid : availableCancellableGroup) {
			Map<ResourceName, Double> benefit = new HashMap<ResourceName, Double>();
			for (Map.Entry<ResourceName, Double> usageEntry : unifiedCancellableGroupUsage.get(cid).entrySet()) {
				benefit.put(usageEntry.getKey(), usageEntry.getValue() * cancellableGroupRemainTime.get(cid));
			}
			cancellableGroupBenefit.put(cid, benefit);
		}
		return cancellableGroupBenefit;
	}

	public Map<CancellableID, Long> getCancellableGroupRemainTime() {
		Map<CancellableID, Long> cancellableGroupRemainTime = new HashMap<CancellableID, Long>();
		for (Map.Entry<CancellableID, CancellableGroup> entry : this.rootCancellableToCancellableGroup.entrySet()) {
			cancellableGroupRemainTime.put(entry.getKey(), entry.getValue().predictRemainTime());
		}
		return cancellableGroupRemainTime;
	}

	public Map<CancellableID, Long> getCancellableGroupRemainTimeNano() {
		Map<CancellableID, Long> cancellableGroupRemainTimeNano = new HashMap<CancellableID, Long>();
		for (Map.Entry<CancellableID, CancellableGroup> entry : this.rootCancellableToCancellableGroup.entrySet()) {
			cancellableGroupRemainTimeNano.put(entry.getKey(), entry.getValue().predictRemainTimeNano());
		}
		return cancellableGroupRemainTimeNano;
	}

	public Boolean isCancellable(CancellableID cid) {
		Boolean isCancellable = false;
		if (cid != null) {
			CancellableGroup cancellableGroup = this.rootCancellableToCancellableGroup.get(cid);
			if (cancellableGroup != null) {
				isCancellable = cancellableGroup.getIsCancellable() && !cancellableGroup.isExit();
			}
		}
		return isCancellable;
	}
}
