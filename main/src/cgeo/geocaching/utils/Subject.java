package cgeo.geocaching.utils;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Synchronized implementation of the {@link ISubject} interface.
 *
 * @param <T>
 *         the kind of data to observe
 */
public class Subject<T> implements ISubject<T> {

    /**
     * Collection of observers.
     */
    protected final Set<IObserver<? super T>> observers = new LinkedHashSet<IObserver<? super T>>();

    @Override
    public synchronized boolean addObserver(final IObserver<? super T> observer) {
        final boolean added = observers.add(observer);
        if (added & observers.size() == 1) {
            onFirstObserver();
        }
        return added;
    }

    @Override
    public synchronized boolean deleteObserver(final IObserver<? super T> observer) {
        final boolean removed = observers.remove(observer);
        if (removed && observers.isEmpty()) {
            onLastObserver();
        }
        return removed;
    }

    @Override
    public synchronized boolean notifyObservers(final T arg) {
        final boolean nonEmpty = !observers.isEmpty();
        for (final IObserver<? super T> observer : observers) {
            observer.update(arg);
        }
        return nonEmpty;
    }

    @Override
    public synchronized int sizeObservers() {
        return observers.size();
    }

    @Override
    public synchronized boolean clearObservers() {
        final boolean nonEmpty = !observers.isEmpty();
        for (final IObserver<? super T> observer : observers) {
            deleteObserver(observer);
        }
        return nonEmpty;
    }

    /**
     * Method called when the collection of observers goes from empty to non-empty.
     * <p/>
     * The default implementation does nothing and may be overwritten by child classes.
     */
    protected void onFirstObserver() {
    }

    /**
     * Method called when the collection of observers goes from non-empty to empty.
     * <p/>
     * The default implementation does nothing and may be overwritten by child classes.
     */
    protected void onLastObserver() {
    }

}
