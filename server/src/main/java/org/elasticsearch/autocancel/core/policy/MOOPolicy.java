package org.elasticsearch.autocancel.core.policy;

import java.util.HashMap;
import java.util.Map;

import org.elasticsearch.autocancel.utils.Policy;
import org.elasticsearch.autocancel.utils.id.CancellableID;
import org.elasticsearch.autocancel.utils.resource.ResourceName;

public class MOOPolicy extends Policy {
    
    private CancelTrigger trigger;

    public MOOPolicy() {
        super();
        this.trigger = new CancelTrigger();
    }

    @Override
    public Boolean needCancellation() {
        return this.trigger.triggered(Policy.infoCenter.getFinishedTaskNumber());
    }

    @Override
    public CancellableID getCancelTarget() {
        Map<CancellableID, Map<ResourceName, Long>> cancellableGroupUsage = Policy.infoCenter.getCancellableGroupUsage();
        Map<ResourceName, Double> weight = Policy.infoCenter.getContentionLevel();
        Map<CancellableID, Double> weightedSum = new HashMap<CancellableID, Double>();
        for (Map.Entry<CancellableID, Map<ResourceName, Long>> cancellableGroupUsageEntry : cancellableGroupUsage.entrySet()) {
            weightedSum.put(cancellableGroupUsageEntry.getKey(), MOOPolicy.calculateWeightedSum(weight, cancellableGroupUsageEntry.getValue()));
        }
        Map.Entry<CancellableID, Double> maxWeightedSum = weightedSum
                                                            .entrySet()
                                                            .stream()
                                                            .max(Map.Entry.comparingByValue())
                                                            .orElse(null);
        CancellableID target = null;
        if (maxWeightedSum != null) {
            target = maxWeightedSum.getKey();
        }

        if (target == null) {
            System.out.println("Failed to find a target to cancel for unknown reason");
            target = new CancellableID();
        }
        else if (!Policy.infoCenter.isCancellable(target)) {
            System.out.println(target.toString() + " is not cancellable");
            target = new CancellableID();
        }

        return target;
    }

    private static Double calculateWeightedSum(Map<ResourceName, Double> weight, Map<ResourceName, Long> resourceUsages) {
        Double sum = 0.0;
        for (Map.Entry<ResourceName, Long> usageEntry : resourceUsages.entrySet()) {
            try {
                sum += Math.max(Double.valueOf(usageEntry.getValue()) * weight.get(usageEntry.getKey()), 0.0);
            }
            catch (NullPointerException e) {
                throw new AssertionError(String.format("%s name is not in weight map", usageEntry.getKey()));
            }
        }
        return sum;
    }
}
