package cgeo.geocaching.ui;

import android.os.Handler;

import java.lang.ref.WeakReference;

/**
 * Standard handler implementation which uses a weak reference to its activity. This avoids that activities stay in
 * memory due to references from the handler to the activity (see Android Lint warning "HandlerLeak")
 *
 * Create static private subclasses of this handler class in your activity.
 */
public abstract class WeakReferenceHandler<ActivityType> extends Handler {

    private final WeakReference<ActivityType> activityRef;

    protected WeakReferenceHandler(final ActivityType activity) {
        this.activityRef = new WeakReference<>(activity);
    }

    protected ActivityType getReference() {
        return activityRef.get();
    }
}
