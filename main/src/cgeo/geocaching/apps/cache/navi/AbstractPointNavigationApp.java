package cgeo.geocaching.apps.cache.navi;

import cgeo.geocaching.Geocache;
import cgeo.geocaching.R;
import cgeo.geocaching.Waypoint;
import cgeo.geocaching.activity.ActivityMixin;
import cgeo.geocaching.apps.AbstractApp;
import cgeo.geocaching.location.Geopoint;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

import android.app.Activity;
import android.content.Intent;

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
    public void navigate(@NonNull final Activity activity, final @NonNull Geocache cache) {
        navigateWithNullCheck(activity, cache.getCoords());
    }

    private void navigateWithNullCheck(@NonNull final Activity activity, @Nullable final Geopoint coords) {
        if (coords != null) {
            navigate(activity, coords);
        } else {
            ActivityMixin.showToast(activity, activity.getResources().getString(R.string.err_nav_no_coordinates));
        }
    }

    @Override
    public void navigate(final @NonNull Activity activity, final @NonNull Waypoint waypoint) {
        navigateWithNullCheck(activity, waypoint.getCoords());
    }

    @Override
    public boolean isEnabled(final @NonNull Geocache cache) {
        return cache.getCoords() != null;
    }

    @Override
    public boolean isEnabled(final @NonNull Waypoint waypoint) {
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
