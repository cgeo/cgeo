package cgeo.geocaching.utils;

/**
 * Synchronized implementation of the {@link ISubject} interface with an added pull interface.
 *
 * @param <T>
 *         the kind of data to observe
 */
public class MemorySubject<T> extends Subject<T> {

    /**
     * The latest version of the observed data.
     * <p/>
     * A child class implementation may want to set this field from its constructors, in case early observers request
     * the data before it got a chance to get updated. Otherwise, <code>null</code> will be returned until updated
     * data is available.
     */
    protected T memory;

    @Override
    public synchronized boolean addObserver(final IObserver<? super T> observer) {
        final boolean added = super.addObserver(observer);
        if (added && memory != null) {
            observer.update(memory);
        }
        return added;
    }

    @Override
    public synchronized boolean notifyObservers(final T data) {
        memory = data;
        return super.notifyObservers(data);
    }

    /**
     * Get the memorized version of the data.
     *
     * @return the initial data set by the subject (which may be <code>null</code>),
     *         or the updated data if it is available
     */
    public T getMemory() {
        return memory;
    }

}
