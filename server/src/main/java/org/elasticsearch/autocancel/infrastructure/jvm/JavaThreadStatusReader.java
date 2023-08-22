package org.elasticsearch.autocancel.infrastructure.jvm;

import org.elasticsearch.autocancel.infrastructure.AbstractInfrastructure;
import org.elasticsearch.autocancel.utils.Settings;
import org.elasticsearch.autocancel.utils.Resource.ResourceType;
import org.elasticsearch.autocancel.utils.id.JavaThreadID;
import org.elasticsearch.autocancel.infrastructure.ResourceBatch;
import org.elasticsearch.autocancel.infrastructure.ResourceReader;
import org.elasticsearch.autocancel.utils.id.ID;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class JavaThreadStatusReader extends AbstractInfrastructure {

    private List<ResourceType> resourceTypes;

    private Map<ResourceType, ResourceReader> resourceReaders;

    public JavaThreadStatusReader() {
        super();
        
        this.resourceTypes = this.getRequiredResourceTypes();

        this.resourceReaders = this.initializeResourceReaders();
    }

    public Map<ResourceType, ResourceReader> initializeResourceReaders() {
        Map<ResourceType, ResourceReader> resourceReaders = new HashMap<ResourceType, ResourceReader>();
        resourceReaders.put(ResourceType.CPU, new JavaCPUReader());
        resourceReaders.put(ResourceType.MEMORY, new JavaMemoryReader());

        return resourceReaders;
    }

    private List<ResourceType> getRequiredResourceTypes() {
        Map<?, ?> monitorResources = (Map<?, ?>) Settings.getSetting("monitor_resources");
        List<ResourceType> requiredResources = new ArrayList<ResourceType>();
        for (Map.Entry<?, ?> entries : monitorResources.entrySet()) {
            if (((String) entries.getValue()).equals("JVM")) {
                requiredResources.add(ResourceType.valueOf((String) entries.getKey()));
            }
        }
        return requiredResources;
    }

    @Override
    protected void updateResource(ID id, Integer version) {        
        ResourceBatch resourceBatch = new ResourceBatch(version);
        for (ResourceType type : this.resourceTypes) {
            Double value = this.resourceReaders.get(type).readResource(id, version);
            resourceBatch.setResourceValue(type, value);
        }

        this.setResourceBatch(id, resourceBatch);
    }

}