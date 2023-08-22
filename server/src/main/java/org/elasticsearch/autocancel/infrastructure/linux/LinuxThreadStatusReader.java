package org.elasticsearch.autocancel.infrastructure.linux;

import org.elasticsearch.autocancel.infrastructure.AbstractInfrastructure;
import org.elasticsearch.autocancel.infrastructure.ResourceBatch;
import org.elasticsearch.autocancel.infrastructure.ResourceReader;
import org.elasticsearch.autocancel.utils.Resource.ResourceType;
import org.elasticsearch.autocancel.utils.id.CancellableID;
import org.elasticsearch.autocancel.utils.id.ID;
import org.elasticsearch.autocancel.utils.id.JavaThreadID;
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

    private List<ResourceType> resourceTypes;

    private Map<ResourceType, ResourceReader> resourceReaders;

    public LinuxThreadStatusReader() {
        super();

        this.javaThreadIDToLinuxThreadID = new HashMap<JavaThreadID, LinuxThreadID>();
        
        this.resourceTypes = this.getRequiredResourceTypes();

        this.resourceReaders = this.initializeResourceReaders();

        // VM.
    }

    public Map<ResourceType, ResourceReader> initializeResourceReaders() {
        Map<ResourceType, ResourceReader> resourceReaders = new HashMap<ResourceType, ResourceReader>();
        resourceReaders.put(ResourceType.CPU, new LinuxCPUReader());
        resourceReaders.put(ResourceType.MEMORY, new LinuxMemoryReader());

        return resourceReaders;
    }

    public List<ResourceType> getRequiredResourceTypes() {
        Map<?, ?> monitorResources = (Map<?, ?>) Settings.getSetting("monitor_resources");
        List<ResourceType> requiredResources = new ArrayList<ResourceType>();
        for (Map.Entry<?, ?> entries : monitorResources.entrySet()) {
            if (((String) entries.getValue()).equals("Linux")) {
                requiredResources.add(ResourceType.valueOf((String) entries.getKey()));
            }
        }
        return requiredResources;
    }

    @Override
    protected void updateResource(ID id, Integer version) {
        LinuxThreadID linuxThreadID = null;
        if (this.javaThreadIDToLinuxThreadID.containsKey((JavaThreadID) id)) {
            linuxThreadID = this.javaThreadIDToLinuxThreadID.get((JavaThreadID) id);
        }
        else {
            linuxThreadID = this.getLinuxThreadIDFromJavaThreadID((JavaThreadID) id);
            assert !linuxThreadID.equals(new LinuxThreadID()) : "Failed to find linux thread id of java thread id";
            this.javaThreadIDToLinuxThreadID.put((JavaThreadID) id, linuxThreadID);
        }
        
        ResourceBatch resourceBatch = new ResourceBatch(version);
        for (ResourceType type : this.resourceTypes) {
            Double value = this.resourceReaders.get(type).readResource(linuxThreadID, version);
            resourceBatch.setResourceValue(type, value);
        }

        this.setResourceBatch(id, resourceBatch);
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
            }
            else {
                assert false : "A thread name should contain two native thread id info";
            }
        }
        LinuxThreadID linuxThreadID = null;
        if (linuxThreadIDStr != null) {
            linuxThreadID = new LinuxThreadID(Long.valueOf(linuxThreadIDStr));
        }
        else {
            linuxThreadID = new LinuxThreadID();
        }
        return linuxThreadID;
    }

}