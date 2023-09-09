/*
 * This is the main class of manager layer
 * To Adaptor Layer:
 *     Manage ID
 *     Manage Infrastructure
 * To Core Layer:
 *     Provide data / control api through CID
 */

package org.elasticsearch.autocancel.manager;

import org.elasticsearch.autocancel.app.elasticsearch.AutoCancel;
import org.elasticsearch.autocancel.core.AutoCancelCore;
import org.elasticsearch.autocancel.core.AutoCancelCoreHolder;
import org.elasticsearch.autocancel.core.AutoCancelInfoCenter;
import org.elasticsearch.autocancel.core.utils.OperationMethod;
import org.elasticsearch.autocancel.core.utils.OperationRequest;
import org.elasticsearch.autocancel.utils.Policy;
import org.elasticsearch.autocancel.core.policy.BasePolicy;
import org.elasticsearch.autocancel.utils.ReleasableLock;
import org.elasticsearch.autocancel.utils.Settings;
import org.elasticsearch.autocancel.utils.id.CancellableID;
import org.elasticsearch.autocancel.utils.id.CancellableIDGenerator;
import org.elasticsearch.autocancel.utils.id.JavaThreadID;
import org.elasticsearch.autocancel.utils.logger.Logger;
import org.elasticsearch.autocancel.utils.resource.ResourceName;
import org.elasticsearch.autocancel.utils.resource.ResourceType;
import org.elasticsearch.autocancel.utils.id.IDInfo;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.Map;

import net.openhft.affinity.AffinityLock;

public class MainManager {

    private ConcurrentLinkedQueue<OperationRequest> managerRequestToCoreBuffer;

    private IDManager idManager;

    private InfrastructureManager infrastructureManager;

    private CancellableIDGenerator cidGenerator;

    private Thread autoCancelCoreThread;

    public MainManager() {
        // TODO: Check performance issue and buffer overflow
        // TODO: Check if this async implementation causes cancel after exit
        this.managerRequestToCoreBuffer = new ConcurrentLinkedQueue<OperationRequest>();
        this.idManager = new IDManager();
        this.infrastructureManager = new InfrastructureManager();
        this.cidGenerator = new CancellableIDGenerator();
        this.autoCancelCoreThread = null;
    }

    public void start(Policy policy) throws AssertionError {
        assert this.autoCancelCoreThread == null : "AutoCancel core thread has been started";
        this.autoCancelCoreThread = new Thread() {
            @Override
            public void run() {
                try (AffinityLock lock = AffinityLock.acquireLock(Runtime.getRuntime().availableProcessors() - 1)) {
                    AutoCancelCore autoCancelCore = AutoCancelCoreHolder.getAutoCancelCore();
                    autoCancelCore.initialize(MainManager.this);
                    Policy actualPolicy = ((policy != null) ? policy : new BasePolicy());
                    while (!Thread.interrupted()) {
                        try {
                            autoCancelCore.startOneLoop();

                            if (actualPolicy.needCancellation()) {
                                CancellableID targetCID = actualPolicy.getCancelTarget();
                                AutoCancel.cancel(targetCID);
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
        }
        catch (InterruptedException e) {
            System.out.println("Giving up waiting for autocancel lib thread to join");
        }
    }

    public void startNewVersion() {
        this.infrastructureManager.startNewVersion();
    }

    private CancellableID createCancellable(JavaThreadID jid, Boolean isCancellable, String name, String action,
            CancellableID parentID) {
        CancellableID cid = this.cidGenerator.generate();
        this.idManager.setCancellableIDAndJavaThreadID(cid, jid, IDInfo.Status.RUN);

        OperationRequest request = new OperationRequest(OperationMethod.CREATE,
                Map.of("cancellable_id", cid, "parent_cancellable_id", parentID));
        request.addRequestParam("is_cancellable", isCancellable);
        request.addRequestParam("cancellable_name", name);
        request.addRequestParam("cancellable_action", action);
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

        this.idManager.setCancellableIDAndJavaThreadID(cid, jid, IDInfo.Status.RUN);
    }

    public void unregisterCancellableIDOnCurrentJavaThreadID() {
        JavaThreadID jid = new JavaThreadID(Thread.currentThread().getId());
        CancellableID cid = this.idManager.getCancellableIDOfJavaThreadID(jid);

        assert cid.isValid() : "Task must be running before finishing.";

        this.idManager.setCancellableIDAndJavaThreadID(cid, jid, IDInfo.Status.EXIT);
    }

    public CancellableID createCancellableIDOnCurrentJavaThreadID(Boolean isCancellable, String name, String action,
            CancellableID parentID) {
        JavaThreadID jid = new JavaThreadID(Thread.currentThread().getId());
        CancellableID cid = this.createCancellable(jid, isCancellable, name, action, parentID);

        return cid;
    }

    public void destoryCancellableIDOnCurrentJavaThreadID(CancellableID cid) {
        JavaThreadID jid = new JavaThreadID(Thread.currentThread().getId());
        CancellableID cidReadFromManager = this.idManager.getCancellableIDOfJavaThreadID(jid);

        boolean cidEqual = cid.equals(cidReadFromManager);
        if (!cidEqual) {
            Logger.systemTrace("Input " + cid.toString() + " is not running on the current " + jid.toString()
                    + " whose " + cidReadFromManager.toString());
        }
        // assert cidEqual : "Input cancellable id is not running on the current java
        // thread id";

        this.idManager.setCancellableIDAndJavaThreadID(cid, jid, IDInfo.Status.EXIT);

        OperationRequest request = new OperationRequest(OperationMethod.DELETE, Map.of("cancellable_id", cid));
        this.putManagerRequestToCore(request);
    }

    public CancellableID getCancellableIDOnCurrentJavaThreadID() {
        JavaThreadID jid = new JavaThreadID(Thread.currentThread().getId());
        CancellableID cid = this.idManager.getCancellableIDOfJavaThreadID(jid);

        return cid;
    }

    public List<IDInfo<JavaThreadID>> getAllJavaThreadIDInfoOfCancellableID(CancellableID cid) {
        return this.idManager.getAllJavaThreadIDInfoOfCancellableID(cid);
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
                resourceUpdateInfos.add(this.infrastructureManager.getSpecifiedResourceLatest(javaThreadID, resourceName));
            }
        }
        return resourceUpdateInfos;
    }

    public void updateCancellableGroup(ResourceType type, String name, Map<String, Object> cancellableGroupUpdateInfo) {
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
}
