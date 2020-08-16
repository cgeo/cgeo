package cgeo.geocaching.maps.routing;

import cgeo.geocaching.CacheDetailActivity;
import cgeo.geocaching.R;
import cgeo.geocaching.activity.AbstractActivity;
import cgeo.geocaching.enumerations.CacheListType;
import cgeo.geocaching.enumerations.LoadFlags;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.models.IWaypoint;
import cgeo.geocaching.models.RouteItem;
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
    private ListView listView;
    private boolean changed = false;
    private int lastActivatedPosition = -1;

    @Override
    public void onCreate(@Nullable final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTheme();
        setTitle(getString(R.string.map_sort_individual_route));

        routeItems = DataStore.loadRoute();

        routeItemAdapter = new ArrayAdapter<RouteItem>(this, 0, routeItems) {
            @SuppressLint("SetTextI18n")
            @NonNull
            public View getView(final int position, final View convertView, @NonNull final ViewGroup parent) {
                View v = convertView;
                if (null == convertView) {
                    v = getLayoutInflater().inflate(R.layout.twotexts_twobuttons_item, null, false);
                    ((ImageButton) v.findViewById(R.id.button_left)).setImageResource(R.drawable.ic_menu_up);
                    ((ImageButton) v.findViewById(R.id.button_right)).setImageResource(R.drawable.ic_menu_down);
                }

                final RouteItem routeItem = routeItems.get(position);
                final IWaypoint data = routeItem.getType() == RouteItem.RouteItemType.GEOCACHE ? DataStore.loadCache(routeItem.getGeocode(), LoadFlags.LOAD_CACHE_OR_DB) : DataStore.loadWaypoint(routeItem.getId());

                final TextView title = v.findViewById(R.id.title);
                final TextView detail = v.findViewById(R.id.detail);
                if (null == data) {
                    title.setText(routeItem.getGeocode());
                    detail.setText(R.string.route_item_not_yet_loaded);
                } else {
                    title.setText(data.getName());
                    if (routeItem.getType() == RouteItem.RouteItemType.GEOCACHE) {
                        assert data instanceof Geocache;
                        detail.setText(Formatter.formatCacheInfoLong((Geocache) data));
                        title.setCompoundDrawablesWithIntrinsicBounds(MapMarkerUtils.getCacheMarker(res, (Geocache) data, CacheListType.OFFLINE).getDrawable(), null, null, null);
                    } else {
                        assert data instanceof Waypoint;
                        final Geocache cache = DataStore.loadCache(data.getGeocode(), LoadFlags.LOAD_CACHE_OR_DB);
                        detail.setText(data.getGeocode() + Formatter.SEPARATOR + cache.getName());
                        title.setCompoundDrawablesWithIntrinsicBounds(data.getWaypointType().markerId, 0, 0, 0);
                    }
                    title.setOnClickListener(v1 -> CacheDetailActivity.startActivity(listView.getContext(), data.getGeocode(), data.getName()));
                    detail.setOnClickListener(v1 -> CacheDetailActivity.startActivity(listView.getContext(), data.getGeocode(), data.getName()));
                }
                title.setOnLongClickListener(v1 -> delete(position));
                detail.setOnLongClickListener(v1 -> delete(position));

                final View buttonUp = v.findViewById(R.id.button_left);
                buttonUp.setVisibility(position > 0 ? View.VISIBLE : View.INVISIBLE);
                buttonUp.setOnClickListener(vUp -> swap(position, position - 1));

                final ImageButton buttonDown = v.findViewById(R.id.button_right);
                buttonDown.setVisibility(position < routeItems.size() - 1 ? View.VISIBLE : View.INVISIBLE);
                buttonDown.setOnClickListener(vDown -> swap(position, position + 1));

                return v;
            }
        };
        listView = new ListView(this);
        setContentView(listView);
        listView.setAdapter(routeItemAdapter);
    }

    private void swap(final int position1, final int position2) {
        Collections.swap(routeItems, position1, position2);
        routeItemAdapter.notifyDataSetChanged();
        changed = true;
        invalidateOptionsMenu();
    }

    private boolean delete(final int position) {
        routeItems.remove(position);
        routeItemAdapter.notifyDataSetChanged();
        changed = true;
        invalidateOptionsMenu();
        return true;
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
