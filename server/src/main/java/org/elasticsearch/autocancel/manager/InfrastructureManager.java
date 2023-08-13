/*
 * Manage Infrastructure:
 *     Get resource data from specific infrastructure according to settings
 */

package org.elasticsearch.autocancel.manager;

import org.elasticsearch.autocancel.infrastructure.AbstractInfrastructure;
import org.elasticsearch.autocancel.infrastructure.jvm.JavaThreadStatusReader;
import org.elasticsearch.autocancel.infrastructure.linux.LinuxThreadStatusReader;
import org.elasticsearch.autocancel.utils.Resource.ResourceType;
import org.elasticsearch.autocancel.utils.id.CancellableID;
import org.elasticsearch.autocancel.utils.id.JavaThreadID;

import java.util.concurrent.atomic.AtomicInteger;

public class InfrastructureManager {

    private AtomicInteger version;

    private JavaThreadStatusReader javaThreadStatusReader;

    private LinuxThreadStatusReader linuxThreadStatusReader;
    
    public InfrastructureManager() {
        this.version = new AtomicInteger();
        this.javaThreadStatusReader = new JavaThreadStatusReader();
        // BUGGY
        this.linuxThreadStatusReader = new LinuxThreadStatusReader();
    }

    public Double getSpecifiedTypeResourceLatest(JavaThreadID jid, ResourceType type) {
        // TODO: get resource from infrastructure
        AbstractInfrastructure infrastructure = this.getInfrastructure(type);
        assert infrastructure != null : String.format("Unsupported resource type: %s", type.toString());
        Double resource = infrastructure.getResource(jid, type, this.version.get());
        return resource;
    }

    public void startNewVersion() {
        this.version.incrementAndGet();
    }

    private AbstractInfrastructure getInfrastructure(ResourceType type) {
        // TODO: use infrastructure according to settings
        AbstractInfrastructure infrastructure;
        switch (type) {
            case CPU:
                infrastructure = this.javaThreadStatusReader;
                break;
            case MEMORY:
                infrastructure = this.javaThreadStatusReader;
                break;
            case NULL:
                infrastructure = null;
                break;
            default:
                infrastructure = null;
                break;
        }

        return infrastructure;
    }
}