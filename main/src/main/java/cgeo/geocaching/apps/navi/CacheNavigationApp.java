package cgeo.geocaching.apps.navi;

import cgeo.geocaching.apps.App;
import cgeo.geocaching.models.Geocache;

import android.content.Context;

import androidx.annotation.NonNull;

/**
 * interface for navigation to a cache
 */
public interface CacheNavigationApp extends App {
    /**
     * Navigate to the given cache. The caller will assert that cache.getCoords() is not null.
     */
    void navigate(@NonNull Context context, @NonNull Geocache cache);
}
