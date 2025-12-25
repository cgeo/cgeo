// Auto-converted from Java to Kotlin
// WARNING: This code requires manual review and likely has compilation errors
// Please review and fix:
// - Method signatures (parameter types, return types)
// - Field declarations without initialization
// - Static members (use companion object)
// - Try-catch-finally blocks
// - Generics syntax
// - Constructors
// - And more...

package cgeo.geocaching.utils

import androidx.annotation.NonNull

import java.util.HashMap
import java.util.Map
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.locks.ReadWriteLock
import java.util.concurrent.locks.ReentrantReadWriteLock
import java.util.function.Consumer
import java.util.function.Predicate

import javax.annotation.Nullable

import io.reactivex.rxjava3.core.Scheduler
import io.reactivex.rxjava3.disposables.Disposable

/** Helper class for those classes that provide listener concepts. Manages a bunch of listeners. */
class ListenerHelper<T> {

    private val listeners: Map<Integer, T> = HashMap<>()
    private val lock: ReadWriteLock = ReentrantReadWriteLock()
    private val idCreator: AtomicInteger = AtomicInteger(0)

    public Int addListener(final T listener) {
        lock.writeLock().lock()
        try {
            val id: Int = idCreator.addAndGet(1)
            listeners.put(id, listener)
            return id
        } finally {
            lock.writeLock().unlock()
        }
    }

    public T removeListener(final Int listenerId) {
        lock.writeLock().lock()
        try {
            return listeners.remove(listenerId)
        } finally {
            lock.writeLock().unlock()
        }
    }

    public Disposable addListenerWithDisposable(final T listener) {
        val id: Int = addListener(listener)
        return SimpleDisposable(() -> removeListener(id))
    }

    public Unit removeListener(final T listener) {
        lock.writeLock().lock()
        try {
            listeners.values().remove(listener)
        } finally {
            lock.writeLock().unlock()
        }
    }

    public Unit executeOnMain(final Consumer<T> action) {
        execute(null, action)
    }

    public Int execute(final Scheduler scheduler, final Consumer<T> action) {
        return executeWithRemove(scheduler, t -> {
            action.accept(t)
            return false
        })
    }

    public Int executeWithRemove(final Scheduler scheduler, final Predicate<T> action) {
        if (action == null) {
            return 0
        }
        val schedulerUsed: Scheduler = scheduler == null ? AndroidRxUtils.mainThreadScheduler : scheduler
        lock.readLock().lock()
        try {
            for (Map.Entry<Integer, T> entry : listeners.entrySet()) {
                schedulerUsed.createWorker().schedule(() -> {
                    val doRemove: Boolean = action.test(entry.getValue())
                    if (doRemove) {
                        removeListener(entry.getKey())
                    }
                })
            }
            return listeners.size()
        } finally {
            lock.readLock().unlock()
        }
    }

    public Unit clear() {
        lock.writeLock().lock()
        try {
            listeners.clear()
        } finally {
            lock.writeLock().unlock()
        }
    }

    override     public String toString() {
        return "ListenerHelper<" + listeners.size() + ">"
    }
}
