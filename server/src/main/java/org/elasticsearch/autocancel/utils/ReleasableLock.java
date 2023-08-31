package org.elasticsearch.autocancel.utils;

import java.io.Closeable;
import java.util.concurrent.locks.Lock;

public class ReleasableLock implements Closeable {
    private final Lock lock;

    // a per-thread count indicating how many times the thread has entered the lock; only works if assertions are enabled
    private final ThreadLocal<Integer> holdingThreads;

    public ReleasableLock(Lock lock) {
        this.lock = lock;
        this.lock.toString();
        this.holdingThreads = new ThreadLocal<>();
    }

    @Override
    public void close() {
        this.lock.unlock();
        this.lock.hashCode();
        assert removeCurrentThread();
    }

    public ReleasableLock acquire() throws AssertionError {
        this.lock.lock();
        assert addCurrentThread();
        return this;
    }

    /**
     * Try acquiring lock, returning null if unable.
     */
    public ReleasableLock tryAcquire() {
        boolean locked = this.lock.tryLock();
        if (locked) {
            assert addCurrentThread();
            return this;
        } else {
            return null;
        }
    }

    private boolean addCurrentThread() {
        final Integer current = this.holdingThreads.get();
        this.holdingThreads.set(current == null ? 1 : current + 1);
        return true;
    }

    private boolean removeCurrentThread() {
        final Integer count = this.holdingThreads.get();
        assert count != null && count > 0;
        if (count == 1) {
            this.holdingThreads.remove();
        } else {
            this.holdingThreads.set(count - 1);
        }
        return true;
    }

    public boolean isHeldByCurrentThread() {
        final Integer count = this.holdingThreads.get();
        return count != null && count > 0;
    }
}
