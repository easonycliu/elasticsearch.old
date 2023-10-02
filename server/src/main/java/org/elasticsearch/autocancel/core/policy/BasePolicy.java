package org.elasticsearch.autocancel.core.policy;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.function.Predicate;
import java.util.function.Function;

import org.elasticsearch.autocancel.utils.Policy;
import org.elasticsearch.autocancel.utils.id.CancellableID;
import org.elasticsearch.autocancel.utils.resource.ResourceName;

public class BasePolicy extends Policy {

    private static final Double ABNORMAL_PERFORMANCE_DROP_PROTION = 0.4;

    private static final Double ABNORMAL_PERFORMANCE_DROP_ABSOLUTE = 200.0;

    private static final Long ONE_CYCLE_MILLI = 1000L;

    private static final Long MAX_CONTINUOUS_ABNORMAL_CYCLE = 10L;

    private static final Long PAST_PERFORMANCE_REF_MILLI = 20000L;

    private static final Integer MAX_PAST_PERFORMANCE_REF_NUM = 3;

    private static final Integer AVERAGE_FILTER_SIZE = 3;

    private AverageFilter averageFilter;

    private Boolean started;

    private PerformanceBuffer performanceBuffer;

    private Long continuousAbnormalCycles;

    private FixSizePriorityQueue<ThroughputDataPoint> maxThroughputQueue;
    
    public BasePolicy() {
        super();
        this.averageFilter = new AverageFilter(AVERAGE_FILTER_SIZE);
        this.started = false;
        this.performanceBuffer = new PerformanceBuffer(BasePolicy.ONE_CYCLE_MILLI);
        this.continuousAbnormalCycles = 0L;
        this.maxThroughputQueue = new FixSizePriorityQueue<ThroughputDataPoint>(
            BasePolicy.MAX_PAST_PERFORMANCE_REF_NUM, 
            (e1, e2) -> e1.getThroughput().intValue() - e2.getThroughput().intValue()
        );
    }

    public Boolean isAbnormal(Double throughput) {
        Boolean abnormal = false;
        Double normalThroughput = this.maxThroughputQueue.mean((element) -> Double.valueOf(element.getThroughput()));
        if (normalThroughput * (1.0 - BasePolicy.ABNORMAL_PERFORMANCE_DROP_PROTION) > throughput) {
            abnormal = true;
        }
        if (normalThroughput - BasePolicy.ABNORMAL_PERFORMANCE_DROP_ABSOLUTE > throughput) {
            abnormal = true;
        }
        return abnormal;
    }

