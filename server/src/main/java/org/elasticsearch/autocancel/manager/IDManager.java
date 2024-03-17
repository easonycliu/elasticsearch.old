/*
 * Manage ID:
 *     Manage a map between javaTID and CID
 *     This is a dynamic map because a CID can be mapped to different javaTID
 */

package org.elasticsearch.autocancel.manager;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.elasticsearch.autocancel.utils.id.CancellableID;
import org.elasticsearch.autocancel.utils.id.JavaThreadID;

public class IDManager {
	private ConcurrentMap<JavaThreadID, CancellableID> javaThreadIDToCancellableID;

	public IDManager() {
		this.javaThreadIDToCancellableID = new ConcurrentHashMap<JavaThreadID, CancellableID>();
	}

	public List<JavaThreadID> getJavaThreadIDOfCancellableID(CancellableID cid) {
		List<JavaThreadID> javaThreadIDs = new ArrayList<JavaThreadID>();

		this.javaThreadIDToCancellableID.forEach((key, value) -> {
			if (value.equals(cid)) {
				javaThreadIDs.add(key);
			}
		});

		return javaThreadIDs;
	}

	public CancellableID getCancellableIDOfJavaThreadID(JavaThreadID jid) {
		CancellableID cancellableID = this.javaThreadIDToCancellableID.getOrDefault(jid, new CancellableID());

		return cancellableID;
	}

	public void setCancellableIDAndJavaThreadID(CancellableID cid, JavaThreadID jid) {
		this.javaThreadIDToCancellableID.put(jid, cid);
	}

	public CancellableID removeJavaThreadID(JavaThreadID jid) {
		return this.javaThreadIDToCancellableID.remove(jid);
	}
}
