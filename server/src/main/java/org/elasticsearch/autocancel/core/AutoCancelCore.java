package org.elasticsearch.autocancel.core;

import java.lang.Thread;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.elasticsearch.autocancel.core.monitor.MainMonitor;
import org.elasticsearch.autocancel.core.performance.Performance;
import org.elasticsearch.autocancel.core.utils.Cancellable;
import org.elasticsearch.autocancel.core.utils.CancellableGroup;
import org.elasticsearch.autocancel.core.utils.OperationRequest;
import org.elasticsearch.autocancel.core.utils.ResourcePool;
import org.elasticsearch.autocancel.manager.MainManager;
import org.elasticsearch.autocancel.utils.Settings;
import org.elasticsearch.autocancel.utils.id.CancellableID;
import org.elasticsearch.autocancel.utils.logger.Logger;
import org.elasticsearch.autocancel.utils.resource.JVMHeapResource;
import org.elasticsearch.autocancel.utils.resource.Resource;
import org.elasticsearch.autocancel.utils.resource.ResourceName;
import org.elasticsearch.autocancel.utils.resource.ResourceType;

public class AutoCancelCore {
	private MainManager mainManager = null;

	private MainMonitor mainMonitor;

	private Map<CancellableID, CancellableGroup> rootCancellableToCancellableGroup;

	private Map<CancellableID, Cancellable> cancellables;

	private Map<CancellableID, CancellableGroup> toBeReexecutedCancellableGroups;

	private ResourcePool resourcePool;

	private Performance performanceMetrix;

	private RequestParser requestParser;

	private Logger logger;

	private AutoCancelInfoCenter infoCenter;

	public AutoCancelCore(MainManager mainManager) {
		this.cancellables = new HashMap<CancellableID, Cancellable>();
		this.rootCancellableToCancellableGroup = new HashMap<CancellableID, CancellableGroup>();
		this.toBeReexecutedCancellableGroups = new HashMap<CancellableID, CancellableGroup>();
		this.requestParser = new RequestParser();
		this.logger = new Logger("corerequest");
		this.performanceMetrix = new Performance();
		this.resourcePool = new ResourcePool(true);

		this.resourcePool.addBuiltinResource();

		this.infoCenter = new AutoCancelInfoCenter(
				this.rootCancellableToCancellableGroup, this.cancellables, this.resourcePool, this.performanceMetrix);

		this.initialize(mainManager);
	}

	public AutoCancelCore() {
		this.cancellables = new HashMap<CancellableID, Cancellable>();
		this.rootCancellableToCancellableGroup = new HashMap<CancellableID, CancellableGroup>();
		this.toBeReexecutedCancellableGroups = new HashMap<CancellableID, CancellableGroup>();
		this.requestParser = new RequestParser();
		this.logger = new Logger("corerequest");
		this.performanceMetrix = new Performance();
		this.resourcePool = new ResourcePool(true);

		this.resourcePool.addBuiltinResource();

		this.infoCenter = new AutoCancelInfoCenter(
				this.rootCancellableToCancellableGroup, this.cancellables, this.resourcePool, this.performanceMetrix);
	}

	public void initialize(MainManager mainManager) {
		if (this.mainManager == null) {
			this.mainManager = mainManager;
			this.mainMonitor =
					new MainMonitor(this.mainManager, this.cancellables, this.rootCancellableToCancellableGroup);
		}
	}

	public Boolean isInitialized() {
		return this.mainManager != null;
	}

	public void start() {
		while (!Thread.interrupted()) {
			try {
				this.startOneLoop();

				Thread.sleep((Long) Settings.getSetting("core_update_cycle_ms"));
			} catch (InterruptedException e) {
				break;
			}
		}
		this.stop();
	}

