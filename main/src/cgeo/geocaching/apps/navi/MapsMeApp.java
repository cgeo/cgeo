package cgeo.geocaching.apps.navi;

import cgeo.geocaching.R;
import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.models.Waypoint;

import android.app.Activity;
import android.content.Context;
import android.support.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;

import com.mapswithme.maps.api.MWMPoint;
import com.mapswithme.maps.api.MapsWithMeApi;

class MapsMeApp extends AbstractPointNavigationApp {

    protected MapsMeApp() {
        super(getString(R.string.cache_menu_mapswithme), null);
    }

    @Override
    public void navigate(@NonNull final Context context, @NonNull final Geopoint coords) {
        navigate(context, coords, getString(R.string.unknown));
    }

    @Override
    public void navigate(@NonNull final Context context, @NonNull final Geocache cache) {
        final List<Waypoint> waypoints = cache.getWaypoints();
        if (waypoints.isEmpty()) {
            navigate(context, cache.getCoords(), cache.getName());
        } else {
            navigateWithWaypoints(context, cache);
        }
    }

    private static void navigateWithWaypoints(final Context context, final Geocache cache) {
        if (!(context instanceof Activity)) {
            // TODO Mapsme API will do a hard cast. We could locally fix this by re-declaring all API methods
            return;
        }
        final ArrayList<MWMPoint> points = new ArrayList<>();
        points.add(new MWMPoint(cache.getCoords().getLatitude(), cache.getCoords().getLongitude(), cache.getName()));
        for (final Waypoint waypoint : cache.getWaypoints()) {
            final Geopoint coords = waypoint.getCoords();
            if (coords != null) {
                points.add(new MWMPoint(coords.getLatitude(), coords.getLongitude(), waypoint.getName(), waypoint.getGeocode()));
            }
        }
        final MWMPoint[] pointsArray = points.toArray(new MWMPoint[points.size()]);
        MapsWithMeApi.showPointsOnMap((Activity) context, cache.getName(), pointsArray);
    }

    private static void navigate(final Context context, final Geopoint coords, final String label) {
        if (!(context instanceof Activity)) {
            // TODO Mapsme API will do a hard cast. We could locally fix this by re-declaring all API methods
            return;
        }
        MapsWithMeApi.showPointOnMap((Activity) context, coords.getLatitude(), coords.getLongitude(), label);
    }

    @Override
    public void navigate(@NonNull final Context context, @NonNull final Waypoint waypoint) {
        navigate(context, waypoint.getCoords(), waypoint.getName());
    }

    @Override
    public boolean isInstalled() {
        // the library can handle the app not being installed
        return true;
    }

}
