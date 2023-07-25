/*
 * Manage ID:
 *     Manage a map between javaTID and CID
 *     This is a dynamic map because a CID can be mapped to different javaTID
 */

package org.elasticsearch.autocancel.manager;

import org.elasticsearch.autocancel.utils.CancellableID;
import org.elasticsearch.autocancel.utils.JavaThreadID;
import org.elasticsearch.autocancel.utils.ReleasableLock;

import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class IDManager {

    private Map<CancellableID, List<JavaThreadID>> cancellableIDToJavaThreadID;

    private Map<CancellableID, List<Long>> cancellableIDToChangeTime;

    private Map<JavaThreadID, List<CancellableID>> javaThreadIDToCancellableID;

    private ReadWriteLock idManagerLock;

    private ReleasableLock readLock;

    private ReleasableLock writeLock;

    public IDManager() {
        this.idManagerLock = new ReentrantReadWriteLock();
        this.readLock = new ReleasableLock(idManagerLock.readLock());
        this.writeLock = new ReleasableLock(idManagerLock.writeLock());

        this.cancellableIDToJavaThreadID = new HashMap<CancellableID, List<JavaThreadID>>();
        this.javaThreadIDToCancellableID = new HashMap<JavaThreadID, List<CancellableID>>();
        this.cancellableIDToChangeTime = new HashMap<CancellableID, List<Long>>();

    }

    public JavaThreadID getJavaThreadIDOfCancellableID(CancellableID cid) {
        JavaThreadID javaThreadID;

        try (ReleasableLock ignored = this.readLock.acquire()) {
            if (this.cancellableIDToJavaThreadID.containsKey(cid)) {
                List<JavaThreadID> javaThreadIDList = this.cancellableIDToJavaThreadID.get(cid);
                javaThreadID = javaThreadIDList.get(javaThreadIDList.size() - 1);
            } else {
                javaThreadID = new JavaThreadID();
            }
        }

        return javaThreadID;
    }

    public List<JavaThreadID> getAllJavaThreadIDOfCancellableID(CancellableID cid) {
        List<JavaThreadID> javaThreadIDs;

        try (ReleasableLock ignored = this.readLock.acquire()) {
            if (this.cancellableIDToJavaThreadID.containsKey(cid)) {
                javaThreadIDs = this.cancellableIDToJavaThreadID.get(cid);
            } else {
                javaThreadIDs = new ArrayList<JavaThreadID>(Arrays.asList(new JavaThreadID()));
            }
        }

        return javaThreadIDs;
    }

    public List<Long> getAllChangeTimeOfCancellableID(CancellableID cid) {
        List<Long> changeTimes;

        try (ReleasableLock ignored = this.readLock.acquire()) {
            if (this.cancellableIDToJavaThreadID.containsKey(cid)) {
                changeTimes = this.cancellableIDToChangeTime.get(cid);
            } else {
                changeTimes = new ArrayList<Long>(Arrays.asList(System.currentTimeMillis()));
            }
        }

        return changeTimes;
    }

    public CancellableID getCancellableIDOfJavaThreadID(JavaThreadID jid) {
        CancellableID cancellableID;

        try (ReleasableLock ignored = this.readLock.acquire()) {
            if (this.javaThreadIDToCancellableID.containsKey(jid)) {
                List<CancellableID> cancellableIDList = this.javaThreadIDToCancellableID.get(jid);
                cancellableID = cancellableIDList.get(cancellableIDList.size() - 1);
            } else {
                cancellableID = new CancellableID();
            }
        }

        return cancellableID;
    }

    public void setCancellableIDAndJavaThreadID(CancellableID cid, JavaThreadID jid) {
        try (ReleasableLock ignored = this.writeLock.acquire()) {
            this.doSetCancellableIDAndJavaThreadID(cid, jid);
        }
    }

    private void doSetCancellableIDAndJavaThreadID(CancellableID cid, JavaThreadID jid) {
        if (this.cancellableIDToJavaThreadID.containsKey(cid)) {
            this.cancellableIDToJavaThreadID.get(cid).add(jid);
            this.cancellableIDToChangeTime.get(cid).add(System.currentTimeMillis());
        } else {
            this.cancellableIDToJavaThreadID.put(cid, new ArrayList<JavaThreadID>(Arrays.asList(jid)));
            this.cancellableIDToChangeTime.put(cid, new ArrayList<Long>(Arrays.asList(System.currentTimeMillis())));
        }

        if (this.javaThreadIDToCancellableID.containsKey(jid)) {
            this.javaThreadIDToCancellableID.get(jid).add(cid);
        } else {
            this.javaThreadIDToCancellableID.put(jid, new ArrayList<CancellableID>(Arrays.asList(cid)));
        }

    }
}