	public void startOneLoop() {
		if (this.isInitialized()) {
			Map<String, Object> refreshInfo = this.addCoreLevelRefreshInfo();

			this.refreshCancellableGroups(refreshInfo);

			this.resourcePool.refreshResources(refreshInfo, this.logger);

			this.logger.log(this.performanceMetrix.toString());

			Long timestampMilli = System.currentTimeMillis();

			this.performanceMetrix.reset(timestampMilli);

			this.logger.log(String.format("Current time: %d", timestampMilli));

			CancellableGroup.cancellableGroupLog(String.format("Current time: %d", timestampMilli));

			// refresh stuff should be done before update
			Integer requestBufferSize = this.mainManager.getManagerRequestToCoreBufferSize();
			for (Integer ignore = 0; ignore < requestBufferSize; ++ignore) {
				OperationRequest request = this.mainManager.getManagerRequestToCore();
				this.requestParser.parse(request);
			}

			this.mainMonitor.updateTasksResources();

			Integer updateBufferSize = this.mainMonitor.getMonitorUpdateToCoreBufferSizeWithoutLock();
			for (Integer ignore = 0; ignore < updateBufferSize; ++ignore) {
				OperationRequest request = this.mainMonitor.getMonitorUpdateToCoreWithoutLock();
				this.requestParser.parse(request);
			}
		} else {
			Logger.systemWarn("AutoCancelCore hasn't initialized, use initialize() first");
		}
	}

	AutoCancelInfoCenter getInfoCenter() {
		if (this.isInitialized()) {
			return this.infoCenter;
		} else {
			Logger.systemWarn("AutoCancelCore hasn't initialized, use initialize() first");
			return null;
		}
	}

	public void stop() {
		if (this.isInitialized()) {
			this.logger.close();
			System.out.println("Recieve interrupt, exit");
		} else {
			Logger.systemWarn("AutoCancelCore hasn't initialized, use initialize() first");
		}
	}

	public Vector<CancellableID> scheduleCancellableGroups() {
		Vector<CancellableID> toBeReexecutedRootCancellableIDs = new Vector<CancellableID>();
		Vector<CancellableID> toBeRemovedReexecutionCancellableGroups = new Vector<CancellableID>();
		for (Map.Entry<CancellableID, CancellableGroup> toBeReexecutedCancellableGroup :
				this.toBeReexecutedCancellableGroups.entrySet()) {
			assert toBeReexecutedCancellableGroup.getValue().isCancelled()
				: "Should get cancelled before adding into to be reexecuted group";
			if (System.nanoTime() - toBeReexecutedCancellableGroup.getValue().getCancelTimeNano()
					> ((Long) Settings.getSetting("reexecute_after_ms") * 1000000)) {
				toBeReexecutedRootCancellableIDs.add(toBeReexecutedCancellableGroup.getKey());
				toBeRemovedReexecutionCancellableGroups.add(toBeReexecutedCancellableGroup.getKey());
			}
		}

		for (CancellableID toBeRemovedReexecutionCancellableGroup : toBeRemovedReexecutionCancellableGroups) {
			this.toBeReexecutedCancellableGroups.remove(toBeRemovedReexecutionCancellableGroup);
		}

		return toBeReexecutedRootCancellableIDs;
	}

	private void refreshCancellableGroups(Map<String, Object> refreshInfo) {
		List<CancellableGroup> toBeRemovedCancellableGroups = new ArrayList<CancellableGroup>();
		for (Map.Entry<CancellableID, CancellableGroup> entry : this.rootCancellableToCancellableGroup.entrySet()) {
			if (entry.getValue().isCancelled() && !this.toBeReexecutedCancellableGroups.containsKey(entry.getKey())) {
				this.toBeReexecutedCancellableGroups.put(entry.getKey(), entry.getValue());
			}

			if (entry.getValue().isExit()) {
				toBeRemovedCancellableGroups.add(entry.getValue());
				continue;
			}

			entry.getValue().refreshResourcePool(refreshInfo);
		}

		for (CancellableGroup cancellableGroup : toBeRemovedCancellableGroups) {
			if (cancellableGroup.isExpired()) {
				this.removeCancellableGroup(cancellableGroup);
			}
		}
	}

