package org.elasticsearch.autocancel.core.performance;

public class Performance {
	private Integer finishedTaskNumber;

	private Long timestampMilli;

	public Performance() {
		this.finishedTaskNumber = 0;
		this.timestampMilli = 0L;
	}

	public Integer increaseFinishedTask() {
		this.finishedTaskNumber += 1;
		return this.finishedTaskNumber;
	}

	public void reset(Long timestampMilli) {
		this.finishedTaskNumber = 0;
		this.timestampMilli = timestampMilli;
	}

	public Integer getFinishedTaskNumber() {
		return this.finishedTaskNumber;
	}

	@Override
	public String toString() {
		Long currentTimeMilli = System.currentTimeMillis();
		return String.format("%d requests has finish since %d to %d the throughput is %f / second",
				this.finishedTaskNumber, this.timestampMilli, currentTimeMilli,
				1000 * Double.valueOf(this.finishedTaskNumber) / (currentTimeMilli - this.timestampMilli));
	}
}