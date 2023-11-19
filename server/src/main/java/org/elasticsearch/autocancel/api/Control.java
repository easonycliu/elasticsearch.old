package org.elasticsearch.autocancel.api;

import java.util.function.Consumer;

public class Control {

    private final Consumer<Object> canceller;

    public Control(Consumer<Object> canceller) {
        this.canceller = canceller;
    }

    public void cancel(Object task) {
        if (task != null) {
            this.canceller.accept(task);
        }
    }

    public void retry(TaskInfo taskInfo) {
        if (taskInfo.hasRequestInfo()) {

        }
        else {
            System.out.println(String.format("Cannot retry unsupported task %s", taskInfo.getName()));
        }
    }
}
