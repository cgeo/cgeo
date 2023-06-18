package cgeo.geocaching.apps.navi;

import cgeo.geocaching.models.Geocache;

import android.content.Context;

import androidx.annotation.NonNull;

public interface TargetSelectorNavigationApp extends CacheNavigationApp {

    /*
     * same as navigate(...), but will display a selector if cache has more than one waypoint
     */
    void navigateWithTargetSelector(@NonNull Context context, @NonNull Geocache cache);

}
