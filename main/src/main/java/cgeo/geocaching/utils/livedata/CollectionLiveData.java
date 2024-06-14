package cgeo.geocaching.utils.livedata;

import cgeo.geocaching.utils.Log;

import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * A data holder class whose content is a collection. Provides methods to safely read and write to this collection
 * avoiding concurrent-access-exceptions. Also, write access will notify consumers
 *
 * @param <T> The type of data held by the collection in this instance
 * @param <C> The type of collection
 */
public class CollectionLiveData<T, C extends Collection<T>> {

    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    private final MutableLiveData<Integer> liveData;
    private final C value;
    private final C valueReadOnly;

    private CollectionLiveData(@NonNull final Supplier<C> supplier, @Nullable final Function<C, C> readOnlyMaker) {
        this.value = supplier.get();
        this.valueReadOnly = readOnlyMaker == null ? this.value : readOnlyMaker.apply(this.value);
        this.liveData = new MutableLiveData<>(0);
    }

    public static <TT> CollectionLiveData<TT, Set<TT>> set(@NonNull final Supplier<Set<TT>> supplier) {
        return new CollectionLiveData<>(supplier, Collections::unmodifiableSet);
    }

    public static <TT> CollectionLiveData<TT, Set<TT>> set() {
        return set(HashSet::new);
    }

    /** executes an Action reading the collection */
    public void read(final Consumer<C> readAction) {
        lock.readLock().lock();
        try {
            readAction.accept(valueReadOnly);
        } finally {
            lock.readLock().unlock();
        }
    }

    /** executes an Action reading the collection and returning a result */
    public <R> R readWithResult(final Function<C, R> readFunction) {
        lock.readLock().lock();
        try {
            return readFunction.apply(valueReadOnly);
        } finally {
            lock.readLock().unlock();
        }
    }


    /** executes an Action writing to the collection. Notification is auto-executed after write */
    public void write(final boolean forcePost, final Consumer<C> writeAction) {
        lock.writeLock().lock();
        try {
            final int sizeBefore = value.size();
            writeAction.accept(value);
            Log.d("CollectionLiveData: write action changed size: " + sizeBefore + "->" + value.size());
        } finally {
            lock.writeLock().unlock();
        }
        notifyDataChanged(forcePost);
    }

    /** executes an Action writing to the collection. Notification is auto-executed after write */
    public void write(final Consumer<C> writeAction) {
        write(false, writeAction);
    }

        /** returns a list-copy from this collection.  */
    public List<T> getListCopy() {
        return readWithResult(ArrayList::new);
    }

    /** registers an observer which wants to read from the collection */
    public void observeForRead(@NonNull final LifecycleOwner owner, @NonNull final Observer<? super C> observer) {
        this.liveData.observe(owner, x -> read(observer::onChanged));
    }

    /** registers an observer which just wants to be notified that the collecton changed */
    public void observeForNotification(@NonNull final LifecycleOwner owner, @NonNull final Runnable observer) {
        this.liveData.observe(owner, x -> observer.run());
    }

    /** manually triggers a notification */
    public void notifyDataChanged(final boolean forcePost) {
        if (forcePost || !isMainThread()) {
            this.liveData.postValue(0);
        } else {
            this.liveData.setValue(0);
        }
    }

    public void notifyDataChanged() {
        notifyDataChanged(false);
    }

    private static boolean isMainThread() {
        return Looper.getMainLooper().getThread() == Thread.currentThread();
    }

}
