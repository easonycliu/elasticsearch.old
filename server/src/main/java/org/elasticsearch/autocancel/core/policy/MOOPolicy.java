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
        Map<CancellableID, Map<ResourceName, Double>> cancellableGroupMeasure = BasePolicy.getCancellableGroupMeasure();
        Map<ResourceName, Double> weight = Policy.infoCenter.getContentionLevel();
        for (Map.Entry<ResourceName, Double> entry : weight.entrySet()) {
            if (entry.getKey().toString().contains("CPU")) {
                weight.put(entry.getKey(), entry.getValue() / 3);
            }
            System.out.println(entry.getKey() + "'s contention level is " + entry.getValue());
        }
        Map<CancellableID, Double> weightedSum = new HashMap<CancellableID, Double>();
        for (Map.Entry<CancellableID, Map<ResourceName, Double>> cancellableGroupUsageEntry : cancellableGroupMeasure.entrySet()) {
            Double sum = MOOPolicy.calculateWeightedSum(weight, cancellableGroupUsageEntry.getValue());
            // System.out.println(String.format("%s has sum: %f", cancellableGroupUsageEntry.getKey().toString(), sum));
            weightedSum.put(cancellableGroupUsageEntry.getKey(), sum);
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
        else {
            for (Map.Entry<ResourceName, Double> entry : cancellableGroupMeasure.get(target).entrySet()) {
                System.out.println(entry.getKey() + "'s unified usage is " + entry.getValue());
            }
            weightedSum.remove(target);
            Map.Entry<CancellableID, Double> secondWeightedSum = weightedSum
                                                            .entrySet()
                                                            .stream()
                                                            .max(Map.Entry.comparingByValue())
                                                            .orElse(null);
            if (secondWeightedSum != null) {
                for (Map.Entry<ResourceName, Double> entry : cancellableGroupMeasure.get(secondWeightedSum.getKey()).entrySet()) {
                    System.out.println("Second weighted cancellable's " + entry.getKey() + "'s unified usage is " + entry.getValue());
                }
            }
        }

        return target;
    }

    private static Double calculateWeightedSum(Map<ResourceName, Double> weight, Map<ResourceName, Double> resourceUsages) {
        Double sum = 0.0;
        for (Map.Entry<ResourceName, Double> usageEntry : resourceUsages.entrySet()) {
            try {
                sum += Math.max(usageEntry.getValue() * weight.get(usageEntry.getKey()), 0.0);
            }
            catch (NullPointerException e) {
                throw new AssertionError(String.format("%s name is not in weight map", usageEntry.getKey()));
            }
        }
        return sum;
    }
}
