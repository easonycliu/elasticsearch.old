package org.elasticsearch.autocancel.infrastructure.jvm;

import org.elasticsearch.autocancel.infrastructure.AbstractInfrastructure;
import org.elasticsearch.autocancel.utils.Settings;
import org.elasticsearch.autocancel.utils.resource.ResourceName;
import org.elasticsearch.autocancel.infrastructure.ResourceBatch;
import org.elasticsearch.autocancel.infrastructure.ResourceReader;
import org.elasticsearch.autocancel.utils.id.ID;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class JavaThreadStatusReader extends AbstractInfrastructure {

    private List<ResourceName> resourceNames;

    private Map<ResourceName, ResourceReader> resourceReaders;

    public JavaThreadStatusReader() {
        super();

        this.resourceNames = this.getRequiredResourceNames();

        this.resourceReaders = this.initializeResourceReaders();
    }

    public Map<ResourceName, ResourceReader> initializeResourceReaders() {
        Map<ResourceName, ResourceReader> resourceReaders = new HashMap<ResourceName, ResourceReader>();
        resourceReaders.put(ResourceName.CPU, new JavaCPUReader());
        resourceReaders.put(ResourceName.MEMORY, new JavaMemoryReader());

        return resourceReaders;
    }

    private List<ResourceName> getRequiredResourceNames() {
        Map<?, ?> monitorResources = (Map<?, ?>) Settings.getSetting("monitor_physical_resources");
        List<ResourceName> requiredResources = new ArrayList<ResourceName>();
        for (Map.Entry<?, ?> entries : monitorResources.entrySet()) {
            if (((String) entries.getValue()).equals("JVM")) {
                requiredResources.add(ResourceName.valueOf((String) entries.getKey()));
            }
        }
        return requiredResources;
    }

    @Override
    protected void updateResource(ID id, Integer version) {
        ResourceBatch resourceBatch = new ResourceBatch(version);
        for (ResourceName resourceName : this.resourceNames) {
            Map<String, Object> resourceUpdateInfo = this.resourceReaders.get(resourceName).readResource(id, version);
            resourceBatch.setResourceValue(resourceName, resourceUpdateInfo);
        }

        this.setResourceBatch(id, resourceBatch);
    }

}