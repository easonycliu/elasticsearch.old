package org.elasticsearch.autocancel.utils.logger;

import java.io.Closeable;

// TODO: maybe we can connect to log4j (application related)
public class Logger implements Closeable {
	private static String ignore;

	public Logger(String fileBaseName) {
		Logger.ignore = fileBaseName;
	}

	public void log(String line) {
		Logger.ignore = line;
	}

	public static void systemTrace(String line) {
		Logger.ignore = line;
	}

	public static void systemInfo(String line) {
		Logger.ignore = line;
	}

	public static void systemWarn(String line) {
		Logger.ignore = line;
	}

	public String ignore() {
		return Logger.ignore;
	}

	@Override
	public void close() {}
}
