package org.elasticsearch.autocancel.utils.Resource;

import java.util.Map;

public class ResourceType {

    public static final ResourceType CPU = new ResourceType("CPU");
    public static final ResourceType MEMORY = new ResourceType("MEMORY");
    public static final ResourceType NULL = new ResourceType("NULL");

    private static final Map<String, ResourceType> builtinTypes = Map.of(
        "CPU", ResourceType.CPU,
        "MEMORY", ResourceType.MEMORY,
        "NULL", ResourceType.NULL
    );

    private final String resourceName;

    public ResourceType(String name) {
        this.resourceName = name;
    }

    public static ResourceType valueOf(String name) {
        ResourceType type = null;
        if (ResourceType.builtinTypes.containsKey(name)) {
            type = ResourceType.builtinTypes.get(name);
        }
        else {
            type = new ResourceType(name);
        }
        return type;
    }

    @Override
    public String toString() {
        return this.resourceName;
    }

    @Override
    public int hashCode() {
        return this.resourceName.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        return ResourceType.class.equals(o.getClass()) && o.toString().equals(this.toString());
    }
    
}
