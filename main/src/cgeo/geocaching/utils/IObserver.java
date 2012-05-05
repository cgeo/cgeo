package cgeo.geocaching.utils;

/**
 * Observer interface.
 * <p/>
 * An observer will receive updates about the observed object (implementing the {@link ISubject} interface) through its
 * {@link #update(Object)} method.
 * 
 * @param <T>
 *            the kind of data to observe
 */
public interface IObserver<T> {

    /**
     * Called when an observed object has updated its data.
     *
     * @param data
     *         the updated data
     */
    void update(final T data);

}
