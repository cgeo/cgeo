package cgeo.geocaching.apps.navi;

import cgeo.geocaching.location.Geopoint;

import android.content.Context;
import android.support.annotation.NonNull;

/**
 * interface for navigation to a coordinate. This one cannot be enabled/disabled.
 *
 */
interface GeopointNavigationApp {
    void navigate(@NonNull final Context context, @NonNull final Geopoint coords);
}
