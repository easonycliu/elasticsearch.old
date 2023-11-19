package org.elasticsearch.autocancel.utils.id;

public class CancellableID implements ID {
    
    private Long id;

    private static final Long INVALID_ID = -1L;

    public CancellableID(Long id) {
        this.id = id;
    }

    // Invalid CancellableID
    public CancellableID() {
        this.id = INVALID_ID;
    }

    @Override
    public String toString() {
        return String.format("Cancellable ID : %d", this.id);
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof CancellableID) {
            return this.toLong().equals(((CancellableID) o).toLong());
        }
        return false;
    }

    @Override
    public int hashCode() {
        return this.id.intValue();
    }

    @Override
    public Boolean isValid() {
        return this.id != INVALID_ID;
    }

    public Long toLong() {
        return this.id;
    }
}