	protected void addCancellable(Cancellable cancellable) {
		this.cancellables.put(cancellable.getID(), cancellable);

		Logger.systemTrace("Add cancellable with " + cancellable.toString());

		if (cancellable.isRoot()) {
			this.rootCancellableToCancellableGroup.put(cancellable.getID(), new CancellableGroup(cancellable));
		} else {
			try {
				this.rootCancellableToCancellableGroup.get(cancellable.getRootID()).putCancellable(cancellable);
			} catch (NullPointerException e) {
				System.out.println(
						"Cannot find cancellable group for " + cancellable.toString() + " at " + e.getStackTrace()[0]);
			}
		}
	}

	protected void exitCancellable(Cancellable cancellable) {
		if (cancellable.isRoot()) {
			// TODO: Problematic point: nullptr
			// CancellableGroup cancellableGroup =
			// this.rootCancellableToCancellableGroup.remove(cancellable.getID());
			// // Remove all child cancellables at the same time
			// Collection<Cancellable> childCancellables =
			// cancellableGroup.getChildCancellables();
			// for (Cancellable childCancellable : childCancellables) {
			// this.cancellables.remove(childCancellable.getID());
			// }
			try {
				this.rootCancellableToCancellableGroup.get(cancellable.getID()).exit();
				if (((Set<?>) Settings.getSetting("monitor_actions")).contains(cancellable.getAction())) {
					this.performanceMetrix.increaseFinishedTask();
				}
			} catch (NullPointerException e) {
				System.out.println(
						"Cannot find cancellable group for " + cancellable.toString() + " at " + e.getStackTrace()[0]);
			}
		} else {
			// Don't care about single cancellable
			// We will remove it in removeCancellableGroup when its cancellable group is moved
		}
	}

	private void removeCancellableGroup(CancellableGroup cancellableGroup) {
		if (cancellableGroup != null) {
			Set<CancellableID> childCancellableIDs = cancellableGroup.getChildCancellableIDs();
			childCancellableIDs.forEach((cancellableID) -> { this.cancellables.remove(cancellableID); });
			this.rootCancellableToCancellableGroup.remove(cancellableGroup.getRootID());
		} else {
			System.out.println("Cancellable group to be removed does not exist");
		}
	}

	private Map<String, Object> addCoreLeveUpdateInfo(
			Map<String, Object> resourceUpdateInfo, OperationRequest request) {
		CancellableID cid = request.getCancellableID();
		assert cid.isValid() : "Should use valid cid to update cancellable group";
		Map<String, Object> updateInfo = new HashMap<>(resourceUpdateInfo);
		updateInfo.putIfAbsent("cancellable_id", cid);
		return updateInfo;
	}

	private Map<String, Object> addCoreLevelRefreshInfo() {
		return Map.of("current_gc_time", JVMHeapResource.getTotalGCTime());
	}

	private class RequestParser {
		ParamHandlers paramHandlers;

		public RequestParser() {
			this.paramHandlers = new ParamHandlers();
		}

		public void parse(OperationRequest request) {
			logger.log(request.toString());
			switch (request.getOperation()) {
				case CREATE:
					create(request);
					break;
				case RETRIEVE:
					retrieve(request);
					break;
				case UPDATE:
					update(request);
					break;
				case DELETE:
					delete(request);
					break;
				default:
					break;
			}
		}

		private void create(OperationRequest request) {
			CancellableID parentID = request.getParentCancellableID();

			assert parentID != null : "Must set parent_cancellable_id when create cancellable.";
			assert request.getCancellableID() != new CancellableID() : "Create operation must have cancellable id set";

			CancellableID rootID = this.getRootID(request);
			Cancellable cancellable = new Cancellable(request.getCancellableID(), parentID, rootID);

			addCancellable(cancellable);

			Map<String, Object> params = request.getParams();
			for (String key : params.keySet()) {
				this.paramHandlers.handle(key, request);
			}
		}

		private void retrieve(OperationRequest request) {}

		private void update(OperationRequest request) {
			Map<String, Object> params = request.getParams();
			for (String key : params.keySet()) {
				this.paramHandlers.handle(key, request);
			}
		}

