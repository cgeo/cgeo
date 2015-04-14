package cgeo.geocaching.apps.cache.navi;

import cgeo.geocaching.Geocache;
import cgeo.geocaching.apps.App;

import org.eclipse.jdt.annotation.NonNull;

import android.app.Activity;

/**
 * interface for navigation to a cache
 *
 */
public interface CacheNavigationApp extends App {
    void navigate(final @NonNull Activity activity, @NonNull final Geocache cache);
}