package org.elasticsearch.autocancel.utils;

public class CancellableID {

    private Long id;

    public CancellableID(Long id) {
        this.id = id;
    }

    // Invalid CancellableID
    public CancellableID() {
        this.id = -1L;
    }

    @Override
    public String toString() {
        return String.format("ID : %d", this.id);
    }

    @Override
    public boolean equals(Object o) {
        return this.id == ((CancellableID) o).id;
    }

    @Override
    public int hashCode() {
        return this.id.intValue();
    }
}
