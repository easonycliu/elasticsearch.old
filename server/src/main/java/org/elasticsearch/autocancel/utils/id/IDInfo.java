package org.elasticsearch.autocancel.utils.id;

public class IDInfo<ObjectID extends ID> {
    
    private Long timestamp;

    private Status status;

    private ObjectID id;

    public IDInfo(Long timestamp, Status status, ObjectID id) {
        this.timestamp = timestamp;
        this.id = id;
        
        this.status = status;
    }

    public IDInfo(Status status, ObjectID id) {
        this.timestamp = System.nanoTime();
        this.id = id;

        this.status = status;
    }

    @Override
    public String toString() {
        return String.format("Time: %s. Status: %s. ID: %s", this.timestamp.toString(),
        this.status.toString(), this.id.toString());
    }

    @Override
    public boolean equals(Object o) {
        return this.id.getClass() == ((IDInfo<?>)o).getID().getClass() && 
        this.id.equals(((IDInfo<?>)o).getID());
    }

    @Override
    public int hashCode() {
        return this.id.hashCode();
    }

    public Long getStartTime() {
        return this.timestamp;
    }

    public ObjectID getID() {
        return this.id;
    }

    public Boolean isExit() {
        return this.status.equals(Status.EXIT);
    }

    public Boolean isRun() {
        return this.status.equals(Status.RUN);
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public Status getStatus() {
        return this.status;
    }

    public void exit() {
        this.status = Status.EXIT;
    }

    public enum Status {
        RUN, QUEUE, EXIT;
    }
}
