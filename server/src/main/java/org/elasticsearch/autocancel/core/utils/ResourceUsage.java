package org.elasticsearch.autocancel.core.utils;

public class ResourceUsage {
    
    private Double usage;

    public ResourceUsage() {
        this.usage = 0.0;
    }

    public ResourceUsage(Double usage) {
        this.usage = usage;
    }

    public void setUsage(Double usage) throws AssertionError {
        this.usage = usage;
    }

    public Double getUsage() {
        return this.usage;
    }
}
