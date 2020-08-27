package cgeo.geocaching.maps.routing;

import cgeo.geocaching.CacheDetailActivity;
import cgeo.geocaching.R;
import cgeo.geocaching.activity.AbstractActivity;
import cgeo.geocaching.enumerations.CacheListType;
import cgeo.geocaching.enumerations.LoadFlags;
import cgeo.geocaching.location.GeopointFormatter;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.models.IWaypoint;
import cgeo.geocaching.models.RouteItem;
import cgeo.geocaching.models.Waypoint;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.storage.DataStore;
import cgeo.geocaching.utils.AndroidRxUtils;
import cgeo.geocaching.utils.Formatter;
import cgeo.geocaching.utils.MapMarkerUtils;
import static cgeo.geocaching.location.GeopointFormatter.Format.LAT_LON_DECMINUTE;

import android.annotation.SuppressLint;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collections;

import com.mobeta.android.dslv.DragSortController;
import com.mobeta.android.dslv.DragSortListView;
import com.mobeta.android.dslv.SimpleFloatViewManager;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class RouteSortActivity extends AbstractActivity {

    private ArrayAdapter<RouteItem> routeItemAdapter;
    private ArrayList<RouteItem> routeItems;
    private DragSortListView listView;
    private boolean changed = false;
    private int lastActivatedPosition = -1;

    private DragSortListView.DropListener onDrop = new DragSortListView.DropListener() {
        @Override
        public void drop(final int from, final int to) {
            if (from != to) {
                swap(from, to);
            }
        }
    };

    @Override
    public void onCreate(@Nullable final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTheme();
        setTitle(getString(R.string.map_sort_individual_route));

        routeItems = DataStore.loadIndividualRoute();
        listView = new DragSortListView(this, null);
        final DragSortController controller = new DragSortController(listView);

        routeItemAdapter = new ArrayAdapter<RouteItem>(this, 0, routeItems) {
            @SuppressLint("SetTextI18n")
            @NonNull
            public View getView(final int position, final View convertView, @NonNull final ViewGroup parent) {
                View v = convertView;
                if (null == convertView) {
                    v = getLayoutInflater().inflate(R.layout.twotexts_button_image_item, null, false);
                    ((ImageButton) v.findViewById(R.id.button_left)).setImageResource(R.drawable.ic_menu_delete);
                    ((ImageView) v.findViewById(R.id.img_right)).setImageResource(Settings.isLightSkin() ? R.drawable.ic_menu_reorder_black : R.drawable.ic_menu_reorder);
                }

                final RouteItem routeItem = routeItems.get(position);
                final boolean cacheOrWaypointType = routeItem.getType() == RouteItem.RouteItemType.GEOCACHE || routeItem.getType() == RouteItem.RouteItemType.WAYPOINT;
                final IWaypoint data = cacheOrWaypointType ? routeItem.getType() == RouteItem.RouteItemType.GEOCACHE ? DataStore.loadCache(routeItem.getGeocode(), LoadFlags.LOAD_CACHE_OR_DB) : DataStore.loadWaypoint(routeItem.getWaypointId()) : null;

                final TextView title = v.findViewById(R.id.title);
                final TextView detail = v.findViewById(R.id.detail);
                if (null == data && cacheOrWaypointType) {
                    title.setText(routeItem.getGeocode());
                    detail.setText(R.string.route_item_not_yet_loaded);
                } else {
                    title.setText(null == data ? "" : data.getName());
                    switch (routeItem.getType()) {
                        case GEOCACHE:
                            assert data instanceof Geocache;
                            detail.setText(Formatter.formatCacheInfoLong((Geocache) data));
                            title.setCompoundDrawablesWithIntrinsicBounds(MapMarkerUtils.getCacheMarker(res, (Geocache) data, CacheListType.OFFLINE).getDrawable(), null, null, null);
                            break;
                        case WAYPOINT:
                            assert data instanceof Waypoint;
                            final Geocache cache = DataStore.loadCache(data.getGeocode(), LoadFlags.LOAD_CACHE_OR_DB);
                            detail.setText(data.getGeocode() + Formatter.SEPARATOR + cache.getName());
                            title.setCompoundDrawablesWithIntrinsicBounds(data.getWaypointType().markerId, 0, 0, 0);
                            break;
                        case COORDS:
                            // title.setText("Coordinates");
                            detail.setText(GeopointFormatter.format(LAT_LON_DECMINUTE, routeItem.getPoint()));
                            title.setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0, 0, 0);
                            break;
                        default:
                            throw new IllegalStateException("unknow RouteItemType in RouteSortActivity");
                    }
                    title.setOnClickListener(v1 -> CacheDetailActivity.startActivity(listView.getContext(), data.getGeocode(), data.getName()));
                    detail.setOnClickListener(v1 -> CacheDetailActivity.startActivity(listView.getContext(), data.getGeocode(), data.getName()));
                }

                final View buttonDelete = v.findViewById(R.id.button_left);
                buttonDelete.setVisibility(View.VISIBLE);
                buttonDelete.setOnClickListener(vUp -> delete(position));

                final ImageView imgReorder = v.findViewById(R.id.img_right);
                imgReorder.setVisibility(View.VISIBLE);
                imgReorder.setOnTouchListener(controller);

                return v;
            }
        };

        setContentView(listView);
        listView.setAdapter(routeItemAdapter);
        listView.setDropListener(onDrop);

        controller.setDragHandleId(R.id.img_right);
        controller.setRemoveEnabled(false);
        controller.setSortEnabled(true);
        controller.setDragInitMode(1);
        listView.setFloatViewManager(controller);
        listView.setOnTouchListener(controller);
        listView.setDragEnabled(true);

        if (Settings.isLightSkin()) {
        final SimpleFloatViewManager simpleFloatViewManager = new SimpleFloatViewManager(listView);
        simpleFloatViewManager.setBackgroundColor(Color.GRAY);
        listView.setFloatViewManager(simpleFloatViewManager);
        }

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
            AndroidRxUtils.andThenOnUi(Schedulers.io(), () -> DataStore.saveIndividualRoute(routeItems), () -> {
                changed = false;
                invalidateOptionsMenu();
                Toast.makeText(this, R.string.sorted_route_saved, Toast.LENGTH_SHORT).show();
            });
            return true;
        }
        return false;
    }

}