    @Override
    public Boolean needCancellation() {
        Boolean need = false;
        if (!this.started) {
            long finishedTaskNumber = this.infoCenter.getFinishedTaskNumber();
            this.started = (finishedTaskNumber != 0);
            if (this.started) {
                this.averageFilter.clear();
                this.performanceBuffer.clear();
                this.continuousAbnormalCycles = 0L;
                this.maxThroughputQueue.clear();

                this.performanceBuffer.lastCyclePerformance(System.currentTimeMillis(), finishedTaskNumber);
            }
        }
        else {
            long currentTimeMilli = System.currentTimeMillis();
            long lastCyclePerformance = this.performanceBuffer.lastCyclePerformance(currentTimeMilli, this.infoCenter.getFinishedTaskNumber());
            if (lastCyclePerformance >= 0) {
                this.maxThroughputQueue.removeIf((element) -> element.isExpired());
                this.maxThroughputQueue.enQueue(new ThroughputDataPoint(lastCyclePerformance, currentTimeMilli));
                Double filteredFinishedTaskNumber = this.averageFilter.putAndGet(lastCyclePerformance);
                System.out.println(String.format("Finished tasks: %f", filteredFinishedTaskNumber));
                if (this.isAbnormal(filteredFinishedTaskNumber)) {
                    this.continuousAbnormalCycles += 1;
                    if (this.continuousAbnormalCycles > BasePolicy.MAX_CONTINUOUS_ABNORMAL_CYCLE) {
                        need = true;
                        this.started = false;
                    }
                }
                else {
                    this.continuousAbnormalCycles = 0L;
                }
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
            for (Map.Entry<ResourceName, Double> entry : resourceContentionLevel.entrySet()) {
                System.out.println(entry.getKey() + "'s contention level is " + entry.getValue());
            }
            Map<CancellableID, Long> cancellableGroupResourceResourceUsage = this.infoCenter.getCancellableGroupResourceUsage(resourceName);
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
        else if (!this.infoCenter.isCancellable(target)) {
            System.out.println(target.toString() + " is not cancellable");
            target = new CancellableID();
        }

        return target;
    }

    class AverageFilter {

        private final int size;

        private long[] buffer;

        private int currentIndex;

        public AverageFilter(int size) {
            this.size = size;
            this.buffer = new long[this.size];
            Arrays.fill(this.buffer, 0);
            this.currentIndex = 0;
        }

        public <T> Double putAndGet(long input) {
            this.buffer[currentIndex] = input;
            currentIndex = (currentIndex + 1) % this.size;
            return Arrays.stream(this.buffer).average().getAsDouble();
        }

        public void clear() {
            Arrays.fill(this.buffer, 0);
        }
    }

    class ThroughputDataPoint implements Comparator<ThroughputDataPoint> {

        private final Long throughput;

        private final Long timestamp;

        public ThroughputDataPoint(Long throughput, Long timestamp) {
            this.throughput = throughput;
            this.timestamp = timestamp;
        }

        public Long getThroughput() {
            return this.throughput;
        }

        public Long getTimestamp() {
            return this.timestamp;
        }

        public int compare(ThroughputDataPoint dataPoint1, ThroughputDataPoint dataPoint2) {
            return dataPoint1.getThroughput().compareTo(dataPoint2.getThroughput());
        }

        public Boolean isExpired() {
            return BasePolicy.PAST_PERFORMANCE_REF_MILLI.compareTo(System.currentTimeMillis() - this.timestamp) < 0;
        }
    }

    class FixSizePriorityQueue<T> {

        private final int size;

        private final Comparator<T> comparator;

        private Queue<T> minQueue;

        public FixSizePriorityQueue(int size, Comparator<T> comparator) {
            this.size = size;
            this.comparator = comparator;
            this.minQueue = new PriorityQueue<>(this.comparator);
        }

        public void enQueue(T e) {
            if (this.minQueue.size() < this.size) {
                this.minQueue.add(e);
            }
            else {
                T minElement = this.minQueue.poll();
                if (minElement != null) {
                    if (this.comparator.compare(e, minElement) > 0) {
                        this.minQueue.add(e);
                    }
                    else {
                        this.minQueue.add(minElement);
                    }
                }
            }
        }

        public void removeIf(Predicate<T> filter) {
            this.minQueue.removeIf(filter);
        }

        public void clear() {
            this.minQueue.clear();
        }

        public Double sum(Function<T, Double> mapToDouble) {
            Double sumDouble = 0.0;
            for (T element : this.minQueue) {
                sumDouble += mapToDouble.apply(element);
            }
            return sumDouble;
        }

        public Double mean(Function<T, Double> mapToDouble) {
            Double meanDouble = 0.0;
            int queueSize = this.minQueue.size();
            if (queueSize > 0) {
                Double sumDouble = 0.0;
                for (T element : this.minQueue) {
                    sumDouble += mapToDouble.apply(element);
                }
                meanDouble = sumDouble / queueSize;
            }
            return meanDouble;
        }
    }

    class PerformanceBuffer {

        private final long outputCycleMilli;

        private long lastCycleTimestamp;

        private long bufferedPerformance;

        public PerformanceBuffer(long outputCycleMilli) {
            this.outputCycleMilli = outputCycleMilli;
            this.lastCycleTimestamp = System.currentTimeMillis();
            this.bufferedPerformance = 0L;
        }

        public long lastCyclePerformance(long timestamp, long performance) {
            long outputPerformance = -1L;
            if (timestamp - this.lastCycleTimestamp > this.outputCycleMilli) {
                outputPerformance = this.bufferedPerformance + performance;
                this.bufferedPerformance = 0;
                this.lastCycleTimestamp = timestamp;
            }
            else {
                this.bufferedPerformance += performance;
            }
            return outputPerformance;
        }

        public void clear() {
            this.bufferedPerformance = 0L;
        }
    }
}
