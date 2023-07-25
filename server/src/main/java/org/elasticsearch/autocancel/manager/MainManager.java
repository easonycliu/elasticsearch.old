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
import org.elasticsearch.autocancel.utils.CancellableID;
import org.elasticsearch.autocancel.utils.JavaThreadID;
import org.elasticsearch.autocancel.utils.ReleasableLock;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.Queue;

public class MainManager {

    private Queue<String> buffer;

    private IDManager idManager;

    private InfrastructureManager infrastructureManager;

    public MainManager() {
        AutoCancelCore autoCancelCore = new AutoCancelCore(this);
    }

    public void startNewVersion() {

    }

    public void start() {

    }

    public void stop() {

    }

    private CancellableID createCancellable(JavaThreadID jid) {
        CancellableID cid = new CancellableID();
        this.idManager.setCancellableIDAndJavaThreadID(cid, jid);
        // TODO: Connect AutoCancelCore
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

        assert !cid.equals(new CancellableID()) : "Cannot register an invalid cancellable id.";

        this.idManager.setCancellableIDAndJavaThreadID(cid, jid);
    }

    public void unregisterCancellableIDOnCurrentJavaThreadID() {
        JavaThreadID jid = new JavaThreadID(Thread.currentThread().getId());
        CancellableID cid = this.idManager.getCancellableIDOfJavaThreadID(jid);

        assert !cid.equals(new CancellableID()) : "Task must be running before finishing.";

        this.idManager.setCancellableIDAndJavaThreadID(new CancellableID(), jid);
    }

    public CancellableID createCancellableIDOnCurrentJavaThreadID() {
        JavaThreadID jid = new JavaThreadID(Thread.currentThread().getId());
        CancellableID cid = this.createCancellable(jid);

        return cid;
    }

    public void destoryCancellableIDOnCurrentJavaThreadID(CancellableID cid) {
        // TODO: Connect AutoCancelCore
        List<JavaThreadID> javaThreadIDs = this.idManager.getAllJavaThreadIDOfCancellableID(cid);
        List<Long> changeTimes = this.idManager.getAllChangeTimeOfCancellableID(cid);

        assert changeTimes.size() == javaThreadIDs.size();

        try {
            FileWriter jidInfo = new FileWriter("/tmp/jidinfo");
            jidInfo.write(String.format("=================== Cancellable %s has been destoried ===================",
                    cid.toString()));
            for (Integer i = 0; i < javaThreadIDs.size(); ++i) {
                jidInfo.write(String.format("System nano : %d. Java thread %s", changeTimes.get(i),
                        javaThreadIDs.get(i).toString()));
            }
            jidInfo.close();
        } catch (IOException e) {

        }
    }

    public CancellableID getCancellableIDOnCurrentJavaThreadID() {
        JavaThreadID jid = new JavaThreadID(Thread.currentThread().getId());
        CancellableID cid = this.idManager.getCancellableIDOfJavaThreadID(jid);

        return cid;
    }

}
