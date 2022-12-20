package cgeo.geocaching.apps.navi;

import cgeo.geocaching.R;
import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.models.Waypoint;

import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;

import androidx.annotation.NonNull;

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
        final Activity activity = getActivity(context);
        if (activity == null) {
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
        MapsWithMeApi.showPointsOnMap(activity, cache.getName(), pointsArray);
    }

    private static void navigate(final Context context, final Geopoint coords, final String label) {
        final Activity activity = getActivity(context);
        if (activity == null) {
            return;
        }

        MapsWithMeApi.showPointOnMap(activity, coords.getLatitude(), coords.getLongitude(), label);
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

    private static Activity getActivity(final Context context) {
        // TODO Mapsme API will do a hard cast. We could locally fix this by re-declaring all API methods
        if (context instanceof Activity) {
            return (Activity) context;
        } else if (context instanceof ContextWrapper) {
            return getActivity(((ContextWrapper) context).getBaseContext());
        }
        return null;
    }

}
