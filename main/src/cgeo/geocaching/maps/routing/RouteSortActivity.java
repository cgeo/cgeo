package cgeo.geocaching.maps.routing;

import cgeo.geocaching.R;
import cgeo.geocaching.activity.AbstractActivity;
import cgeo.geocaching.enumerations.CacheListType;
import cgeo.geocaching.enumerations.CoordinatesType;
import cgeo.geocaching.enumerations.LoadFlags;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.models.IWaypoint;
import cgeo.geocaching.models.Waypoint;
import cgeo.geocaching.storage.DataStore;
import cgeo.geocaching.utils.AndroidRxUtils;
import cgeo.geocaching.utils.Formatter;
import cgeo.geocaching.utils.MapMarkerUtils;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collections;

import io.reactivex.rxjava3.schedulers.Schedulers;

public class RouteSortActivity extends AbstractActivity {

    private ArrayAdapter<RouteItem> routeItemAdapter;
    private ArrayList<RouteItem> routeItems;
    private boolean changed = false;

    @Override
    public void onCreate(@Nullable final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTheme();
        setTitle(getString(R.string.map_sort_individual_route));

        routeItems = DataStore.loadRoute();

        routeItemAdapter = new ArrayAdapter<RouteItem>(this, 0, routeItems) {
            @SuppressLint("SetTextI18n")
            public View getView(final int position, final View convertView, @NonNull final ViewGroup parent) {
                View v = convertView;
                if (null == convertView) {
                    v = getLayoutInflater().inflate(R.layout.twotexts_twobuttons_item, null, false);
                    ((ImageButton) v.findViewById(R.id.button_left)).setImageResource(R.drawable.ic_menu_up);
                    ((ImageButton) v.findViewById(R.id.button_right)).setImageResource(R.drawable.ic_menu_down);
                }
                final RouteItem routeItem = routeItems.get(position);
                final IWaypoint data = routeItem.getType() == CoordinatesType.CACHE ? DataStore.loadCache(routeItem.getGeocode(), LoadFlags.LOAD_CACHE_OR_DB) : DataStore.loadWaypoint(routeItem.getId());

                final TextView title = v.findViewById(R.id.title);
                title.setText(data.getName());

                final TextView detail = v.findViewById(R.id.detail);
                if (routeItem.getType() == CoordinatesType.CACHE) {
                    assert data instanceof Geocache;
                    detail.setText(Formatter.formatCacheInfoLong((Geocache) data));
                    title.setCompoundDrawablesWithIntrinsicBounds(MapMarkerUtils.getCacheMarker(res, (Geocache) data, CacheListType.OFFLINE).getDrawable(), null, null, null);
                } else {
                    assert data instanceof Waypoint;
                    final Geocache cache = DataStore.loadCache(data.getGeocode(), LoadFlags.LOAD_CACHE_OR_DB);
                    detail.setText(data.getGeocode() + Formatter.SEPARATOR + cache.getName());
                    title.setCompoundDrawablesWithIntrinsicBounds(data.getWaypointType().markerId, 0, 0, 0);
                }

                final View buttonUp = v.findViewById(R.id.button_left);
                buttonUp.setVisibility(position > 0 ? View.VISIBLE : View.INVISIBLE);
                buttonUp.setOnClickListener(vUp -> swap(position, position - 1));

                final View buttonDown = v.findViewById(R.id.button_right);
                buttonDown.setVisibility(position < routeItems.size() - 1 ? View.VISIBLE : View.INVISIBLE);
                buttonDown.setOnClickListener(vDown -> swap(position, position + 1));

                return v;
            }
        };
        final ListView list = new ListView(this);
        setContentView(list);
        list.setAdapter(routeItemAdapter);
    }

    private void swap(final int position1, final int position2) {
        Collections.swap(routeItems, position1, position2);
        routeItemAdapter.notifyDataSetChanged();
        changed = true;
        invalidateOptionsMenu();
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        getMenuInflater().inflate(R.menu.route_sort, menu);
        menu.findItem(R.id.save_sorted_route).setVisible(changed);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        if (item.getItemId() == R.id.save_sorted_route) {
            AndroidRxUtils.andThenOnUi(Schedulers.io(), () -> DataStore.saveRoute(routeItems), () -> {
                changed = false;
                invalidateOptionsMenu();
                Toast.makeText(this, R.string.sorted_route_saved, Toast.LENGTH_SHORT).show();
            });
            return true;
        }
        return false;
    }

}
