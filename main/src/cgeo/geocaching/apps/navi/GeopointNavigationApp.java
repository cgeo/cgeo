package cgeo.geocaching.apps.navi;

import cgeo.geocaching.location.Geopoint;

import android.app.Activity;

import androidx.annotation.NonNull;

/**
 * interface for navigation to a coordinate. This one cannot be enabled/disabled.
 *
 */
interface GeopointNavigationApp {
    void navigate(@NonNull Activity activity, @NonNull Geopoint coords);
}
