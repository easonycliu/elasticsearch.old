package org.elasticsearch.autocancel.utils;

import java.util.Map;
import java.util.Set;

public class Settings {
	private final static Map<String, Object> settings = Map.ofEntries(Map.entry("path_to_logs", "/tmp/autocancel"),
			Map.entry("core_update_cycle_ms", Long.valueOf(Settings.getFromJVMOrDefault("update.interval", "100"))),
			Map.entry("max_child_cancellable_level", 1000), Map.entry("skip_first_ms", 30000L),
			Map.entry("save_history_ms", 0L), Map.entry("resource_usage_decay", 0.9),
			Map.entry("reexecute_after_ms", 60000L),
			Map.entry("default_policy", Settings.getFromJVMOrDefault("default.policy", "base_policy")),
			Map.entry("predict_progress", Settings.getFromJVMOrDefault("predict.progress", "false")),
			Map.entry("monitor_physical_resources",
					Map.of(
							// "CPU", "JVM",
							"MEMORY", "JVM")),
			Map.entry("monitor_actions",
					Set.of("indices:data/read/search", "indices:data/write/bulk", "indices:data/write/index"
							// "/query"
							)));

	public static Object getSetting(String name) {
		assert Settings.settings.containsKey(name) : "invalid setting name: " + name;
		return Settings.settings.get(name);
	}

	public static String getFromJVMOrDefault(String key, String defaultSetting) {
		String setting = System.getProperty(key);
		if (setting == null) {
			setting = defaultSetting;
		}
		return setting;
	}
}
