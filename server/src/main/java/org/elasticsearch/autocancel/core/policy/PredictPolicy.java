package org.elasticsearch.autocancel.core.policy;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.elasticsearch.autocancel.utils.Policy;
import org.elasticsearch.autocancel.utils.id.CancellableID;
import org.elasticsearch.autocancel.utils.resource.ResourceName;

public class PredictPolicy extends Policy {
    private CancelTrigger trigger;

    public PredictPolicy() {
        super();
        this.trigger = new CancelTrigger();
    }

    @Override
    public Boolean needCancellation() {
        return this.trigger.triggered(Policy.infoCenter.getFinishedTaskNumber());
    }

    @Override
    public CancellableID getCancelTarget() {
        return null;
    }

    public static Map<CancellableID, Double> getCancellableGroupResourceBenefit(ResourceName resourceName) {
        Map<CancellableID, Double> cancellableGroupResourceBenefit = new HashMap<CancellableID, Double>();
        Map<CancellableID, Long> cancellableGroupResourceUsage = Policy.infoCenter.getCancellableGroupResourceUsage(resourceName);
        Map<CancellableID, Long> cancellableGroupRemainTime = Policy.infoCenter.getCancellableGroupRemainTime();
        Long cancellableGroupResourceUsageSum = cancellableGroupResourceUsage.values().stream().reduce(0L, Long::sum);
        Set<CancellableID> availableCancellableGroup = new HashSet<CancellableID>(cancellableGroupResourceUsage.keySet());
        availableCancellableGroup.retainAll(cancellableGroupRemainTime.keySet());
        for (CancellableID cid : availableCancellableGroup) {
            cancellableGroupResourceBenefit.put(cid, Double.valueOf(cancellableGroupResourceUsage.get(cid) * cancellableGroupRemainTime.get(cid)) / cancellableGroupResourceUsageSum);
        }
        return cancellableGroupResourceBenefit;
    }

    public static Map<CancellableID, Map<ResourceName, Double>> getCancellableGroupBenefit() {
        Map<CancellableID, Map<ResourceName, Double>> cancellableGroupBenefit = new HashMap<CancellableID, Map<ResourceName, Double>>();
        Map<CancellableID, Map<ResourceName, Long>> cancellableGroupUsage = Policy.infoCenter.getCancellableGroupUsage();
        Map<CancellableID, Long> cancellableGroupRemainTime = Policy.infoCenter.getCancellableGroupRemainTime();
        Map<ResourceName, Long> cancellableGroupUsageSum = cancellableGroupUsage.values().stream().reduce(new HashMap<ResourceName, Long>(), (result, element) -> {
            element.forEach((key, value) -> {
                result.merge(key, value, Long::sum);
            });
            return result;
        });
        Set<CancellableID> availableCancellableGroup = new HashSet<CancellableID>(cancellableGroupUsage.keySet());
        availableCancellableGroup.retainAll(cancellableGroupRemainTime.keySet());
        for (CancellableID cid : availableCancellableGroup) {
            Map<ResourceName, Double> benefit = new HashMap<ResourceName, Double>();
            Long remainTime = cancellableGroupRemainTime.get(cid);
            for (Map.Entry<ResourceName, Long> usageEntry : cancellableGroupUsage.get(cid).entrySet()) {
                benefit.put(usageEntry.getKey(), Double.valueOf(usageEntry.getValue() * remainTime) / cancellableGroupUsageSum.get(usageEntry.getKey()));
            }
            cancellableGroupBenefit.put(cid, benefit);
        }
        return cancellableGroupBenefit;
    }
}
