package cgeo.geocaching;

import cgeo.geocaching.activity.AbstractActionBarActivity;
import cgeo.geocaching.connector.internal.InternalConnector;
import cgeo.geocaching.enumerations.LoadFlags;
import cgeo.geocaching.enumerations.WaypointType;
import cgeo.geocaching.list.StoredList;
import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.models.Waypoint;
import cgeo.geocaching.storage.DataStore;
import cgeo.geocaching.ui.GeoItemSelectorUtils;
import cgeo.geocaching.ui.dialog.Dialogs;
import cgeo.geocaching.utils.Log;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;

import java.util.ArrayList;

import de.k3b.geo.api.GeoPointDto;
import de.k3b.geo.api.IGeoPointInfo;
import de.k3b.geo.io.GeoUri;

public class NavigateAnyPointActivity extends AbstractActionBarActivity {
    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTheme(R.style.cgeo);

        InternalConnector.assertHistoryCacheExists(this);

        // check if "geo" action is requested
        boolean geoActionRequested = false;
        if (getIntent() != null) {
            final GeoUri parser = new GeoUri(GeoUri.OPT_DEFAULT);
            final IGeoPointInfo geo = parser.fromUri(getIntent().getDataString());
            if (geo != null && !GeoPointDto.isEmpty(geo)) {
                Log.i("Received a geo intent: lat=" + geo.getLatitude()
                        + ", lon=" + geo.getLongitude() + ", name=" + geo.getName()
                        + " form " + getIntent().getDataString());
                selectTargetType(this, geo.getLatitude(), geo.getLongitude(), geo.getName());
                geoActionRequested = true;
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

        final ListAdapter adapter = new ArrayAdapter<Geocache>(context, R.layout.cacheslist_item_select, items) {
            @SuppressLint("SetTextI18n")
            @NonNull
            @Override
            public View getView(final int position, final View convertView, @NonNull final ViewGroup parent) {
                final Geocache cache = getItem(position);

                if (cache == null) { // special case: we want to display a "<New cache>" item on top
                    final View view = GeoItemSelectorUtils.getOrCreateView(context, convertView, parent);

                    final TextView title = (TextView) view.findViewById(R.id.text);
                    title.setText("<" + context.getString(R.string.create_internal_cache_short) + ">");

                    final TextView info = (TextView) view.findViewById(R.id.info);
                    info.setText(context.getString(R.string.create_internal_cache));

                    return view;
                }

                return GeoItemSelectorUtils.createGeocacheItemView(context, cache,
                        GeoItemSelectorUtils.getOrCreateView(context, convertView, parent));
            }
        };

        final AlertDialog dialog = Dialogs.newBuilder(context)
                .setTitle(R.string.add_target_to)
                .setAdapter(adapter, (dialog1, which) -> {
                    final String geocode;
                    if (which == 0) {
                        // create new UDC
                        geocode = InternalConnector.createCache(context, name, null, 0, new Geopoint(latitude, longitude), StoredList.STANDARD_LIST_ID);
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
