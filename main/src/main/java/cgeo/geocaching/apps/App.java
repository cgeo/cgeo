package cgeo.geocaching.apps;

import cgeo.geocaching.models.Geocache;

import androidx.annotation.NonNull;

public interface App {
    boolean isInstalled();

    /**
     * Whether or not an application can be used as the default navigation.
     */
    boolean isUsableAsDefaultNavigationApp();

    @NonNull
    String getName();

    /**
     * Whether or not the app can be used with the given cache (may depend on properties of the cache).
     */
    boolean isEnabled(@NonNull Geocache cache);
}
