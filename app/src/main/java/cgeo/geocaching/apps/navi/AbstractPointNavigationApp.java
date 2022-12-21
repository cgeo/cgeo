package cgeo.geocaching.apps.navi;

import cgeo.geocaching.apps.AbstractApp;
import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.models.Waypoint;

import android.content.Context;
import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * navigation app for simple point navigation (no differentiation between cache/waypoint/point)
 */
abstract class AbstractPointNavigationApp extends AbstractApp implements CacheNavigationApp, WaypointNavigationApp, GeopointNavigationApp {

    protected AbstractPointNavigationApp(@NonNull final String name, @Nullable final String intent) {
        super(name, intent);
    }

    protected AbstractPointNavigationApp(@NonNull final String name, @Nullable final String intent, @Nullable final String packageName) {
        super(name, intent, packageName);
    }

    @Override
    public void navigate(@NonNull final Context context, @NonNull final Geocache cache) {
        final Geopoint coords = cache.getCoords();
        assert coords != null; // asserted by caller
        navigate(context, coords);
    }

    @Override
    public void navigate(@NonNull final Context context, @NonNull final Waypoint waypoint) {
        final Geopoint coords = waypoint.getCoords();
        assert coords != null; // asserted by caller
        navigate(context, coords);
    }

    @Override
    public boolean isEnabled(@NonNull final Geocache cache) {
        return cache.getCoords() != null;
    }

    @Override
    public boolean isEnabled(@NonNull final Waypoint waypoint) {
        return waypoint.getCoords() != null;
    }

    protected static void addIntentExtras(@NonNull final Intent intent, @NonNull final Waypoint waypoint) {
        intent.putExtra("name", waypoint.getName());
        intent.putExtra("code", waypoint.getGeocode());
    }

    protected static void addIntentExtras(@NonNull final Intent intent, @NonNull final Geocache cache) {
        intent.putExtra("difficulty", cache.getDifficulty());
        intent.putExtra("terrain", cache.getTerrain());
        intent.putExtra("name", cache.getName());
        intent.putExtra("code", cache.getGeocode());
        intent.putExtra("size", cache.getSize().getL10n());
    }
}
