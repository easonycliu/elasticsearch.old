package org.elasticsearch.autocancel.utils;

import java.lang.Runnable;
import java.lang.management.ThreadMXBean;
import java.lang.management.ThreadInfo;
import java.lang.management.MonitorInfo;
import java.lang.management.LockInfo;
import java.lang.management.ManagementFactory;
import java.util.stream.Collectors;
import java.util.Arrays;

public class ThreadDumper implements Runnable {
    @Override
    public void run() {
        ThreadMXBean bean = ManagementFactory.getThreadMXBean();
            ThreadInfo[] infos = bean.dumpAllThreads(true, true);
            System.out.println(Arrays.stream(infos).map((info) -> {
                StringBuilder sb = new StringBuilder("\"" + info.getThreadName() + "\"" +
                                                    (info.isDaemon() ? " daemon" : "") +
                                                    " prio=" + info.getPriority() +
                                                    " Id=" + info.getThreadId() + " " +
                                                    info.getThreadState());
                if (info.getLockName() != null) {
                    sb.append(" on " + info.getLockName());
                }
                if (info.getLockOwnerName() != null) {
                    sb.append(" owned by \"" + info.getLockOwnerName() +
                            "\" Id=" + info.getLockOwnerId());
                }
                if (info.isSuspended()) {
                    sb.append(" (suspended)");
                }
                if (info.isInNative()) {
                    sb.append(" (in native)");
                }
                sb.append('\n');
                int i = 0;
                StackTraceElement[] stackTrace = info.getStackTrace();
                for (; i < stackTrace.length; i++) {
                    StackTraceElement ste = stackTrace[i];
                    sb.append("\tat " + ste.toString());
                    sb.append('\n');
                    if (i == 0 && info.getLockInfo() != null) {
                        Thread.State ts = info.getThreadState();
                        switch (ts) {
                            case BLOCKED:
                                sb.append("\t-  blocked on " + info.getLockInfo());
                                sb.append('\n');
                                break;
                            case WAITING:
                                sb.append("\t-  waiting on " + info.getLockInfo());
                                sb.append('\n');
                                break;
                            case TIMED_WAITING:
                                sb.append("\t-  waiting on " + info.getLockInfo());
                                sb.append('\n');
                                break;
                            default:
                        }
                    }

                    for (MonitorInfo mi : info.getLockedMonitors()) {
                        if (mi.getLockedStackDepth() == i) {
                            sb.append("\t-  locked " + mi);
                            sb.append('\n');
                        }
                    }
            }
            if (i < stackTrace.length) {
                sb.append("\t...");
                sb.append('\n');
            }

            LockInfo[] locks = info.getLockedSynchronizers();
            if (locks.length > 0) {
                sb.append("\n\tNumber of locked synchronizers = " + locks.length);
                sb.append('\n');
                for (LockInfo li : locks) {
                    sb.append("\t- " + li);
                    sb.append('\n');
                }
            }
            sb.append('\n');
            return sb.toString();
            }).collect(Collectors.joining()));
    }
}
