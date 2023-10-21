package cgeo.geocaching.utils.livedata;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.util.Consumer;

/**
 * Helper class to wrap events for usage with LiveData
 * <p>
 * Implementation is based on <a href="https://medium.com/androiddevelopers/livedata-with-snackbar-navigation-and-other-events-the-singleliveevent-case-ac2622673150">this guide</a>
 */
public class Event<T> {

    private final T content;
    private boolean hasBeenHandled = false;

    public Event(final T content) {
        this.content = content;
    }

    /**
     * @return the event content, or null if already handled
     */
    @Nullable
    public T getContentIfNotHandled() {
        if (hasBeenHandled) {
            return null;
        }
        hasBeenHandled = true;
        return content;
    }

    /**
     * Invoke the given consumer, if event was not yet handled
     *
     * @param consumer action which should be performed on the given event content
     */
    public void ifNotHandled(@NonNull final Consumer<T> consumer) {
        if (hasBeenHandled) {
            return;
        }
        hasBeenHandled = true;
        consumer.accept(content);
    }

    /**
     * @return the event content, no matter whether it was already handled
     */
    public T peek() {
        return content;
    }
}
