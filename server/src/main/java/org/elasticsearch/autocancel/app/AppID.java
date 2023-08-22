package org.elasticsearch.autocancel.app;

public interface AppID {
    @Override
    public String toString();

    @Override
    public int hashCode();

    @Override
    public boolean equals(Object o);
    
    public Boolean isValid();
}
