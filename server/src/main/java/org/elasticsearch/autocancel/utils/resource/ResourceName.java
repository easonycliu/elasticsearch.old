package org.elasticsearch.autocancel.utils.resource;

import java.util.Map;

public class ResourceName {
	public static final ResourceName CPU = new ResourceName("CPU");
	public static final ResourceName MEMORY = new ResourceName("MEMORY");
	public static final ResourceName NULL = new ResourceName("NULL");

	private static final Map<String, ResourceName> builtinNames =
			Map.of("CPU", ResourceName.CPU, "MEMORY", ResourceName.MEMORY, "NULL", ResourceName.NULL);

	private final String resourceName;

	public ResourceName(String name) {
		this.resourceName = name;
	}

	public static ResourceName valueOf(String name) {
		ResourceName resourceName = null;
		if (ResourceName.builtinNames.containsKey(name)) {
			resourceName = ResourceName.builtinNames.get(name);
		} else {
			resourceName = new ResourceName(name);
		}
		return resourceName;
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
		return ResourceName.class.equals(o.getClass()) && o.toString().equals(this.toString());
	}
}
