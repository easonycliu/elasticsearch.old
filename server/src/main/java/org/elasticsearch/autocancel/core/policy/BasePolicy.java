package org.elasticsearch.autocancel.core.policy;

import java.util.Map;

import org.elasticsearch.autocancel.utils.Policy;
import org.elasticsearch.autocancel.utils.id.CancellableID;
import org.elasticsearch.autocancel.utils.resource.ResourceName;

public class BasePolicy extends Policy {

    private CancelTrigger trigger;
    
    public BasePolicy() {
        super();
        this.trigger = new CancelTrigger();
    }

    @Override
    public Boolean needCancellation() {
        return this.trigger.triggered(Policy.infoCenter.getFinishedTaskNumber());
    }

    @Override
    public CancellableID getCancelTarget() {
        Map<ResourceName, Double> resourceContentionLevel = Policy.infoCenter.getContentionLevel();
        Map.Entry<ResourceName, Double> maxContentionLevel = resourceContentionLevel
                                                                .entrySet()
                                                                .stream()
                                                                .max(Map.Entry.comparingByValue()).orElse(null);
        ResourceName resourceName = null;
        if (maxContentionLevel != null) {
            resourceName = maxContentionLevel.getKey();
        }

        CancellableID target = null;

        if (resourceName != null) {
            System.out.println("Find contention resource " + resourceName);
            for (Map.Entry<ResourceName, Double> entry : resourceContentionLevel.entrySet()) {
                System.out.println(entry.getKey() + "'s contention level is " + entry.getValue());
            }
            Map<CancellableID, Long> cancellableGroupResourceResourceUsage = Policy.infoCenter.getCancellableGroupResourceUsage(resourceName);
            Map.Entry<CancellableID, Long> maxResourceUsage = cancellableGroupResourceResourceUsage
                                                                    .entrySet()
                                                                    .stream()
                                                                    .max(Map.Entry.comparingByValue()).orElse(null);
            if (maxResourceUsage != null) {
                target = maxResourceUsage.getKey();
                System.out.println(String.format("Detect abnormal performance behaviour, cancel %s, %s usage %d", 
                target.toString(), resourceName.toString(), maxResourceUsage.getValue()));
            }
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
}
