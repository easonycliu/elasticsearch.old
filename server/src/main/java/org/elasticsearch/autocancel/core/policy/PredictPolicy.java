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

    public static Map<CancellableID, Long> getCancellableGroupResourceBenefit(ResourceName resourceName) {
        Map<CancellableID, Long> cancellableGroupResourceBenefit = new HashMap<CancellableID, Long>();
        Map<CancellableID, Long> cancellableGroupResourceUsage = Policy.infoCenter.getCancellableGroupResourceUsage(resourceName);
        Map<CancellableID, Long> cancellableGroupRemainTime = Policy.infoCenter.getCancellableGroupRemainTime();
        Set<CancellableID> availableCancellableGroup = new HashSet<CancellableID>(cancellableGroupResourceUsage.keySet());
        availableCancellableGroup.retainAll(cancellableGroupRemainTime.keySet());
        for (CancellableID cid : availableCancellableGroup) {
            cancellableGroupResourceBenefit.put(cid, cancellableGroupResourceUsage.get(cid) * cancellableGroupRemainTime.get(cid));
        }
        return cancellableGroupResourceBenefit;
    }

    public static Map<CancellableID, Map<ResourceName, Long>> getCancellableGroupBenefit() {
        Map<CancellableID, Map<ResourceName, Long>> cancellableGroupBenefit = new HashMap<CancellableID, Map<ResourceName, Long>>();
        Map<CancellableID, Map<ResourceName, Long>> cancellableGroupUsage = Policy.infoCenter.getCancellableGroupUsage();
        Map<CancellableID, Long> cancellableGroupRemainTime = Policy.infoCenter.getCancellableGroupRemainTime();
        Set<CancellableID> availableCancellableGroup = new HashSet<CancellableID>(cancellableGroupUsage.keySet());
        availableCancellableGroup.retainAll(cancellableGroupRemainTime.keySet());
        for (CancellableID cid : availableCancellableGroup) {
            Map<ResourceName, Long> benefit = new HashMap<ResourceName, Long>();
            Long remainTime = cancellableGroupRemainTime.get(cid);
            for (Map.Entry<ResourceName, Long> usageEntry : cancellableGroupUsage.get(cid).entrySet()) {
                benefit.put(usageEntry.getKey(), usageEntry.getValue() * remainTime);
            }
            cancellableGroupBenefit.put(cid, benefit);
        }
        return cancellableGroupBenefit;
    }
}
