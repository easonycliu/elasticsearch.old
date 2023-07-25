package org.elasticsearch.autocancel.utils;

public class JavaThreadID {

    private Long id;

    public JavaThreadID(Long id) {
        this.id = id;
    }

    // Invalid JavaThreadID
    public JavaThreadID() {
        this.id = -1L;
    }

    @Override
    public String toString() {
        return String.format("Java Thread ID : %d", this.id);
    }

    @Override
    public boolean equals(Object o) {
        return this.id == ((JavaThreadID) o).id;
    }

    @Override
    public int hashCode() {
        return this.id.intValue();
    }

    public Long unwrap() {
        return this.id;
    }
}
