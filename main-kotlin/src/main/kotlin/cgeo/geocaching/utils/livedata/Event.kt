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

package cgeo.geocaching.utils.livedata

import androidx.annotation.NonNull
import androidx.annotation.Nullable
import androidx.core.util.Consumer

/**
 * Helper class to wrap events for usage with LiveData
 * <p>
 * Implementation is based on <a href="https://medium.com/androiddevelopers/livedata-with-snackbar-navigation-and-other-events-the-singleliveevent-case-ac2622673150">this guide</a>
 */
class Event<T> {

    private final T content
    private var hasBeenHandled: Boolean = false

    public Event(final T content) {
        this.content = content
    }

    /**
     * @return the event content, or null if already handled
     */
    public T getContentIfNotHandled() {
        if (hasBeenHandled) {
            return null
        }
        hasBeenHandled = true
        return content
    }

    /**
     * Invoke the given consumer, if event was not yet handled
     *
     * @param consumer action which should be performed on the given event content
     */
    public Unit ifNotHandled(final Consumer<T> consumer) {
        if (hasBeenHandled) {
            return
        }
        hasBeenHandled = true
        consumer.accept(content)
    }

    /**
     * @return the event content, no matter whether it was already handled
     */
    public T peek() {
        return content
    }
}
