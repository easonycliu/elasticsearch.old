package org.elasticsearch.autocancel.app.elasticsearch;

import org.elasticsearch.autocancel.manager.MainManager;
import org.elasticsearch.autocancel.utils.id.CancellableID;

import java.util.function.BiConsumer;
import java.util.function.Function;

public class Control {

    private final MainManager mainManager;

    private final Function<CancellableID, TaskWrapper.TaskID> taskIDGetter;

    private final BiConsumer<Long, String> canceller;

    public Control(MainManager mainManager, Function<CancellableID, TaskWrapper.TaskID> taskIDGetter, BiConsumer<Long, String> canceller) {
        this.mainManager = mainManager;
        this.taskIDGetter = taskIDGetter;
        this.canceller = canceller;
    }

    public void cancel(CancellableID cid) {
        TaskWrapper.TaskID taskID = this.taskIDGetter.apply(cid);
        if (taskID != null) {
            this.canceller.accept(taskID.unwrap(), "Auto Cancel Library");
        }
    }
}
