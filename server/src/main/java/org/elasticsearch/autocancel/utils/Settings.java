package org.elasticsearch.autocancel.utils;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.HashSet;
import java.util.Set;

public class Settings {
    
    private final static Map<String, Object> settings = Map.of(
        "path_to_logs", "/usr/share/elasticsearch/logs",
        "core_update_cycle_ms", 100L,
        "max_child_cancellable_level", 1000,
        "log_file_max_line", 200000,
        "system_log_level", "INFO",
        "monitor_physical_resources", Map.of(
            "CPU", "JVM",
            "MEMORY", "Linux"
        ),
        "monitor_locks", Arrays.asList(
            Map.of("file_name", "xxx.java", "line_number", "xxx"), // example
            Map.of("class_name", "IndicesRequestCache..."), // use ... to include all classes inside specified task
            Map.of("method_name", "xxx")  // example
        )
    );

    public static Object getSetting(String name) {
        assert Settings.settings.containsKey(name) : "invalid setting name: " + name;
        return Settings.settings.get(name);
    }
}
