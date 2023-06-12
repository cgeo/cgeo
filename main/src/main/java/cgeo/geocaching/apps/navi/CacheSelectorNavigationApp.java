package cgeo.geocaching.apps.navi;

import cgeo.geocaching.models.Geocache;

import android.content.Context;

import androidx.annotation.NonNull;

public interface CacheSelectorNavigationApp extends CacheNavigationApp {

    /*
     * same as navigate(...), but will display a selector if cache has more than one waypoint
     */
    void navigateWithoutSelector(@NonNull Context context, @NonNull Geocache cache);

}
