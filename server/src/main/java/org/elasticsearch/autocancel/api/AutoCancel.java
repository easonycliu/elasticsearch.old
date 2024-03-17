package org.elasticsearch.autocancel.api;

import java.util.function.BiFunction;
import java.util.function.Consumer;

import org.elasticsearch.autocancel.manager.MainManager;
import org.elasticsearch.autocancel.utils.Settings;
import org.elasticsearch.autocancel.utils.id.CancellableID;
import org.elasticsearch.autocancel.utils.logger.Logger;
import org.elasticsearch.autocancel.utils.resource.QueueEvent;

public class AutoCancel {
	private static Boolean started = false;

	private static Boolean warnNotStarted = true;

	private static MainManager mainManager = new MainManager();

	private static TaskTracker taskTracker = null;

	private static Resource resourceTracker = new Resource(AutoCancel.mainManager);

	private static Control controller = null;

	private static RequestManager requestManager = new RequestManager();

	public static void start(BiFunction<Object, Object, TaskInfo> taskInfoFunction, Consumer<Object> canceller) {
		if (Settings.getFromJVMOrDefault("autocancel.start", "true").equals("true")) {
			AutoCancel.mainManager.start(null);
			AutoCancel.taskTracker = new TaskTracker(mainManager, taskInfoFunction);
			AutoCancel.controller = new Control(canceller);
		}
	}

	public static void setRequestSender(Consumer<Object> requestSender) {
		AutoCancel.requestManager.setRequestSender(requestSender);
	}

	public static void onRequestReceive(Object task, Object request) {
		if (AutoCancel.started) {
			TaskInfo taskInfo = AutoCancel.taskTracker.getTaskInfo(task);
			CancellableID cid = taskInfo.getTaskID();
			if (!cid.equals(new CancellableID())) {
				if (request != null) {
					AutoCancel.requestManager.onRequestReceive(cid, request);
				}
			} else {
				System.out.println("Cannot find cancellable id from task");
			}
		} else if (warnNotStarted) {
			Logger.systemWarn("You should start lib AutoCancel first.");
			AutoCancel.warnNotStarted = false;
		}
	}

	public static void reexecuteRequestOfTask(CancellableID cid) {
		if (AutoCancel.started) {
			AutoCancel.requestManager.reexecuteRequestOfTask(cid);
		} else if (warnNotStarted) {
			Logger.systemWarn("You should start lib AutoCancel first.");
			AutoCancel.warnNotStarted = false;
		}
	}

	public static void doStart() {
		started = true;
		Logger.systemInfo("AutoCancel started.");
	}

	public static void stop() {
		AutoCancel.taskTracker.stop();
		AutoCancel.mainManager.stop();
	}

	public static void onTaskCreate(Object task, Object request) {
		if (AutoCancel.started) {
			AutoCancel.taskTracker.onTaskCreate(task, request);
		} else if (warnNotStarted) {
			Logger.systemWarn("You should start lib AutoCancel first.");
			AutoCancel.warnNotStarted = false;
		}
	}

	public static void onTaskExit(Object task) {
		if (AutoCancel.started) {
			AutoCancel.taskTracker.onTaskExit(task);
		} else if (warnNotStarted) {
			Logger.systemWarn("You should start lib AutoCancel first.");
			AutoCancel.warnNotStarted = false;
		}
	}

	public static void onTaskFinishInThread() {
		if (AutoCancel.started) {
			AutoCancel.taskTracker.onTaskFinishInThread();
		} else if (warnNotStarted) {
			Logger.systemWarn("You should start lib AutoCancel first.");
			AutoCancel.warnNotStarted = false;
		}
	}

	public static void onTaskQueueInThread(Runnable runnable) {
		if (AutoCancel.started) {
			AutoCancel.taskTracker.onTaskQueueInThread(runnable);
		} else if (warnNotStarted) {
			Logger.systemWarn("You should start lib AutoCancel first.");
			AutoCancel.warnNotStarted = false;
		}
	}

