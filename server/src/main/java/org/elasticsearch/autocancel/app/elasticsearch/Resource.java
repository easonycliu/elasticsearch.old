package org.elasticsearch.autocancel.app.elasticsearch;

import org.elasticsearch.autocancel.manager.MainManager;

import java.util.Map;
import java.util.HashMap;

public class Resource {

    private final MainManager mainManager;

    private Map<String, Long> resourceWaitStartTime;

    public Resource(MainManager mainManager) {
        this.mainManager = mainManager;
        this.resourceWaitStartTime = new HashMap<String, Long>();
    }
    
    public void addResourceUsage(String name, Double value) {
        this.mainManager.updateAppResource(name, value);
    }

    public void startResourceWait(String name) {
        if (this.resourceWaitStartTime.containsKey(name)) {
            Long startTime = this.resourceWaitStartTime.get(name);
            if (startTime.equals(-1L)) {
                this.resourceWaitStartTime.put(name, System.nanoTime());
            }
            else {
                System.out.println("Resource waiting hasn't finished last time.");
                // TODO: do something more
            }
        }
        else {
            this.resourceWaitStartTime.put(name, System.nanoTime());
        }
    }

    public void endResourceWait(String name) {
        if (this.resourceWaitStartTime.containsKey(name)) {
            Long startTime = this.resourceWaitStartTime.get(name);
            if (startTime.equals(-1L)) {
                this.resourceWaitStartTime.put(name, System.nanoTime());
                System.out.println("Resource waiting has finished, don't finish it again.");
                // TODO: do something more
            }
            else {
                Long waitingTime = System.nanoTime() - startTime;
                this.addResourceUsage(name, Double.valueOf(waitingTime));
            }
        }
        else {
            System.out.println("Resource waiting didn't start yet.");
            // TODO: do something more
        }
    }
}
