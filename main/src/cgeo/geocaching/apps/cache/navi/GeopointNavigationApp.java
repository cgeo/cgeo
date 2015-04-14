package cgeo.geocaching.apps.cache.navi;

import cgeo.geocaching.location.Geopoint;

import org.eclipse.jdt.annotation.NonNull;

import android.app.Activity;

/**
 * interface for navigation to a coordinate. This one cannot be enabled/disabled.
 *
 */
interface GeopointNavigationApp {
    void navigate(@NonNull final Activity activity, @NonNull final Geopoint coords);
}
