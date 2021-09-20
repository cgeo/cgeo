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
import cgeo.geocaching.ui.dialog.Dialogs;
import cgeo.geocaching.utils.Log;

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

import de.k3b.geo.api.GeoPointDto;
import de.k3b.geo.api.IGeoPointInfo;
import de.k3b.geo.io.GeoUri;

import java.util.ArrayList;

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
                    detail.setText(item.getShortGeocode());
                } else {
                    detail.setText(context.getString(R.string.create_internal_cache));
                }
                return view;
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
