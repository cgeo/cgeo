package cgeo.geocaching.apps.navi;

import cgeo.geocaching.R;
import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.location.GeopointFormatter.Format;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.models.Waypoint;

import android.content.Context;
import android.content.Intent;

import androidx.annotation.NonNull;

import java.util.ArrayList;

class RMapsApp extends AbstractPointNavigationApp {

    private static final String INTENT = "com.robert.maps.action.SHOW_POINTS";

    RMapsApp() {
        super(getString(R.string.cache_menu_rmaps), INTENT);
    }

    @Override
    public void navigate(@NonNull final Context context, @NonNull final Waypoint waypoint) {
        navigate(context, waypoint.getCoords(), waypoint.getLookup(), waypoint.getName());
    }

    private static void navigate(final Context context, final Geopoint coords, final String code, final String name) {
        final ArrayList<String> locations = new ArrayList<>();
        locations.add(coords.format(Format.LAT_LON_DECDEGREE_COMMA) + ";" + code + ";" + name);
        final Intent intent = new Intent(INTENT);
        intent.putStringArrayListExtra("locations", locations);
        context.startActivity(intent);
    }

    @Override
    public void navigate(@NonNull final Context context, @NonNull final Geocache cache) {
        navigate(context, cache.getCoords(), cache.getGeocode(), cache.getName());
    }

    @Override
    public void navigate(@NonNull final Context context, @NonNull final Geopoint coords) {
        navigate(context, coords, "", "");
    }
}
