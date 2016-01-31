package cgeo.geocaching.apps.navi;

import android.support.annotation.StringRes;
import cgeo.geocaching.R;
import cgeo.geocaching.activity.ActivityMixin;
import cgeo.geocaching.location.Geopoint;

import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.models.Waypoint;
import org.eclipse.jdt.annotation.NonNull;

import android.app.Activity;
import android.content.Intent;

abstract class OruxMapsApp extends AbstractPointNavigationApp {

    private static final String ORUXMAPS_EXTRA_LONGITUDE = "targetLon";
    private static final String ORUXMAPS_EXTRA_LATITUDE = "targetLat";
    private static final String ORUXMAPS_EXTRA_NAME = "targetName";
    private static final String INTENT_ONLINE = "com.oruxmaps.VIEW_MAP_ONLINE";
    private static final String INTENT_OFFLINE = "com.oruxmaps.VIEW_MAP_OFFLINE";

    private OruxMapsApp(@StringRes final int nameResourceId, String intent) {
        super(getString(nameResourceId), intent);
    }

    private void navigate(final @NonNull Activity activity, final @NonNull Geopoint point, final @NonNull String name) {
        final Intent intent = new Intent(this.intent);
        double[] targetLat = { point.getLatitude() };
        double[] targetLon = { point.getLongitude() };
        intent.putExtra(ORUXMAPS_EXTRA_LATITUDE, targetLat);//latitude, wgs84 datum
        intent.putExtra(ORUXMAPS_EXTRA_LONGITUDE, targetLon);//longitude, wgs84 datum
        if (!name.isEmpty()) {
            String[] targetName = { name };
            intent.putExtra(ORUXMAPS_EXTRA_NAME, targetName);
        }

        activity.startActivity(intent);
    }

    @Override
    public void navigate(final @NonNull Activity activity, final @NonNull Geopoint point) {
        navigate(activity, point, "Waypoint");
    }

    @Override
    public void navigate(@NonNull Activity activity, @NonNull Geocache cache) {
        Geopoint coords = cache.getCoords();
        if (coords != null) {
            navigate(activity, coords, cache.getName());
        } else {
            ActivityMixin.showToast(activity, activity.getResources().getString(R.string.err_nav_no_coordinates));
        }
    }

    @Override
    public void navigate(@NonNull Activity activity, @NonNull Waypoint waypoint) {
        Geopoint coords = waypoint.getCoords();
        if (coords != null) {
            navigate(activity, coords, waypoint.getName());
        } else {
            ActivityMixin.showToast(activity, activity.getResources().getString(R.string.err_nav_no_coordinates));
        }
    }

    static class OruxOnlineMapApp extends OruxMapsApp {
        OruxOnlineMapApp() {
            super(R.string.cache_menu_oruxmaps_online, INTENT_ONLINE);
        }
    }

    static class OruxOfflineMapApp extends OruxMapsApp {
        OruxOfflineMapApp() {
            super(R.string.cache_menu_oruxmaps_offline, INTENT_OFFLINE);
        }
    }
}
