package cgeo.geocaching.maps.routing;

import cgeo.geocaching.CacheDetailActivity;
import cgeo.geocaching.R;
import cgeo.geocaching.activity.AbstractActionBarActivity;
import cgeo.geocaching.enumerations.CacheListType;
import cgeo.geocaching.enumerations.LoadFlags;
import cgeo.geocaching.location.GeopointFormatter;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.models.IWaypoint;
import cgeo.geocaching.models.RouteItem;
import cgeo.geocaching.models.Waypoint;
import cgeo.geocaching.storage.DataStore;
import cgeo.geocaching.ui.dialog.SimpleDialog;
import cgeo.geocaching.utils.AndroidRxUtils;
import cgeo.geocaching.utils.Formatter;
import cgeo.geocaching.utils.MapMarkerUtils;
import static cgeo.geocaching.location.GeopointFormatter.Format.LAT_LON_DECMINUTE;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collections;

import com.google.android.material.button.MaterialButton;
import com.mobeta.android.dslv.DragSortController;
import com.mobeta.android.dslv.DragSortListView;
import com.mobeta.android.dslv.SimpleFloatViewManager;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class RouteSortActivity extends AbstractActionBarActivity {

    private ArrayAdapter<RouteItem> routeItemAdapter;
    private ArrayList<RouteItem> routeItems;
    private DragSortListView listView;
    private boolean changed = false;

    private final DragSortListView.DropListener onDrop = new DragSortListView.DropListener() {
        @Override
        public void drop(final int from, final int to) {
            if (from != to) {
                routeItems.add(to, routeItems.remove(from));
                routeItemAdapter.notifyDataSetChanged();
                changed = true;
                invalidateOptionsMenu();
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
                    v = getLayoutInflater().inflate(R.layout.twotexts_button_image_item, parent, false);
                }

                final RouteItem routeItem = routeItems.get(position);
                final boolean cacheOrWaypointType = routeItem.getType() == RouteItem.RouteItemType.GEOCACHE || routeItem.getType() == RouteItem.RouteItemType.WAYPOINT;
                final IWaypoint data = cacheOrWaypointType ? routeItem.getType() == RouteItem.RouteItemType.GEOCACHE ? DataStore.loadCache(routeItem.getGeocode(), LoadFlags.LOAD_CACHE_OR_DB) : DataStore.loadWaypoint(routeItem.getWaypointId()) : null;

                final TextView title = v.findViewById(R.id.title);
                final TextView detail = v.findViewById(R.id.detail);
                if (null == data && cacheOrWaypointType) {
                    title.setText(routeItem.getShortGeocode());
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
                            throw new IllegalStateException("unknown RouteItemType in RouteSortActivity");
                    }
                    title.setOnClickListener(v1 -> CacheDetailActivity.startActivity(listView.getContext(), data.getGeocode(), data.getName()));
                    detail.setOnClickListener(v1 -> CacheDetailActivity.startActivity(listView.getContext(), data.getGeocode(), data.getName()));
                }

                final MaterialButton buttonDelete = v.findViewById(R.id.button_left);
                buttonDelete.setIconResource(R.drawable.ic_menu_delete);
                buttonDelete.setVisibility(View.VISIBLE);
                buttonDelete.setOnClickListener(vUp -> delete(position));

                final ImageView imgReorder = v.findViewById(R.id.img_right);
                imgReorder.setImageResource(R.drawable.ic_menu_reorder);
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

        final SimpleFloatViewManager simpleFloatViewManager = new SimpleFloatViewManager(listView);
        simpleFloatViewManager.setBackgroundColor(getResources().getColor(R.color.colorBackgroundSelected));
        listView.setFloatViewManager(simpleFloatViewManager);
    }

    private void delete(final int position) {
        routeItems.remove(position);
        routeItemAdapter.notifyDataSetChanged();
        changed = true;
    }

    private void invertOrder() {
        Collections.reverse(routeItems);
        routeItemAdapter.notifyDataSetChanged();
        changed = true;
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        getMenuInflater().inflate(R.menu.menu_ok_cancel, menu);
        menu.findItem(R.id.menu_invert_order).setVisible(true);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        final int itemId = item.getItemId();
        if (itemId == R.id.menu_item_save) {
            AndroidRxUtils.andThenOnUi(Schedulers.io(), () -> DataStore.saveIndividualRoute(routeItems), () -> {
                changed = false;
                invalidateOptionsMenu();
                Toast.makeText(this, R.string.sorted_route_saved, Toast.LENGTH_SHORT).show();
                finish();
            });
            return true;
        } else if (itemId == R.id.menu_item_cancel) {
            finish();
            return true;
        } else if (itemId == R.id.menu_invert_order) {
            invertOrder();
            return true;
        } else if (itemId == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return false;
    }

    @Override
    public void onBackPressed() {
        if (changed) {
            SimpleDialog.of(this).setTitle(R.string.confirm_unsaved_changes_title).setMessage(R.string.confirm_discard_changes).confirm((dialog, which) -> finish());
        } else {
            finish();
        }
    }
}
