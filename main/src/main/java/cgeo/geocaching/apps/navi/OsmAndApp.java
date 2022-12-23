package cgeo.geocaching.apps.navi;

import cgeo.geocaching.R;
import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.models.Waypoint;
import cgeo.geocaching.network.Parameters;
import cgeo.geocaching.utils.ProcessUtils;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.apache.commons.collections4.CollectionUtils;

public class OsmAndApp extends AbstractPointNavigationApp {

    private static final String PARAM_NAME = "name";
    private static final String PARAM_LAT = "lat";
    private static final String PARAM_LON = "lon";
    private static final String PREFIX = "osmand.api://";
    private static final String GET_INFO = "get_info";
    private static final String ADD_MAP_MARKER = "add_map_marker";

    protected OsmAndApp() {
        super(getString(R.string.cache_menu_osmand), null);
    }

    @Override
    public boolean isInstalled() {
        return ProcessUtils.isIntentAvailable(Intent.ACTION_VIEW, Uri.parse(PREFIX + GET_INFO));
    }

    @Override
    public void navigate(@NonNull final Context context, @NonNull final Geocache cache) {
        final Geopoint coords = cache.getCoords();
        assert coords != null; // guaranteed by super class
        navigate(context, coords, cache.getName());
    }

    @Override
    public void navigate(@NonNull final Context context, @NonNull final Waypoint waypoint) {
        final Geopoint coords = waypoint.getCoords();
        assert coords != null; // guaranteed by super class
        navigate(context, coords, waypoint.getName());
    }

    @Override
    public void navigate(@NonNull final Context context, @NonNull final Geopoint coords) {
        navigate(context, coords, context.getString(R.string.osmand_marker_cgeo));
    }

    private static void navigate(@NonNull final Context context, @NonNull final Geopoint coords, @NonNull final String markerName) {
        final Parameters params = new Parameters(PARAM_LAT, String.valueOf(coords.getLatitude()),
                PARAM_LON, String.valueOf(coords.getLongitude()),
                PARAM_NAME, markerName);
        context.startActivity(buildIntent(params));
    }

    private static Intent buildIntent(@Nullable final Parameters parameters) {
        final StringBuilder stringBuilder = new StringBuilder(PREFIX);
        stringBuilder.append(ADD_MAP_MARKER);
        if (CollectionUtils.isNotEmpty(parameters)) {
            stringBuilder.append('?');
            stringBuilder.append(parameters);
        }
        return new Intent(Intent.ACTION_VIEW, Uri.parse(stringBuilder.toString()));
    }
}
