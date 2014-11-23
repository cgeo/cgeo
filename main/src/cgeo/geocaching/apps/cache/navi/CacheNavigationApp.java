package cgeo.geocaching.apps.cache.navi;

import cgeo.geocaching.Geocache;
import cgeo.geocaching.apps.App;

import android.app.Activity;

/**
 * interface for navigation to a cache
 *
 */
public interface CacheNavigationApp extends App {
    void navigate(final Activity activity, final Geocache cache);
}