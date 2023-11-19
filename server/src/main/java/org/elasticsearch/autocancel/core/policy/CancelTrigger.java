package org.elasticsearch.autocancel.core.policy;

import java.util.Arrays;
import java.util.Comparator;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.function.Function;
import java.util.function.Predicate;

public class CancelTrigger {
    private static final Double ABNORMAL_PERFORMANCE_DROP_PROTION = 0.4;

    private static final Double ABNORMAL_PERFORMANCE_DROP_ABSOLUTE = 300.0;

    private static final Long ONE_CYCLE_MILLI = 1000L;

    private static final Long MAX_CONTINUOUS_ABNORMAL_CYCLE = 3L;

    private static final Long PAST_PERFORMANCE_REF_CYCLE = 30L;

    private static final Integer MAX_PAST_PERFORMANCE_REF_NUM = 3;

    private static final Integer AVERAGE_FILTER_SIZE = 3;

    private AverageFilter averageFilter;

    private Boolean started;

    private PerformanceBuffer performanceBuffer;

    private Long continuousAbnormalCycles;

    private FixSizePriorityQueue<ThroughputDataPoint> maxThroughputQueue;

    public CancelTrigger() {
        this.averageFilter = new AverageFilter(CancelTrigger.AVERAGE_FILTER_SIZE);
        this.started = false;
        this.performanceBuffer = new PerformanceBuffer(CancelTrigger.ONE_CYCLE_MILLI);
        this.continuousAbnormalCycles = 0L;
        this.maxThroughputQueue = new FixSizePriorityQueue<ThroughputDataPoint>(
            CancelTrigger.MAX_PAST_PERFORMANCE_REF_NUM, 
            (e1, e2) -> e1.getThroughput().intValue() - e2.getThroughput().intValue()
        );
    }

    public Boolean isAbnormal(Double throughput) {
        Boolean abnormal = false;
        Double normalThroughput = this.maxThroughputQueue.mean((element) -> Double.valueOf(element.getThroughput()));
        if (normalThroughput * (1.0 - CancelTrigger.ABNORMAL_PERFORMANCE_DROP_PROTION) > throughput) {
            abnormal = true;
        }
        if (normalThroughput - CancelTrigger.ABNORMAL_PERFORMANCE_DROP_ABSOLUTE > throughput) {
            abnormal = true;
        }
        return abnormal;
    }

    public Boolean triggered(long finishedTaskNumber) {
        Boolean need = false;
        if (!this.started) {
            this.started = (finishedTaskNumber != 0);
            if (this.started) {
                this.averageFilter.clear();
                this.performanceBuffer.clear();
                this.continuousAbnormalCycles = 0L;
                // this.maxThroughputQueue.clear();

                this.performanceBuffer.lastCyclePerformance(System.currentTimeMillis(), finishedTaskNumber);
            }
        }
        else {
            long currentTimeMilli = System.currentTimeMillis();
            long lastCyclePerformance = this.performanceBuffer.lastCyclePerformance(currentTimeMilli, finishedTaskNumber);
            if (lastCyclePerformance >= 0) {
                this.maxThroughputQueue.removeIf((element) -> element.isExpired());
                this.maxThroughputQueue.enQueue(new ThroughputDataPoint(lastCyclePerformance, currentTimeMilli));
                Double filteredFinishedTaskNumber = this.averageFilter.putAndGet(lastCyclePerformance);
                Boolean abnormal = this.isAbnormal(filteredFinishedTaskNumber);
                System.out.println(String.format("Finished tasks: %f, Abnormal: %b", filteredFinishedTaskNumber, abnormal));
                if (abnormal) {
                    this.continuousAbnormalCycles += 1;
                    if (this.continuousAbnormalCycles > CancelTrigger.MAX_CONTINUOUS_ABNORMAL_CYCLE) {
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
            return CancelTrigger.PAST_PERFORMANCE_REF_CYCLE.compareTo((System.currentTimeMillis() - this.timestamp) / CancelTrigger.ONE_CYCLE_MILLI) < 0;
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
