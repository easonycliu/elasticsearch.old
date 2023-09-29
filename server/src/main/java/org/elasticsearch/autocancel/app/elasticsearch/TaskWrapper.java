package org.elasticsearch.autocancel.app.elasticsearch;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.elasticsearch.autocancel.utils.id.CancellableID;

public class TaskWrapper {

    private static final Pattern parentPattern = Pattern.compile("(.*)(parentTask=)([^\\s]+)(,)(.*)");

    private static final Pattern taskPattern = Pattern.compile("(.*)(Task\\{id=)([0-9]+)(,)(.*)");

    private static final Pattern actionPattern = Pattern.compile("(.*)(action=')([^\\s]+)(',)(.*)");

    private static final Pattern startTimeNanoPattern = Pattern.compile("(.*)(startTimeNanos=)([^\\s]+)(\\})(.*)");

    private static final Pattern startTimePattern = Pattern.compile("(.*)(startTime=)([^\\s]+)(,)(.*)");
    
    private Object task;

    private CancellableID taskID;

    private CancellableID parentID;

    private String action;

    private Long startTimeNano;

    private Long startTime;

    public TaskWrapper(Object task) throws AssertionError {
        assert task.toString().contains("Task") : "Input is not a class Task.";

        this.task = task;

        Matcher parentMatcher = parentPattern.matcher(this.task.toString());

        if (parentMatcher.find()) {
            String id = parentMatcher.group(3);
            if (id.equals("unset")) {
                this.parentID = new CancellableID();
            }
            else {
                String[] items = id.split(":");
                assert items.length == 2 && items[1].matches("^[0-9]+$") : "Illegal task name format " + this.task.toString();
                this.parentID = new CancellableID(Long.valueOf(items[1]));
            }
        }
        else {
            assert false : "Illegal task name format " + this.task.toString();
        }

        Matcher taskMatcher = taskPattern.matcher(this.task.toString());

        if (taskMatcher.find()) {
            this.taskID = new CancellableID(Long.valueOf(taskMatcher.group(3)));
        }
        else {
            assert false : "Illegal task name format " + this.task.toString();
        }

        Matcher actionMatcher = actionPattern.matcher(this.task.toString());

        if (actionMatcher.find()) {
            this.action = actionMatcher.group(3);
        }
        else {
            assert false : "Illegal task name format " + this.task.toString();
        }

        Matcher startTimeNanoMatcher = startTimeNanoPattern.matcher(this.task.toString());

        if (startTimeNanoMatcher.find()) {
            this.startTimeNano = Long.valueOf(startTimeNanoMatcher.group(3));
        }
        else {
            assert false : "Illegal task name format " + this.task.toString();
        }

        Matcher startTimeMatcher = startTimePattern.matcher(this.task.toString());

        if (startTimeMatcher.find()) {
            this.startTime = Long.valueOf(startTimeMatcher.group(3));
        }
        else {
            assert false : "Illegal task name format " + this.task.toString();
        }

        if (this.taskID.equals(this.parentID)) {
            this.parentID = new CancellableID();
        }

    }

    public CancellableID getParentTaskID() {
        return this.parentID;
    }

    public CancellableID getTaskID() {
        assert this.taskID.isValid() : "Task id should never be invalid";
        return this.taskID;
    }

    public String getAction() {
        return this.action;
    }

    public Long getStartTimeNano() {
        return this.startTimeNano;
    }

    public Long getStartTime() {
        return this.startTime;
    }

    @Override
    public String toString() {
        return "Wrapped" + this.task.toString();
    }

    @Override
    public int hashCode() {
        return this.taskID.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        return this.hashCode() == o.hashCode();
    }
}
