package cgeo.geocaching.apps;

import cgeo.geocaching.Geocache;

import org.eclipse.jdt.annotation.NonNull;

public interface App {
    public boolean isInstalled();

    /**
     * Whether or not an application can be used as the default navigation.
     */
    public boolean isUsableAsDefaultNavigationApp();

    @NonNull
    public String getName();

    /**
     * @return the unique ID of the application, defined in res/values/ids.xml
     */
    int getId();

    /**
     * Whether or not the app can be used with the given cache (may depend on properties of the cache).
     *
     */
    boolean isEnabled(final Geocache cache);
}
