/*
 * Manage Infrastructure:
 *     Get resource data from specific infrastructure according to settings
 */

package org.elasticsearch.autocancel.manager;

import org.elasticsearch.autocancel.infrastructure.AbstractInfrastructure;
import org.elasticsearch.autocancel.infrastructure.jvm.JavaThreadStatusReader;
import org.elasticsearch.autocancel.infrastructure.linux.LinuxThreadStatusReader;
import org.elasticsearch.autocancel.utils.Settings;
import org.elasticsearch.autocancel.utils.id.JavaThreadID;
import org.elasticsearch.autocancel.utils.resource.ResourceName;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class InfrastructureManager {

    private AtomicInteger version;

    private final Map<String, AbstractInfrastructure> infrastructures;

    public InfrastructureManager() {
        this.version = new AtomicInteger();
        this.infrastructures = Map.of(
                "JVM", new JavaThreadStatusReader(),
                "Linux", new LinuxThreadStatusReader());
    }

    public Map<String, Object> getSpecifiedResourceLatest(JavaThreadID jid, ResourceName resourceName) {
        AbstractInfrastructure infrastructure = this.getInfrastructure(resourceName);
        assert infrastructure != null : String.format("Unsupported resource name: %s", resourceName.toString());
        Map<String, Object> resourceUpdateInfo = infrastructure.getResource(jid, resourceName, this.version.get());
        return resourceUpdateInfo;
    }

    public void startNewVersion() {
        this.version.incrementAndGet();
    }

    private AbstractInfrastructure getInfrastructure(ResourceName resourceName) {
        AbstractInfrastructure infrastructure = this.infrastructures
                .get((String) ((Map<?, ?>) Settings.getSetting("monitor_physical_resources")).get(resourceName.toString()));

        if (infrastructure == null) {
            System.out.println("Invalid infrastructure name " + resourceName.toString());
            // TODO: do something more
        }

        return infrastructure;
    }
}