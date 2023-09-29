package org.elasticsearch.autocancel.app.elasticsearch;

import org.elasticsearch.autocancel.utils.id.CancellableID;

import java.util.function.BiConsumer;

public class Control {

    private final BiConsumer<Long, String> canceller;

    public Control(BiConsumer<Long, String> canceller) {
        this.canceller = canceller;
    }

    public void cancel(CancellableID cid) {
        if (cid.isValid()) {
            this.canceller.accept(cid.toLong(), "Auto Cancel Library");
        }
    }
}
