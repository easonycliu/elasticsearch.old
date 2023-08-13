package org.elasticsearch.autocancel.app.elasticsearch;

import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

import org.elasticsearch.autocancel.manager.MainManager;
import org.elasticsearch.autocancel.utils.id.CancellableID;
import org.elasticsearch.autocancel.utils.id.IDInfo;
import org.elasticsearch.autocancel.utils.id.JavaThreadID;
import org.elasticsearch.autocancel.utils.logger.Logger;

public class Log {
    
    MainManager mainManager;

    Logger logger;

    public Log(MainManager mainManager) {
        this.mainManager = mainManager;
        this.logger = new Logger("/tmp/logs", "cidinfo", 10000);
    }

    public void stop() {
        this.logger.close();
    }

    public void logCancellableJavaThreadIDInfo(CancellableID cid, Object task) {
        List<IDInfo<JavaThreadID>> javaThreadIDInfos = this.mainManager.getAllJavaThreadIDInfoOfCancellableID(cid);

        // TODO: add config to jidinfo save path
        this.logger.log(String.format("========== Cancellable %s %s ==========\n", cid.toString(), task.toString()));
        for (IDInfo<JavaThreadID> javaThreadIDInfo : javaThreadIDInfos) {
            this.logger.log(javaThreadIDInfo.toString() + "\n");
        }
    }

}