		private void delete(OperationRequest request) {
			assert request.getCancellableID() != new CancellableID() : "Delete operation must have cancellable id set";

			if (cancellables.containsKey(request.getCancellableID())) {
				exitCancellable(cancellables.get(request.getCancellableID()));

				Map<String, Object> params = request.getParams();
				for (String key : params.keySet()) {
					this.paramHandlers.handle(key, request);
				}
			} else {
				// Some task will exit before its child task exit, we will remove all child
				// tasks when root task exit (See exitCancellable())
				// TODO: Find a method to handle it
				System.out.println(String.format("Cancellable id not found: Time: %d, %s", System.currentTimeMillis(),
						request.getCancellableID().toString()));
			}
		}

		private CancellableID getRootID(OperationRequest request) {
			CancellableID parentID = request.getParentCancellableID();
			assert parentID != null : "Must set parent_cancellable_id when create cancellable.";
			CancellableID rootID = null;
			if (!parentID.isValid()) {
				// Itself is a root cancellable
				rootID = request.getCancellableID();
			} else {
				Cancellable parentCancellable = cancellables.get(parentID);
				if (parentCancellable == null) {
					System.out.println(String.format(
							"Failed to find parenct , pretending to be cancellable itself", parentID.toString()));
					rootID = request.getCancellableID();
				} else {
					rootID = cancellables.get(parentID).getRootID();
				}
			}
			return rootID;
		}
	}

	private class ParamHandlers {
		// These parameters' parsing order doesn't matter
		private final Map<String, Consumer<OperationRequest>> paramHandlers =
				Map.ofEntries(Map.entry("basic_info", request -> {}),
						Map.entry("is_cancellable", request -> this.isCancellable(request)),
						Map.entry("is_canceled", request -> this.isCanceled(request)),
						Map.entry("cancellable_name", request -> this.cancellableName(request)),
						Map.entry("cancellable_action", request -> this.cancellableAction(request)),
						Map.entry("cancellable_start_time_nano", request -> this.cancellableStartTimeNano(request)),
						Map.entry("cancellable_start_time", request -> this.cancellableStartTime(request)),
						Map.entry("cancellable_exit_time_nano", request -> this.cancellableExitTimeNano(request)),
						Map.entry("cancellable_exit_time", request -> this.cancellableExitTime(request)),
						Map.entry("update_group_resource", request -> this.updateGroupResource(request)),
						Map.entry("update_group_work", request -> this.updateGroupWork(request)));

		public ParamHandlers() {}

		public void handle(String type, OperationRequest request) {
			assert this.paramHandlers.containsKey(type) : "Invalid parameter " + type;
			this.paramHandlers.get(type).accept(request);
		}

		@SuppressWarnings("unchecked")
		private void isCanceled(OperationRequest request) {
			Cancellable cancellable = cancellables.get(request.getCancellableID());
			if (cancellable.isRoot()) {
				// Set isCanceled for cancellable group
				Supplier<Boolean> isCanceled = (Supplier<Boolean>) request.getParams().get("is_canceled");
				try {
					rootCancellableToCancellableGroup.get(cancellable.getID()).setIsCanceled(isCanceled);
				} catch (NullPointerException e) {
					System.out.println("Cannot find cancellable group for " + cancellable.toString() + " at "
							+ e.getStackTrace()[0]);
				}
			}
		}

		private void isCancellable(OperationRequest request) {
			Cancellable cancellable = cancellables.get(request.getCancellableID());
			if (cancellable.isRoot()) {
				// This is a root cancellable
				// Parameter is_cancellable is useful only if this cancellable is a root
				// cancellable
				// TODO: Add a warning if this is not a root cancellable
				Boolean isCancellable = (Boolean) request.getParams().get("is_cancellable");
				try {
					rootCancellableToCancellableGroup.get(cancellable.getID()).setIsCancellable(isCancellable);
				} catch (NullPointerException e) {
					System.out.println("Cannot find cancellable group for " + cancellable.toString() + " at "
							+ e.getStackTrace()[0]);
				}
			}
		}

		private void cancellableName(OperationRequest request) {
			Cancellable cancellable = cancellables.get(request.getCancellableID());
			String name = (String) request.getParams().get("cancellable_name");
			cancellable.setName(name);
		}

		private void cancellableAction(OperationRequest request) {
			Cancellable cancellable = cancellables.get(request.getCancellableID());
			String action = (String) request.getParams().get("cancellable_action");
			cancellable.setAction(action);
		}

