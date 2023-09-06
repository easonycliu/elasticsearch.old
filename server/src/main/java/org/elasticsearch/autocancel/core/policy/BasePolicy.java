package org.elasticsearch.autocancel.core.policy;

import java.util.Comparator;
import java.util.Map;

import org.elasticsearch.autocancel.utils.Policy;
import org.elasticsearch.autocancel.utils.Settings;
import org.elasticsearch.autocancel.utils.id.CancellableID;
import org.elasticsearch.autocancel.utils.logger.Logger;
import org.elasticsearch.autocancel.utils.resource.ResourceName;

public class BasePolicy extends Policy {

    private static final Integer ABNORMAL_PERFORMANCE_THRESHOLD = 1;

    private static final Long MAX_CONTINUOUS_ABNORMAL_MILLI = 5000L;

    private Boolean started = false;

    private Long continuousAbnormalTimeMilli = 0L;
    
    public BasePolicy() {
        super();
    }

    @Override
    public Boolean needCancellation() {
        Boolean need = false;
        if (!this.started) {
            this.started = (this.infoCenter.getFinishedTaskNumber() != 0);
            if (this.started) {
                Logger.systemInfo("Base policy activated.");
                this.continuousAbnormalTimeMilli = System.currentTimeMillis();
            }
        }
        else {
            Long currentTimeMilli = System.currentTimeMillis();
            if (this.infoCenter.getFinishedTaskNumber() > BasePolicy.ABNORMAL_PERFORMANCE_THRESHOLD) {
                this.continuousAbnormalTimeMilli = currentTimeMilli;
            }

            if (currentTimeMilli - this.continuousAbnormalTimeMilli > BasePolicy.MAX_CONTINUOUS_ABNORMAL_MILLI) {
                need = true;
                this.started = false;
                this.continuousAbnormalTimeMilli = 0L;
            }
        }
        
        return need;
    }

    @Override
    public CancellableID getCancelTarget() {
        Map<ResourceName, Double> resourceContentionLevel = this.infoCenter.getContentionLevel();
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
            Map<CancellableID, Double> cancellableGroupResourceResourceUsage = this.infoCenter.getCancellableGroupResourceUsage(resourceName);
            Map.Entry<CancellableID, Double> maxResourceUsage = cancellableGroupResourceResourceUsage
                                                                    .entrySet()
                                                                    .stream()
                                                                    .max(Map.Entry.comparingByValue()).orElse(null);
            if (maxResourceUsage != null) {
                target = maxResourceUsage.getKey();
            }
        }

        if (target == null) {
            target = new CancellableID();
        }

        Logger.systemInfo("Detect abnormal performance behaviour, cancel " + target.toString());
        return target;
    }
}
