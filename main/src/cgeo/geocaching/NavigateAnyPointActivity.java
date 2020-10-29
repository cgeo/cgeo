package cgeo.geocaching;

import cgeo.geocaching.activity.AbstractActionBarActivity;
import cgeo.geocaching.connector.internal.InternalConnector;
import cgeo.geocaching.enumerations.LoadFlags;
import cgeo.geocaching.enumerations.WaypointType;
import cgeo.geocaching.list.StoredList;
import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.models.Waypoint;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.storage.DataStore;
import cgeo.geocaching.utils.Log;
import cgeo.geocaching.utils.MatcherWrapper;

import android.app.Activity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

public class NavigateAnyPointActivity extends AbstractActionBarActivity {
    private static final Pattern PATTERN_COORDS_NAME = Pattern.compile("^geo:0,0\\?q=([-]?[0-9]{1,2}\\.[0-9]{1,15}),([-]?[0-9]{1,3}\\.[0-9]{1,15})(\\((.*)\\))?$");
    private static final Pattern PATTERN_COORDS = Pattern.compile("^geo:([-]?[0-9]{1,2}\\.[0-9]{1,15}),([-]?[0-9]{1,3}\\.[0-9]{1,15})$");
    private static final Pattern PATTERN_COORDS_ZOOM = Pattern.compile("^geo:([-]?[0-9]{1,2}\\.[0-9]{1,15}),([-]?[0-9]{1,3}\\.[0-9]{1,15})\\?z=([1-9]|1[0-9]|2[0-3])$");

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTheme(Settings.isLightSkin() ? R.style.light : R.style.dark);

        InternalConnector.assertHistoryCacheExists(this);

        // check if "geo" action is requested
        boolean geoActionRequested = false;
        final String data = getIntent().getDataString();
        if (StringUtils.isNotBlank(data)) {
            MatcherWrapper match = new MatcherWrapper(PATTERN_COORDS, data);
            if (match.find()) {
                Log.i("Received a geo intent: lat=" + match.group(1) + ", lon=" + match.group(2));
                selectTargetType(this, Double.parseDouble(match.group(1)), Double.parseDouble(match.group(2)), null);
                geoActionRequested = true;
            } else {
                match = new MatcherWrapper(PATTERN_COORDS_NAME, data);
                if (match.find()) {
                    Log.i("Received a geo intent: lat=" + match.group(1) + ", lon=" + match.group(2) + ", name=" + match.group(4));
                    try {
                        selectTargetType(this, Double.parseDouble(match.group(1)), Double.parseDouble(match.group(2)), URLDecoder.decode(match.group(4), "UTF-8"));
                    } catch (UnsupportedEncodingException e) {
                        // try without URL decoding as fallback
                        selectTargetType(this, Double.parseDouble(match.group(1)), Double.parseDouble(match.group(2)), match.group(4));
                    }
                    geoActionRequested = true;
                } else {
                    match = new MatcherWrapper(PATTERN_COORDS_ZOOM, data);
                    if (match.find()) {
                        Log.i("Received a geo intent: lat=" + match.group(1) + ", lon=" + match.group(2) + ", zoom=" + match.group(3) + " (zoom level being ignored currently)");
                        selectTargetType(this, Double.parseDouble(match.group(1)), Double.parseDouble(match.group(2)), null);
                        geoActionRequested = true;
                    }
                }
            }
        }

        if (!geoActionRequested) {
            CacheDetailActivity.startActivity(this, InternalConnector.GEOCODE_HISTORY_CACHE, true);
            finish();
        }
    }

    private static void selectTargetType(final Activity context, final double latitude, final double longitude, @Nullable final String name) {

        final ArrayList<Geocache> items = new ArrayList<>();
        items.add(null);
        items.add(DataStore.loadCache(InternalConnector.GEOCODE_HISTORY_CACHE, LoadFlags.LOAD_CACHE_OR_DB));
        items.addAll(DataStore.loadUDCSorted());

        final LayoutInflater inflater = LayoutInflater.from(context);
        final ListAdapter adapter = new ArrayAdapter<Geocache>(context, R.layout.cacheslist_item_select, items) {
            @NonNull
            @Override
            public View getView(final int position, final View convertView, @NonNull final ViewGroup parent) {

                final View view = convertView == null ? inflater.inflate(R.layout.twotexts_twobuttons_item, parent, false) : convertView;
                final TextView title = (TextView) view.findViewById(R.id.title);
                final TextView detail = (TextView) view.findViewById(R.id.detail);

                final Geocache item = getItem(position);
                title.setText(item == null ? "<" + context.getString(R.string.create_internal_cache_short) + ">" : item.getName());

                if (null != item) {
                    title.setCompoundDrawablesWithIntrinsicBounds(item.getType().markerId, 0, 0, 0);
                    detail.setText(item.getGeocode());
                } else {
                    detail.setText(context.getString(R.string.create_internal_cache));
                }
                return view;
            }
        };

        final AlertDialog dialog = new AlertDialog.Builder(context)
            .setTitle(R.string.add_target_to)
            .setAdapter(adapter, (dialog1, which) -> {
                final String geocode;
                if (which == 0) {
                    // create new UDC
                    geocode = InternalConnector.createCache(context, name, null, new Geopoint(latitude, longitude), StoredList.STANDARD_LIST_ID);
                } else {
                    // add to an existing UDC
                    geocode = items.get(which).getGeocode();
                    final Geocache cache = DataStore.loadCache(geocode, LoadFlags.LOAD_CACHE_OR_DB);
                    final Waypoint newWaypoint = new Waypoint(null != name ? name : Waypoint.getDefaultWaypointName(cache, WaypointType.WAYPOINT), WaypointType.WAYPOINT, true);
                    newWaypoint.setCoords(new Geopoint(latitude, longitude));
                    newWaypoint.setGeocode(geocode);
                    cache.addOrChangeWaypoint(newWaypoint, true);
                }
                CacheDetailActivity.startActivity(context, geocode, which != 0);
                context.finish();
            })
            .create();
        dialog.setCanceledOnTouchOutside(true);
        dialog.show();
    }

}
