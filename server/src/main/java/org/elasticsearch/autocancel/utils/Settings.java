package org.elasticsearch.autocancel.utils;

import java.util.Arrays;
import java.util.Map;
import java.util.Set;

public class Settings {
    
    private final static Map<String, Object> settings = Map.of(
        "path_to_logs", "/usr/share/elasticsearch/logs",
        "core_update_cycle_ms", 100L,
        "max_child_cancellable_level", 1000,
        "log_file_max_line", 200000,
        "system_log_level", "WARN",
        "skip_first_ms", 60000L,
        "save_history_ms", 0L,
        "resource_usage_decay", 0.8,
        "monitor_physical_resources", Map.of(
            "CPU", "JVM",
            "MEMORY", "JVM"
        ),
        "monitor_actions", Set.of(
            "indices:data/read/search",
            "indices:data/write/bulk",
            "indices:data/write/index"
        )
    );

    public static Object getSetting(String name) {
        assert Settings.settings.containsKey(name) : "invalid setting name: " + name;
        return Settings.settings.get(name);
    }
}
