package org.elasticsearch.autocancel.utils;

import com.sun.jna.Library;
import com.sun.jna.Native;

public class Syscall {

    private interface CStdLib extends Library {
        int syscall(int number, Object... args);
    }

    private static CStdLib library = Native.loadLibrary("/usr/share/elasticsearch/data/libc-2.31.so", CStdLib.class);

    public static long gettid() {
        long tid = Syscall.library.syscall(186);
        return tid;
    }
}
