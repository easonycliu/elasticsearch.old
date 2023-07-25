package org.elasticsearch.autocancel.app.elasticsearch;

public class TaskWrapper {

    private Object task;

    public TaskWrapper(Object task) throws AssertionError {
        assert task.toString().contains("Task") : "Input is not a class Task.";

        this.task = task;
    }

    @Override
    public String toString() {
        return "Wrapped" + this.task.toString();
    }

    @Override
    public int hashCode() {
        return this.toString().hashCode();
    }

    @Override
    public boolean equals(Object o) {
        return this.hashCode() == o.hashCode();
    }
}