	public static void onTaskStartInThread(Runnable runnable) {
		if (AutoCancel.started) {
			AutoCancel.taskTracker.onTaskStartInThread(runnable);
		} else if (warnNotStarted) {
			Logger.systemWarn("You should start lib AutoCancel first.");
			AutoCancel.warnNotStarted = false;
		}
	}

	public static void addTaskWork(Long work) {
		if (AutoCancel.started) {
			AutoCancel.taskTracker.addTaskWork(work);
		} else if (warnNotStarted) {
			Logger.systemWarn("You should start lib AutoCancel first.");
			AutoCancel.warnNotStarted = false;
		}
	}

	public static void finishTaskWork(Long work) {
		if (AutoCancel.started) {
			AutoCancel.taskTracker.finishTaskWork(work);
		} else if (warnNotStarted) {
			Logger.systemWarn("You should start lib AutoCancel first.");
			AutoCancel.warnNotStarted = false;
		}
	}

	public static void startCPUUsing(String name) {
		if (AutoCancel.started) {
			AutoCancel.resourceTracker.startCPUUsing(name);
		} else if (warnNotStarted) {
			Logger.systemWarn("You should start lib AutoCancel first.");
			AutoCancel.warnNotStarted = false;
		}
	}

	public static void endCPUUsing(String name) {
		if (AutoCancel.started) {
			AutoCancel.resourceTracker.endCPUUsing(name);
		} else if (warnNotStarted) {
			Logger.systemWarn("You should start lib AutoCancel first.");
			AutoCancel.warnNotStarted = false;
		}
	}

	public static void addMemoryUsage(
			String name, Long evictTime, Long totalMemory, Long usingMemory, Long reuseMemory) {
		if (AutoCancel.started) {
			AutoCancel.resourceTracker.addMemoryUsage(name, evictTime, totalMemory, usingMemory, reuseMemory);
		} else if (warnNotStarted) {
			Logger.systemWarn("You should start lib AutoCancel first.");
			AutoCancel.warnNotStarted = false;
		}
	}

	public static void startQueueWait(String name) {
		if (AutoCancel.started) {
			AutoCancel.resourceTracker.startQueueEvent(name, QueueEvent.QUEUE);
		} else if (warnNotStarted) {
			Logger.systemWarn("You should start lib AutoCancel first.");
			AutoCancel.warnNotStarted = false;
		}
	}

	public static void endQueueWait(String name) {
		if (AutoCancel.started) {
			AutoCancel.resourceTracker.endQueueEvent(name, QueueEvent.QUEUE);
		} else if (warnNotStarted) {
			Logger.systemWarn("You should start lib AutoCancel first.");
			AutoCancel.warnNotStarted = false;
		}
	}

	public static void startQueueOccupy(String name) {
		if (AutoCancel.started) {
			AutoCancel.resourceTracker.startQueueEvent(name, QueueEvent.OCCUPY);
		} else if (warnNotStarted) {
			Logger.systemWarn("You should start lib AutoCancel first.");
			AutoCancel.warnNotStarted = false;
		}
	}

	public static void endQueueOccupy(String name) {
		if (AutoCancel.started) {
			AutoCancel.resourceTracker.endQueueEvent(name, QueueEvent.OCCUPY);
		} else if (warnNotStarted) {
			Logger.systemWarn("You should start lib AutoCancel first.");
			AutoCancel.warnNotStarted = false;
		}
	}

	public static void cancel(CancellableID cid) {
		if (AutoCancel.started) {
			TaskInfo taskInfo = AutoCancel.taskTracker.getTaskInfo(cid);
			if (taskInfo != null) {
				AutoCancel.controller.cancel(taskInfo.getTask());
			} else {
				System.out.println("Cannot get task info from task tackrer");
			}
		} else if (warnNotStarted) {
			Logger.systemWarn("You should start lib AutoCancel first.");
			AutoCancel.warnNotStarted = false;
		}
	}
}
