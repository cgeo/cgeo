package cgeo.geocaching.apps.navi;

import cgeo.geocaching.R;
import cgeo.geocaching.activity.ActivityMixin;
import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.models.Waypoint;
import cgeo.geocaching.network.Parameters;
import cgeo.geocaching.utils.ProcessUtils;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;


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
    public void navigate(@NonNull final Activity activity, @NonNull final Geocache cache) {
        if (cache.getCoords() != null) {
            navigate(activity, cache.getCoords(), cache.getName());
        } else {
            ActivityMixin.showToast(activity, activity.getResources().getString(R.string.err_nav_no_coordinates));
        }
    }

    @Override
    public void navigate(@NonNull final Activity activity, @NonNull final Waypoint waypoint) {
        if (waypoint.getCoords() != null) {
            navigate(activity, waypoint.getCoords(), waypoint.getName());
        } else {
            ActivityMixin.showToast(activity, activity.getResources().getString(R.string.err_nav_no_coordinates));
        }
    }

    @Override
    public void navigate(@NonNull final Activity activity, @NonNull final Geopoint coords) {
        navigate(activity, coords, activity.getResources().getString(R.string.osmand_marker_cgeo));
    }

    private void navigate(@NonNull final Activity activity, @NonNull final Geopoint coords, @NonNull final String markerName) {
        final Parameters params = new Parameters(PARAM_LAT, String.valueOf(coords.getLatitude()),
                PARAM_LON, String.valueOf(coords.getLongitude()),
                PARAM_NAME, markerName);
        activity.startActivity(buildIntent(params));
    }

    private Intent buildIntent(@Nullable final Parameters parameters) {
        final StringBuilder stringBuilder = new StringBuilder(PREFIX);
        stringBuilder.append(ADD_MAP_MARKER);
        if (parameters != null && !parameters.isEmpty()) {
            stringBuilder.append('?');
            stringBuilder.append(parameters.toString());
        }
        return new Intent(Intent.ACTION_VIEW, Uri.parse(stringBuilder.toString()));
    }
}
