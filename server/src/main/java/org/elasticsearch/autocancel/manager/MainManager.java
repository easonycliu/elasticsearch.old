/*
 * This is the main class of manager layer
 * To Adaptor Layer:
 *     Manage ID
 *     Manage Infrastructure
 * To Core Layer:
 *     Provide data / control api through CID
 */

package org.elasticsearch.autocancel.manager;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import org.elasticsearch.autocancel.api.AutoCancel;
import org.elasticsearch.autocancel.core.AutoCancelCore;
import org.elasticsearch.autocancel.core.AutoCancelCoreHolder;
import org.elasticsearch.autocancel.core.AutoCancelInfoCenter;
import org.elasticsearch.autocancel.core.policy.BasePolicy;
import org.elasticsearch.autocancel.core.policy.CancelLogger;
import org.elasticsearch.autocancel.core.utils.OperationMethod;
import org.elasticsearch.autocancel.core.utils.OperationRequest;
import org.elasticsearch.autocancel.utils.Policy;
import org.elasticsearch.autocancel.utils.ReleasableLock;
import org.elasticsearch.autocancel.utils.Settings;
import org.elasticsearch.autocancel.utils.id.CancellableID;
import org.elasticsearch.autocancel.utils.id.CancellableIDGenerator;
import org.elasticsearch.autocancel.utils.id.IDInfo;
import org.elasticsearch.autocancel.utils.id.JavaThreadID;
import org.elasticsearch.autocancel.utils.logger.Logger;
import org.elasticsearch.autocancel.utils.resource.ResourceName;
import org.elasticsearch.autocancel.utils.resource.ResourceType;

public class MainManager {
	private ConcurrentLinkedQueue<OperationRequest> managerRequestToCoreBuffer;

	private IDManager idManager;

	private InfrastructureManager infrastructureManager;

	private Thread autoCancelCoreThread;

	public MainManager() {
		// TODO: Check performance issue and buffer overflow
		// TODO: Check if this async implementation causes cancel after exit
		this.managerRequestToCoreBuffer = new ConcurrentLinkedQueue<OperationRequest>();
		this.idManager = new IDManager();
		this.infrastructureManager = new InfrastructureManager();
		this.autoCancelCoreThread = null;
	}

	public void start(Policy policy) throws AssertionError {
		assert this.autoCancelCoreThread == null : "AutoCancel core thread has been started";
		this.autoCancelCoreThread = new Thread() {
			@Override
			public void run() {
				Boolean exitWhenSleep = false;
				try {
					Thread.sleep((Long) Settings.getSetting("skip_first_ms"));
				} catch (InterruptedException e) {
					System.out.println(e.toString());
					exitWhenSleep = true;
				}
				if (!exitWhenSleep) {
					System.out.println("Autocancel core start");
					System.out.println(String.format("Policy: %s, Predict: %s", Settings.getSetting("default_policy"),
							Settings.getSetting("predict_progress")));
					CancelLogger.logExperimentHeader();
					AutoCancel.doStart();
					AutoCancelCore autoCancelCore = AutoCancelCoreHolder.getAutoCancelCore();
					autoCancelCore.initialize(MainManager.this);
					Policy actualPolicy = ((policy != null) ? policy : Policy.getPolicyBySetting());
					while (!Thread.interrupted()) {
						try {
							autoCancelCore.startOneLoop();

							if (actualPolicy.needCancellation()) {
								CancellableID targetCID = actualPolicy.getCancelTarget();
								if (targetCID.isValid()) {
									AutoCancel.cancel(targetCID);
								}
							}

							Thread.sleep((Long) Settings.getSetting("core_update_cycle_ms"));
						} catch (InterruptedException e) {
							break;
						}
					}
					autoCancelCore.stop();
				}
			}
		};
		this.autoCancelCoreThread.start();
	}

	public void stop() throws AssertionError {
		assert this.autoCancelCoreThread != null : "AutoCancel core thread has to be started";
		this.autoCancelCoreThread.interrupt();
		try {
			System.out.println("Waiting for autocancel lib thread to join");
			this.autoCancelCoreThread.join();
		} catch (InterruptedException e) {
			System.out.println("Giving up waiting for autocancel lib thread to join");
		}
	}

	public void startNewVersion() {
		this.infrastructureManager.startNewVersion();
	}

	private CancellableID createCancellable(CancellableID cid, JavaThreadID jid, Boolean isCancellable,
			Supplier<Boolean> isCanceled, String name, String action, CancellableID parentID, Long startTimeNano,
			Long startTime) {
		this.idManager.setCancellableIDAndJavaThreadID(cid, jid);

		OperationRequest request = new OperationRequest(
				OperationMethod.CREATE, Map.of("cancellable_id", cid, "parent_cancellable_id", parentID));
		request.addRequestParam("is_cancellable", isCancellable);
		request.addRequestParam("is_canceled", isCanceled);
		request.addRequestParam("cancellable_name", name);
		request.addRequestParam("cancellable_action", action);
		request.addRequestParam("cancellable_start_time_nano", startTimeNano);
		request.addRequestParam("cancellable_start_time", startTime);
		this.putManagerRequestToCore(request);

		return cid;
	}

