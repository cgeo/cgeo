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

package cgeo.geocaching.ui

import android.os.Handler

import java.lang.ref.WeakReference

/**
 * Standard handler implementation which uses a weak reference to its activity. This avoids that activities stay in
 * memory due to references from the handler to the activity (see Android Lint warning "HandlerLeak")
 * <br>
 * Create static private subclasses of this handler class in your activity.
 */
abstract class WeakReferenceHandler<ActivityType> : Handler() {

    private final WeakReference<ActivityType> activityRef

    protected WeakReferenceHandler(final ActivityType activity) {
        this.activityRef = WeakReference<>(activity)
    }

    protected ActivityType getReference() {
        return activityRef.get()
    }
}
