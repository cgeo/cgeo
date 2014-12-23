package cgeo.geocaching.apps.cache.navi;

import cgeo.geocaching.location.Geopoint;

import android.app.Activity;

/**
 * interface for navigation to a coordinate. This one cannot be enabled/disabled.
 *
 */
interface GeopointNavigationApp {
    void navigate(final Activity activity, final Geopoint coords);
}
