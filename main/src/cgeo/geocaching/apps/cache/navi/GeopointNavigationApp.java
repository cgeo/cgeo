package cgeo.geocaching.apps.cache.navi;

import cgeo.geocaching.geopoint.Geopoint;

import android.app.Activity;

/**
 * interface for navigation to a coordinate. This one cannot be enabled/disabled.
 *
 */
public interface GeopointNavigationApp {
    void navigate(final Activity activity, final Geopoint coords);
}
