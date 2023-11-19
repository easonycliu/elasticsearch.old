package org.elasticsearch.autocancel.utils.id;

public class JavaThreadID implements ID {

    private Long id;

    private static final Long INVALID_ID = -1L;

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
        if (o instanceof JavaThreadID) {
            return this.toLong().equals(((JavaThreadID) o).toLong());
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

    public Long unwrap() {
        return this.id;
    }

    public Long toLong() {
        return this.id;
    }
}
