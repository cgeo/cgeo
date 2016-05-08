package cgeo.geocaching.apps.navi;

import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.apps.App;

import org.eclipse.jdt.annotation.NonNull;

import android.app.Activity;

/**
 * interface for navigation to a cache
 *
 */
public interface CacheNavigationApp extends App {
    void navigate(@NonNull final Activity activity, @NonNull final Geocache cache);
}