	// public CancellableID getCancellableIDOfJavaThreadID(JavaThreadID jid) {
	// return this.idManager.getCancellableIDOfJavaThreadID(jid);
	// }

	// public void setCancellableIDAndJavaThreadID(CancellableID cid, JavaThreadID
	// jid) {
	// this.idManager.setCancellableIDAndJavaThreadID(cid, jid);
	// }

	public void registerCancellableIDOnCurrentJavaThreadID(CancellableID cid) {
		JavaThreadID jid = new JavaThreadID(Thread.currentThread().getId());

		assert cid.isValid() : "Cannot register an invalid cancellable id.";

		this.idManager.setCancellableIDAndJavaThreadID(cid, jid);
	}

	public void unregisterCancellableIDOnCurrentJavaThreadID() {
		JavaThreadID jid = new JavaThreadID(Thread.currentThread().getId());
		CancellableID cid = this.idManager.removeJavaThreadID(jid);

		assert cid != null && cid.isValid() : "Task must be running before finishing.";
	}

	public void createCancellableIDOnCurrentJavaThreadID(CancellableID cid, Boolean isCancellable,
			Supplier<Boolean> isCanceled, String name, String action, CancellableID parentID, Long startTimeNano,
			Long startTime) {
		JavaThreadID jid = new JavaThreadID(Thread.currentThread().getId());
		this.createCancellable(cid, jid, isCancellable, isCanceled, name, action, parentID, startTimeNano, startTime);
	}

	public void destoryCancellableIDOnCurrentJavaThreadID(CancellableID cid) {
		// No need to remove cancellable id from id manager
		// unregisterCancellableIDOnCurrentJavaThreadID() should handle it

		OperationRequest request = new OperationRequest(OperationMethod.DELETE, Map.of("cancellable_id", cid));
		request.addRequestParam("cancellable_exit_time", System.currentTimeMillis());
		request.addRequestParam("cancellable_exit_time_nano", System.nanoTime());
		this.putManagerRequestToCore(request);
	}

	public CancellableID getCancellableIDOnCurrentJavaThreadID() {
		JavaThreadID jid = new JavaThreadID(Thread.currentThread().getId());
		CancellableID cid = this.idManager.getCancellableIDOfJavaThreadID(jid);

		return cid;
	}

	public void putManagerRequestToCore(OperationRequest request) {
		this.managerRequestToCoreBuffer.add(request);
	}

	public OperationRequest getManagerRequestToCore() {
		OperationRequest request;
		request = this.managerRequestToCoreBuffer.poll();
		return request;
	}

	public Integer getManagerRequestToCoreBufferSize() {
		Integer size;
		synchronized (this.managerRequestToCoreBuffer) {
			size = this.managerRequestToCoreBuffer.size();
		}
		return size;
	}

	public List<Map<String, Object>> getSpecifiedResource(CancellableID cid, ResourceName resourceName) {
		List<Map<String, Object>> resourceUpdateInfos = new ArrayList<Map<String, Object>>();
		List<JavaThreadID> javaThreadIDs = this.idManager.getJavaThreadIDOfCancellableID(cid);
		for (JavaThreadID javaThreadID : javaThreadIDs) {
			if (javaThreadID.isValid()) {
				resourceUpdateInfos.add(
						this.infrastructureManager.getSpecifiedResourceLatest(javaThreadID, resourceName));
			}
		}
		return resourceUpdateInfos;
	}

	public void updateCancellableGroupResource(
			ResourceType type, String name, Map<String, Object> cancellableGroupUpdateInfo) {
		JavaThreadID jid = new JavaThreadID(Thread.currentThread().getId());
		CancellableID cid = this.idManager.getCancellableIDOfJavaThreadID(jid);
		if (cid.isValid()) {
			OperationRequest request = new OperationRequest(OperationMethod.UPDATE,
					Map.of("cancellable_id", cid, "resource_name", ResourceName.valueOf(name), "resource_type", type));
			request.addRequestParam("update_group_resource", cancellableGroupUpdateInfo);
			this.putManagerRequestToCore(request);
		} else {
			System.out.println("Cannot find cancellable id from current " + jid.toString());
			// TODO: do something more
		}
	}

	public void updateCancellableGroupWork(Map<String, Object> cancellableGroupWorkInfo) {
		JavaThreadID jid = new JavaThreadID(Thread.currentThread().getId());
		CancellableID cid = this.idManager.getCancellableIDOfJavaThreadID(jid);
		if (cid.isValid()) {
			OperationRequest request = new OperationRequest(OperationMethod.UPDATE, Map.of("cancellable_id", cid));
			request.addRequestParam("update_group_work", cancellableGroupWorkInfo);
			this.putManagerRequestToCore(request);
		} else {
			System.out.println("Cannot find cancellable id from current " + jid.toString());
			// TODO: do something more
		}
	}
}
