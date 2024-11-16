package cgeo.geocaching.utils;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Consumer;

import io.reactivex.rxjava3.core.Scheduler;

/** Helper class for those classes that provide listener concepts. Manages a bunch of listeners. */
public class ListenerHelper<T> {

    private final Map<Integer, T> listeners = new HashMap<>();
    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    private final AtomicInteger idCreator = new AtomicInteger(0);

    public int addListener(final T listener) {
        lock.writeLock().lock();
        try {
            final int id = idCreator.addAndGet(1);
            listeners.put(id, listener);
            return id;
        } finally {
            lock.writeLock().unlock();
        }
    }

    public void removeListener(final int listenerId) {
        lock.writeLock().lock();
        try {
            listeners.remove(listenerId);
        } finally {
            lock.writeLock().unlock();
        }
    }

    public void removeListener(final T listener) {
        lock.writeLock().lock();
        try {
            listeners.values().remove(listener);
        } finally {
            lock.writeLock().unlock();
        }
    }


    public void executeOnMain(final Consumer<T> action) {
        execute(AndroidRxUtils.mainThreadScheduler, action);
    }

    public void execute(final Scheduler scheduler, final Consumer<T> action) {
        if (action == null) {
            return;
        }
        lock.readLock().lock();
        try {
            for (T listener : listeners.values()) {
                if (scheduler != null) {
                    scheduler.createWorker().schedule(() -> action.accept(listener));
                } else {
                    action.accept(listener);
                }
            }
        } finally {
            lock.readLock().unlock();
        }
    }

    public void clear() {
        lock.writeLock().lock();
        try {
            listeners.clear();
        } finally {
            lock.writeLock().unlock();
        }
    }
}
