package cgeo.geocaching;

import cgeo.geocaching.activity.AbstractActionBarActivity;
import cgeo.geocaching.connector.internal.InternalConnector;
import cgeo.geocaching.enumerations.LoadFlags;
import cgeo.geocaching.enumerations.WaypointType;
import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.models.Waypoint;
import cgeo.geocaching.storage.DataStore;
import cgeo.geocaching.utils.Log;
import cgeo.geocaching.utils.MatcherWrapper;

import android.os.Bundle;

import androidx.annotation.Nullable;

import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

public class NavigateAnyPointActivity extends AbstractActionBarActivity {
    private static final Pattern PATTERN_COORDS_NAME = Pattern.compile("^geo:0,0\\?q=([-]?[0-9]{1,2}\\.[0-9]{1,15}),([-]?[0-9]{1,3}\\.[0-9]{1,15})(\\((.*)\\))?$");
    private static final Pattern PATTERN_COORDS = Pattern.compile("^geo:([-]?[0-9]{1,2}\\.[0-9]{1,15}),([-]?[0-9]{1,3}\\.[0-9]{1,15})$");
    private static final Pattern PATTERN_COORDS_ZOOM = Pattern.compile("^geo:([-]?[0-9]{1,2}\\.[0-9]{1,15}),([-]?[0-9]{1,3}\\.[0-9]{1,15})\\?z=([1-9]|1[0-9]|2[0-3])$");

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        InternalConnector.assertHistoryCacheExists(this);

        // check if "geo" action is requested
        final String data = getIntent().getDataString();
        if (StringUtils.isNotBlank(data)) {
            MatcherWrapper match = new MatcherWrapper(PATTERN_COORDS, data);
            if (match.find()) {
                Log.i("Received a geo intent: lat=" + match.group(1) + ", lon=" + match.group(2));
                createHistoryWaypoint(Double.parseDouble(match.group(1)), Double.parseDouble(match.group(2)), null);
            } else {
                match = new MatcherWrapper(PATTERN_COORDS_NAME, data);
                if (match.find()) {
                    Log.i("Received a geo intent: lat=" + match.group(1) + ", lon=" + match.group(2) + ", name=" + match.group(4));
                    createHistoryWaypoint(Double.parseDouble(match.group(1)), Double.parseDouble(match.group(2)), match.group(4));
                } else {
                    match = new MatcherWrapper(PATTERN_COORDS_ZOOM, data);
                    if (match.find()) {
                        Log.i("Received a geo intent: lat=" + match.group(1) + ", lon=" + match.group(2) + ", zoom=" + match.group(3) + " (zoom level being ignored currently)");
                        createHistoryWaypoint(Double.parseDouble(match.group(1)), Double.parseDouble(match.group(2)), null);
                    }
                }
            }
        }

        CacheDetailActivity.startActivity(this, InternalConnector.GEOCODE_HISTORY_CACHE, true);
        finish();
    }

    private static void createHistoryWaypoint(final double latitude, final double longitude, @Nullable final String name) {
        final Geocache cache = DataStore.loadCache(InternalConnector.GEOCODE_HISTORY_CACHE, LoadFlags.LOAD_CACHE_OR_DB);
        if (null != cache) {
            final Waypoint newWaypoint = new Waypoint(null != name ? name : Waypoint.getDefaultWaypointName(cache, WaypointType.WAYPOINT), WaypointType.WAYPOINT, true);
            newWaypoint.setCoords(new Geopoint(latitude, longitude));
            newWaypoint.setGeocode(cache.getGeocode());
            cache.addOrChangeWaypoint(newWaypoint, true);
        }
    }
}
