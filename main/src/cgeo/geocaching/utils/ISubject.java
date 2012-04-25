package cgeo.geocaching.utils;

/**
 * Interface for subjects objects. Those can be observed by objects implementing the {@link IObserver} interface.
 *
 * @param <T>
 *         the kind of data to observe
 */

public interface ISubject<T> {

    /**
     * Add an observer to the observers list.
     * <p/>
     * Observers will be notified with no particular order.
     *
     * @param observer
     *         the observer to add
     * @return true if the observer has been added, false if it was present already
     */
    public boolean addObserver(final IObserver<? super T> observer);

    /**
     * Delete an observer from the observers list.
     *
     * @param observer
     *         the observer to remove
     * @return true if the observer has been removed, false if it was not in the list of observers
     */
    public boolean deleteObserver(final IObserver<? super T> observer);

    /**
     * Number of observers currently observing the object.
     *
     * @return the number of observers
     */
    public int sizeObservers();

    /**
     * Notify all the observers that new data is available.
     * <p/>
     * The {@link IObserver#update(T)} method of each observer will be called with no particular order.
     *
     * @param data
     *         the updated data
     * @return true if at least one observer was notified, false if there were no observers
     */
    public boolean notifyObservers(final T data);

    /**
     * Clear the observers list.
     *
     * @return true if there were observers before calling this method, false if the observers list was empty
     */
    public boolean clearObservers();

}
