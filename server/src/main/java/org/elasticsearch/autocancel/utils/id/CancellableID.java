package org.elasticsearch.autocancel.utils.id;

import org.elasticsearch.autocancel.utils.id.ID;

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
        // TODO: Class should be the same
        Long id = this.id;
        Long input_id = ((CancellableID) o).id;
        return id == input_id;
    }

    @Override
    public int hashCode() {
        return this.id.intValue();
    }

    @Override
    public Boolean isValid() {
        return this.id != INVALID_ID;
    }
}