		private void cancellableStartTime(OperationRequest request) {
			Cancellable cancellable = cancellables.get(request.getCancellableID());
			if (cancellable.isRoot()) {
				// This is a root cancellable
				// Parameter cancellable_start_time is useful only if this cancellable is a root
				// cancellable
				// TODO: Add a warning if this is not a root cancellable
				Long startTime = (Long) request.getParams().get("cancellable_start_time");
				rootCancellableToCancellableGroup.get(cancellable.getID()).setStartTime(startTime);
			}
		}

		private void cancellableStartTimeNano(OperationRequest request) {
			Cancellable cancellable = cancellables.get(request.getCancellableID());
			if (cancellable.isRoot()) {
				// This is a root cancellable
				// Parameter cancellable_start_time_nano is useful only if this cancellable is a
				// root cancellable
				// TODO: Add a warning if this is not a root cancellable
				Long startTimeNano = (Long) request.getParams().get("cancellable_start_time_nano");
				rootCancellableToCancellableGroup.get(cancellable.getID()).setStartTimeNano(startTimeNano);
			}
		}

		private void cancellableExitTime(OperationRequest request) {
			Cancellable cancellable = cancellables.get(request.getCancellableID());
			if (cancellable.isRoot()) {
				// This is a root cancellable
				// Parameter cancellable_start_time is useful only if this cancellable is a root
				// cancellable
				// TODO: Add a warning if this is not a root cancellable
				Long exitTime = (Long) request.getParams().get("cancellable_exit_time");
				rootCancellableToCancellableGroup.get(cancellable.getID()).setExitTime(exitTime);
			}
		}

		private void cancellableExitTimeNano(OperationRequest request) {
			Cancellable cancellable = cancellables.get(request.getCancellableID());
			if (cancellable.isRoot()) {
				// This is a root cancellable
				// Parameter cancellable_start_time_nano is useful only if this cancellable is a
				// root cancellable
				// TODO: Add a warning if this is not a root cancellable
				Long exitTimeNano = (Long) request.getParams().get("cancellable_exit_time_nano");
				rootCancellableToCancellableGroup.get(cancellable.getID()).setExitTimeNano(exitTimeNano);
			}
		}

		@SuppressWarnings("unchecked")
		private void updateGroupResource(OperationRequest request) {
			Cancellable cancellable = cancellables.get(request.getCancellableID());
			if (cancellable != null) {
				ResourceType resourceType = request.getResourceType();
				ResourceName resourceName = request.getResourceName();
				if (!resourceName.equals(ResourceName.NULL) && !resourceType.equals(ResourceType.NULL)) {
					Map<String, Object> resourceUpdateInfo = AutoCancelCore.this.addCoreLeveUpdateInfo(
							(Map<String, Object>) request.getParams().get("update_group_resource"), request);
					try {
						rootCancellableToCancellableGroup.get(cancellable.getRootID())
								.updateResource(resourceType, resourceName, resourceUpdateInfo);
						if (!resourcePool.isResourceExist(resourceName)) {
							resourcePool.addResource(Resource.createResource(resourceType, resourceName));
						}
						resourcePool.setResourceUpdateInfo(resourceName, resourceUpdateInfo);
					} catch (NullPointerException e) {
						System.out.println("Cannot find cancellable group for " + cancellable.toString() + " at "
								+ e.getStackTrace()[0]);
					}
				}
			} else {
				// System.out.println("Can't find cancellable for cid " +
				// request.getCancellableID());
			}
		}

		@SuppressWarnings("unchecked")
		private void updateGroupWork(OperationRequest request) {
			try {
				Cancellable cancellable = cancellables.get(request.getCancellableID());
				Map<String, Object> workUpdateInfo = (Map<String, Object>) request.getParams().get("update_group_work");
				rootCancellableToCancellableGroup.get(cancellable.getRootID()).updateWork(workUpdateInfo);
			} catch (NullPointerException e) {
				System.out.println(String.format("Dereference to null pointer at %s", e.getMessage()));
			}
		}
	}
}
