package org.elasticsearch.autocancel.infrastructure.linux;

import org.elasticsearch.autocancel.utils.id.ID;

class LinuxThreadID implements ID {

    private Long id;

    private static final Long INVALID_ID = -1L;
    
    public LinuxThreadID(Long id) {
        this.id = id;
    }

    // Invalid LinuxThreadID
    public LinuxThreadID() {
        this.id = -1L;
    }

    @Override
    public String toString() {
        return String.format("Linux Thread ID : %d", this.id);
    }

    @Override
    public boolean equals(Object o) {
        // TODO: Class should be the same
        return this.id == ((LinuxThreadID) o).id;
    }

    @Override
    public int hashCode() {
        return this.id.intValue();
    }

    @Override
    public Boolean isValid() {
        return this.id != INVALID_ID;
    }

    public Long unwrap() {
        return this.id;
    }
}
