package org.elasticsearch.autocancel.utils;

import java.util.Map;
import java.util.Set;

public class Settings {
    
    private final static Map<String, Object> settings = Map.ofEntries(
        Map.entry("path_to_logs", "/var/solr/logs"),
        Map.entry("core_update_cycle_ms", 100L),
        Map.entry("max_child_cancellable_level", 1000),
        Map.entry("log_file_max_line", 200000),
        Map.entry("system_log_level", "WARN"),
        Map.entry("skip_first_ms", 30000L),
        Map.entry("save_history_ms", 0L),
        Map.entry("resource_usage_decay", 0.8),
        Map.entry("default_policy", Settings.getFromJVMOrDefault("default.policy", "base_policy")),
        Map.entry("predict_progress", Settings.getFromJVMOrDefault("predict.progress", "false")),
        Map.entry(
            "monitor_physical_resources", 
            Map.of(
                "CPU", "JVM",
                "MEMORY", "JVM"
            )
        ),
        Map.entry(
            "monitor_actions", 
            Set.of(
                "indices:data/read/search",
                "indices:data/write/bulk",
                "indices:data/write/index"
            )
        )
    );

    public static Object getSetting(String name) {
        assert Settings.settings.containsKey(name) : "invalid setting name: " + name;
        return Settings.settings.get(name);
    }

    private static String getFromJVMOrDefault(String key, String defaultSetting) {
        String setting = System.getProperty("default.policy");
        if (setting == null) {
            setting = defaultSetting;
        }
        return setting;
    }
}
