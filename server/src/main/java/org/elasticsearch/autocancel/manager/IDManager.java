/*
 * Manage ID:
 *     Manage a map between javaTID and CID
 *     This is a dynamic map because a CID can be mapped to different javaTID
 */

package org.elasticsearch.autocancel.manager;

import org.elasticsearch.autocancel.utils.ReleasableLock;
import org.elasticsearch.autocancel.utils.id.CancellableID;
import org.elasticsearch.autocancel.utils.id.JavaThreadID;
import org.elasticsearch.autocancel.utils.id.IDInfo;
import org.elasticsearch.autocancel.utils.logger.Logger;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class IDManager {

    private Map<JavaThreadID, List<IDInfo<CancellableID>>> javaThreadIDToCancellableID;

    private Map<CancellableID, List<IDInfo<JavaThreadID>>> cancellableIDToJavaThreadID;

    private ReadWriteLock idManagerLock;

    private ReleasableLock readLock;

    private ReleasableLock writeLock;

    public IDManager() {
        this.idManagerLock = new ReentrantReadWriteLock();
        this.readLock = new ReleasableLock(idManagerLock.readLock());
        this.writeLock = new ReleasableLock(idManagerLock.writeLock());

        this.javaThreadIDToCancellableID = new HashMap<JavaThreadID, List<IDInfo<CancellableID>>>();
        this.cancellableIDToJavaThreadID = new HashMap<CancellableID, List<IDInfo<JavaThreadID>>>();
    }

    public List<JavaThreadID> getJavaThreadIDOfCancellableID(CancellableID cid) {
        List<JavaThreadID> javaThreadIDs = new ArrayList<JavaThreadID>();

        try (ReleasableLock ignored = this.readLock.acquire()) {
            if (this.cancellableIDToJavaThreadID.containsKey(cid)) {
                for (IDInfo<JavaThreadID> javaThreadIDInfo : this.cancellableIDToJavaThreadID.get(cid)) {
                    switch (javaThreadIDInfo.getStatus()) {
                        case RUN:
                            javaThreadIDs.add(javaThreadIDInfo.getID());
                            break;
                        case QUEUE:
                            break;
                        case EXIT:
                            while (javaThreadIDs.contains(javaThreadIDInfo.getID())) {
                                javaThreadIDs.remove(javaThreadIDInfo.getID());
                            }
                            break;
                        default:
                            break;
                    }
                }
                if (javaThreadIDs.size() == 0) {
                    Logger.systemTrace(String.format("%s is alive but not running on any java threads", cid.toString()));
                    javaThreadIDs.add(new JavaThreadID());
                }
            }
            else {
                javaThreadIDs.add(new JavaThreadID());
            }
        }

        return javaThreadIDs;
    }

    public CancellableID getCancellableIDOfJavaThreadID(JavaThreadID jid) {
        CancellableID cancellableID;

        try (ReleasableLock ignored = this.readLock.acquire()) {
            if (this.javaThreadIDToCancellableID.containsKey(jid)) {
                List<IDInfo<CancellableID>> cancellableIDList = this.javaThreadIDToCancellableID.get(jid);
                IDInfo<CancellableID> cancellableIDInfo = cancellableIDList.get(cancellableIDList.size() - 1);
                if (cancellableIDInfo.isRun()) {
                    cancellableID = cancellableIDInfo.getID();
                }
                else {
                    cancellableID = new CancellableID();
                }
                if (cancellableIDList.get(0).isExit()) {
                    // Exit should not be the first status of the cancellable
                    // TODO: Find the root cause and fix it
                    // System.out.println("Concurrency Error.");
                }
            }
            else {
                cancellableID = new CancellableID();
            }
        }
        
        return cancellableID;
    }

    public List<IDInfo<JavaThreadID>> getAllJavaThreadIDInfoOfCancellableID(CancellableID cid) {
        List<IDInfo<JavaThreadID>> javaThreadIDInfos;

        try (ReleasableLock ignored = this.readLock.acquire()) {
            if (this.cancellableIDToJavaThreadID.containsKey(cid)) {
                javaThreadIDInfos = this.cancellableIDToJavaThreadID.get(cid);
            }
            else {
                javaThreadIDInfos = new ArrayList<IDInfo<JavaThreadID>>();
            }
        }

        return javaThreadIDInfos;
    }

    public List<IDInfo<CancellableID>> getAllCancellableIDInfoOfJavaThreadID(JavaThreadID jid) {
        List<IDInfo<CancellableID>> cancellableIDInfos;

        try (ReleasableLock ignored = this.readLock.acquire()) {
            if (this.javaThreadIDToCancellableID.containsKey(jid)) {
                cancellableIDInfos = this.javaThreadIDToCancellableID.get(jid);
            }
            else {
                cancellableIDInfos = new ArrayList<IDInfo<CancellableID>>();
            }
        }

        return cancellableIDInfos;
    }

    public void setCancellableIDAndJavaThreadID(CancellableID cid, JavaThreadID jid, IDInfo.Status status) {
        try (ReleasableLock ignored = this.writeLock.acquire()) {
            this.doSetCancellableIDAndJavaThreadID(cid, jid, status);
        }
    }

    private void doSetCancellableIDAndJavaThreadID(CancellableID cid, JavaThreadID jid, IDInfo.Status status) {
        if (this.cancellableIDToJavaThreadID.containsKey(cid)) {
            this.cancellableIDToJavaThreadID.get(cid).add(new IDInfo<JavaThreadID>(status, jid));
        }
        else {
            this.cancellableIDToJavaThreadID.put(cid, new ArrayList<IDInfo<JavaThreadID>>(Arrays.asList(new IDInfo<JavaThreadID>(status, jid))));
        }

        if (this.javaThreadIDToCancellableID.containsKey(jid)) {
            this.javaThreadIDToCancellableID.get(jid).add(new IDInfo<CancellableID>(status, cid));
        }
        else {
            this.javaThreadIDToCancellableID.put(jid, new ArrayList<IDInfo<CancellableID>>(Arrays.asList(new IDInfo<CancellableID>(status, cid))));
        }
    }
}
