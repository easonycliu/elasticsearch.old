package org.elasticsearch.autocancel.api;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import org.elasticsearch.autocancel.utils.id.CancellableID;

public class TaskInfo {
	private Object task;

	private CancellableID taskID;

	private CancellableID parentID;

	private String action;

	private Long startTimeNano;

	private Long startTime;

	private Boolean isCancellable;

	private Supplier<Boolean> isCanceled;

	private String name;

	private RequestInfo requestInfo;

	public TaskInfo(Object task, Long taskID, Long parentID, String action, Long startTimeNano, Long startTime,
			Boolean isCancellable, Supplier<Boolean> isCanceled, String name, RequestInfo requestInfo) {
		this.task = task;
		this.taskID = new CancellableID(taskID);
		this.parentID = new CancellableID(parentID);
		this.action = action;
		this.startTimeNano = startTimeNano;
		this.startTime = startTime;
		this.isCancellable = isCancellable;
		this.isCanceled = isCanceled;
		this.name = name;
		this.requestInfo = requestInfo;
	}

	public Object getTask() {
		return this.task;
	}

	public CancellableID getParentTaskID() {
		return this.parentID;
	}

	public CancellableID getTaskID() {
		assert this.taskID.isValid() : "Task id should never be invalid";
		return this.taskID;
	}

	public String getAction() {
		return this.action;
	}

	public Long getStartTimeNano() {
		return this.startTimeNano;
	}

	public Long getStartTime() {
		return this.startTime;
	}

	public Boolean getIsCancellable() {
		return this.isCancellable;
	}

	public Supplier<Boolean> getIsCanceled() {
		return this.isCanceled;
	}

	public String getName() {
		return this.name;
	}

	public Boolean hasRequestInfo() {
		return this.requestInfo != null;
	}

	public String getPath() {
		String path = null;
		if (this.hasRequestInfo()) {
			path = this.requestInfo.getPath();
		}
		return path;
	}

	public Map<String, List<String>> getHeaders() {
		Map<String, List<String>> headers = null;
		if (this.hasRequestInfo()) {
			headers = this.requestInfo.getHeaders();
		}
		return headers;
	}

	public Map<String, String> getParams() {
		Map<String, String> params = null;
		if (this.hasRequestInfo()) {
			params = this.requestInfo.getParams();
		}
		return params;
	}

	public String getContent() {
		String content = null;
		if (this.hasRequestInfo()) {
			content = this.requestInfo.getContent();
		}
		return content;
	}

	public static class RequestInfo {
		private String path;

		private Map<String, List<String>> headers;

		private Map<String, String> params;

		private String content;

		public RequestInfo(String path, Map<String, List<String>> headers, Map<String, String> params, String content) {
			this.path = path;
			this.headers = headers;
			this.params = params;
			this.content = content;
		}

		public String getPath() {
			return this.path;
		}

		public Map<String, List<String>> getHeaders() {
			return this.headers;
		}

		public Map<String, String> getParams() {
			return this.params;
		}

		public String getContent() {
			return this.content;
		}
	}
}
