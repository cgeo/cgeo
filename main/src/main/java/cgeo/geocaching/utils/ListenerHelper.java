package cgeo.geocaching.utils;

import androidx.annotation.NonNull;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Consumer;
import java.util.function.Predicate;

import javax.annotation.Nullable;

import io.reactivex.rxjava3.core.Scheduler;
import io.reactivex.rxjava3.disposables.Disposable;

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

    public T removeListener(final int listenerId) {
        lock.writeLock().lock();
        try {
            return listeners.remove(listenerId);
        } finally {
            lock.writeLock().unlock();
        }
    }

    public Disposable addListenerWithDisposable(final T listener) {
        final int id = addListener(listener);
        return new SimpleDisposable(() -> removeListener(id));
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
        execute(null, action);
    }

    public int execute(@Nullable final Scheduler scheduler, final Consumer<T> action) {
        return executeWithRemove(scheduler, t -> {
            action.accept(t);
            return false;
        });
    }

    public int executeWithRemove(@Nullable final Scheduler scheduler, final Predicate<T> action) {
        if (action == null) {
            return 0;
        }
        final Scheduler schedulerUsed = scheduler == null ? AndroidRxUtils.mainThreadScheduler : scheduler;
        lock.readLock().lock();
        try {
            for (Map.Entry<Integer, T> entry : listeners.entrySet()) {
                schedulerUsed.createWorker().schedule(() -> {
                    final boolean doRemove = action.test(entry.getValue());
                    if (doRemove) {
                        removeListener(entry.getKey());
                    }
                });
            }
            return listeners.size();
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

    @NonNull
    @Override
    public String toString() {
        return "ListenerHelper<" + listeners.size() + ">";
    }
}
