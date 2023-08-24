/*
 * Manage Infrastructure:
 *     Get resource data from specific infrastructure according to settings
 */

package org.elasticsearch.autocancel.manager;

import org.elasticsearch.autocancel.infrastructure.AbstractInfrastructure;
import org.elasticsearch.autocancel.infrastructure.jvm.JavaThreadStatusReader;
import org.elasticsearch.autocancel.infrastructure.linux.LinuxThreadStatusReader;
import org.elasticsearch.autocancel.utils.Settings;
import org.elasticsearch.autocancel.utils.Resource.ResourceType;
import org.elasticsearch.autocancel.utils.id.CancellableID;
import org.elasticsearch.autocancel.utils.id.JavaThreadID;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class InfrastructureManager {

    private AtomicInteger version;

    private final Map<String, AbstractInfrastructure> infrastructures;
    
    public InfrastructureManager() {
        this.version = new AtomicInteger();
        this.infrastructures = Map.of(
            "JVM", new JavaThreadStatusReader(),
            "Linux", new LinuxThreadStatusReader()
        );
    }

    public Double getSpecifiedTypeResourceLatest(JavaThreadID jid, ResourceType type) {
        AbstractInfrastructure infrastructure = this.getInfrastructure(type);
        assert infrastructure != null : String.format("Unsupported resource type: %s", type.toString());
        Double resource = infrastructure.getResource(jid, type, this.version.get());
        return resource;
    }

    public void startNewVersion() {
        this.version.incrementAndGet();
    }

    private AbstractInfrastructure getInfrastructure(ResourceType type) {
        AbstractInfrastructure infrastructure = this.infrastructures.get((String)((Map<?, ?>)Settings.getSetting("monitor_resources")).get(type.toString()));
        
        if (infrastructure == null) {
            System.out.println("Invalid infrastructure type " + type.toString());
            // TODO: do something more
        }

        return infrastructure;
    }
}