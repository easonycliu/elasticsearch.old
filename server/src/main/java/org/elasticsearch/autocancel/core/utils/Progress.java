package org.elasticsearch.autocancel.core.utils;

import java.util.Map;

public class Progress {
	private Long totalWork;

	private Long finishWork;

	public Progress() {
		this.totalWork = 0L;
		this.finishWork = 0L;
	}

	public void setWorkUpdateInfo(Map<String, Object> workUpdateInfo) {
		for (Map.Entry<String, Object> entry : workUpdateInfo.entrySet()) {
			switch (entry.getKey()) {
				case "add_work":
					this.totalWork += (Long) entry.getValue();
					break;
				case "finish_work":
					this.finishWork += (Long) entry.getValue();
					break;
				default:
					System.out.println(String.format("Failed to handle update info %s", entry.getKey()));
					break;
			}
		}
	}

	public Double getProgress() {
		Double progress = 0.0;
		if (this.finishWork.equals(0L) || this.totalWork.equals(0L)) {
			// It's likely we failed to trace a task
			// So we can only guess a number as the progress
			progress = 0.5;
		} else {
			progress = Double.valueOf(this.finishWork) / this.totalWork;
		}
		return progress;
	}
}
