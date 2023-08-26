package org.elasticsearch.autocancel.infrastructure.linux;

import org.elasticsearch.autocancel.infrastructure.AbstractInfrastructure;
import org.elasticsearch.autocancel.infrastructure.ResourceBatch;
import org.elasticsearch.autocancel.infrastructure.ResourceReader;
import org.elasticsearch.autocancel.utils.Resource.ResourceName;
import org.elasticsearch.autocancel.utils.id.CancellableID;
import org.elasticsearch.autocancel.utils.id.ID;
import org.elasticsearch.autocancel.utils.id.JavaThreadID;
import org.elasticsearch.autocancel.utils.logger.Logger;
import org.elasticsearch.autocancel.infrastructure.linux.LinuxThreadID;
import org.elasticsearch.autocancel.utils.Settings;

import java.util.Map;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class LinuxThreadStatusReader extends AbstractInfrastructure {

    private Map<JavaThreadID, LinuxThreadID> javaThreadIDToLinuxThreadID;

    private List<ResourceName> resourceNames;

    private Map<ResourceName, ResourceReader> resourceReaders;

    public LinuxThreadStatusReader() {
        super();

        this.javaThreadIDToLinuxThreadID = new HashMap<JavaThreadID, LinuxThreadID>();

        this.resourceNames = this.getRequiredResourceNames();

        this.resourceReaders = this.initializeResourceReaders();

        // VM.
    }

    public Map<ResourceName, ResourceReader> initializeResourceReaders() {
        Map<ResourceName, ResourceReader> resourceReaders = new HashMap<ResourceName, ResourceReader>();
        resourceReaders.put(ResourceName.CPU, new LinuxCPUReader());
        resourceReaders.put(ResourceName.MEMORY, new LinuxMemoryReader());

        return resourceReaders;
    }

    public List<ResourceName> getRequiredResourceNames() {
        Map<?, ?> monitorResources = (Map<?, ?>) Settings.getSetting("monitor_physical_resources");
        List<ResourceName> requiredResources = new ArrayList<ResourceName>();
        for (Map.Entry<?, ?> entries : monitorResources.entrySet()) {
            if (((String) entries.getValue()).equals("Linux")) {
                requiredResources.add(ResourceName.valueOf((String) entries.getKey()));
            }
        }
        return requiredResources;
    }

    @Override
    protected void updateResource(ID id, Integer version) {
        LinuxThreadID linuxThreadID = null;
        if (this.javaThreadIDToLinuxThreadID.containsKey((JavaThreadID) id)) {
            linuxThreadID = this.javaThreadIDToLinuxThreadID.get((JavaThreadID) id);
        } else {
            linuxThreadID = this.getLinuxThreadIDFromJavaThreadID((JavaThreadID) id);
            // TODO: add isValid()
            if (linuxThreadID.isValid()) {
                Logger.systemTrace(id.toString() + " is running on " + linuxThreadID.toString());
                this.javaThreadIDToLinuxThreadID.put((JavaThreadID) id, linuxThreadID);
            } else {
                Logger.systemTrace("Failed to find linux thread id of " + id.toString());
            }
        }

        if (linuxThreadID.isValid()) {
            ResourceBatch resourceBatch = new ResourceBatch(version);
            for (ResourceName resourceName : this.resourceNames) {
                Double value = this.resourceReaders.get(resourceName).readResource(linuxThreadID, version);
                resourceBatch.setResourceValue(resourceName, value);
            }
            this.setResourceBatch(id, resourceBatch);
        } else {
            Logger.systemTrace("Skip updating version " + version.toString());
        }
    }

    private LinuxThreadID getLinuxThreadIDFromJavaThreadID(JavaThreadID jid) {
        ThreadMXBean tmx = ManagementFactory.getThreadMXBean();
        ThreadInfo info = tmx.getThreadInfo(jid.unwrap());
        String threadName = info.getThreadName();
        String regex = "(.*)(NativeTID:\\[)(.*)(\\])(.*)";
        Pattern pattern = java.util.regex.Pattern.compile(regex);
        Matcher matcher = pattern.matcher(threadName);
        String linuxThreadIDStr = null;
        while (matcher.find()) {
            if (linuxThreadIDStr == null) {
                linuxThreadIDStr = matcher.group(3);
            } else {
                assert false : "A thread name should contain two native thread id info";
            }
        }
        LinuxThreadID linuxThreadID = null;
        if (linuxThreadIDStr != null) {
            linuxThreadID = new LinuxThreadID(Long.valueOf(linuxThreadIDStr));
        } else {
            linuxThreadID = new LinuxThreadID();
        }
        return linuxThreadID;
    }

}