package cgeo.geocaching.apps.navi;

import cgeo.geocaching.Geocache;
import cgeo.geocaching.R;
import cgeo.geocaching.Waypoint;
import cgeo.geocaching.location.Geopoint;

import com.mapswithme.maps.api.MWMPoint;
import com.mapswithme.maps.api.MapsWithMeApi;

import org.eclipse.jdt.annotation.NonNull;

import android.app.Activity;

import java.util.ArrayList;
import java.util.List;

class MapsMeApp extends AbstractPointNavigationApp {

    protected MapsMeApp() {
        super(getString(R.string.cache_menu_mapswithme), null);
    }

    @Override
    public void navigate(final @NonNull Activity activity, final @NonNull Geopoint coords) {
        navigate(activity, coords, getString(R.string.unknown));
    }

    @Override
    public void navigate(final @NonNull Activity activity, final @NonNull Geocache cache) {
        final List<Waypoint> waypoints = cache.getWaypoints();
        if (waypoints.isEmpty()) {
            navigate(activity, cache.getCoords(), cache.getName());
        } else {
            navigateWithWaypoints(activity, cache);
        }
    }

    private static void navigateWithWaypoints(final Activity activity, final Geocache cache) {
        final ArrayList<MWMPoint> points = new ArrayList<>();
        points.add(new MWMPoint(cache.getCoords().getLatitude(), cache.getCoords().getLongitude(), cache.getName()));
        for (final Waypoint waypoint : cache.getWaypoints()) {
            final Geopoint coords = waypoint.getCoords();
            if (coords != null) {
                points.add(new MWMPoint(coords.getLatitude(), coords.getLongitude(), waypoint.getName(), waypoint.getGeocode()));
            }
        }
        final MWMPoint[] pointsArray = points.toArray(new MWMPoint[points.size()]);
        MapsWithMeApi.showPointsOnMap(activity, cache.getName(), pointsArray);
    }

    private static void navigate(final Activity activity, final Geopoint coords, final String label) {
        MapsWithMeApi.showPointOnMap(activity, coords.getLatitude(), coords.getLongitude(), label);
    }

    @Override
    public void navigate(final @NonNull Activity activity, final @NonNull Waypoint waypoint) {
        navigate(activity, waypoint.getCoords(), waypoint.getName());
    }

    @Override
    public boolean isInstalled() {
        // the library can handle the app not being installed
        return true;
    }

}
