package cgeo.geocaching.maps.routing;

import cgeo.geocaching.CacheDetailActivity;
import cgeo.geocaching.R;
import cgeo.geocaching.activity.AbstractActionBarActivity;
import cgeo.geocaching.databinding.RouteSortItemBinding;
import cgeo.geocaching.enumerations.CacheListType;
import cgeo.geocaching.enumerations.LoadFlags;
import cgeo.geocaching.location.GeopointFormatter;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.models.IWaypoint;
import cgeo.geocaching.models.RouteItem;
import cgeo.geocaching.models.Waypoint;
import cgeo.geocaching.storage.DataStore;
import cgeo.geocaching.ui.TextParam;
import cgeo.geocaching.ui.dialog.SimpleDialog;
import cgeo.geocaching.ui.recyclerview.AbstractRecyclerViewHolder;
import cgeo.geocaching.ui.recyclerview.ManagedListAdapter;
import cgeo.geocaching.utils.AndroidRxUtils;
import cgeo.geocaching.utils.Formatter;
import cgeo.geocaching.utils.MapMarkerUtils;
import static cgeo.geocaching.location.GeopointFormatter.Format.LAT_LON_DECMINUTE;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Collections;

import io.reactivex.rxjava3.schedulers.Schedulers;

public class RouteSortActivity extends AbstractActionBarActivity {

    private RouteItemListAdapter routeItemAdapter;
    private ArrayList<RouteItem> originalRouteItems;
    private RecyclerView listView;

    private static final String SAVED_STATE_ROUTEITEMS = "cgeo.geocaching.saved_state_routeitems";


    protected static class RouteItemViewHolder extends AbstractRecyclerViewHolder {
        private final RouteSortItemBinding binding;

        public RouteItemViewHolder(final View rowView) {
            super(rowView);
            binding = RouteSortItemBinding.bind(rowView);
        }
    }

    private final class RouteItemListAdapter extends ManagedListAdapter<RouteItem, RouteItemViewHolder> {

        private RouteItemListAdapter(final RecyclerView recyclerView) {
            super(new ManagedListAdapter.Config(recyclerView)
                    .setSupportDragDrop(true));
        }

        @SuppressLint("SetTextI18n")
        private void fillViewHolder(final RouteItemViewHolder holder, final RouteItem routeItem) {
            final boolean cacheOrWaypointType = routeItem.getType() == RouteItem.RouteItemType.GEOCACHE || routeItem.getType() == RouteItem.RouteItemType.WAYPOINT;
            final IWaypoint data = cacheOrWaypointType ? routeItem.getType() == RouteItem.RouteItemType.GEOCACHE ? DataStore.loadCache(routeItem.getGeocode(), LoadFlags.LOAD_CACHE_OR_DB) : DataStore.loadWaypoint(routeItem.getWaypointId()) : null;

            if (null == data && cacheOrWaypointType) {
                holder.binding.title.setText(routeItem.getShortGeocode());
                holder.binding.detail.setText(R.string.route_item_not_yet_loaded);
            } else {
                holder.binding.title.setText(null == data ? "" : data.getName());
                switch (routeItem.getType()) {
                    case GEOCACHE:
                        assert data instanceof Geocache;
                        holder.binding.detail.setText(Formatter.formatCacheInfoLong((Geocache) data, null, null));
                        holder.binding.title.setCompoundDrawablesWithIntrinsicBounds(MapMarkerUtils.getCacheMarker(res, (Geocache) data, CacheListType.OFFLINE).getDrawable(), null, null, null);
                        break;
                    case WAYPOINT:
                        assert data instanceof Waypoint;
                        final Geocache cache = DataStore.loadCache(data.getGeocode(), LoadFlags.LOAD_CACHE_OR_DB);
                        holder.binding.detail.setText(data.getGeocode() + (cache != null ? Formatter.SEPARATOR + cache.getName() : ""));
                        holder.binding.title.setCompoundDrawablesWithIntrinsicBounds(MapMarkerUtils.getWaypointMarker(res, (Waypoint) data, false).getDrawable(), null, null, null);
                        break;
                    case COORDS:
                        // title.setText("Coordinates");
                        holder.binding.detail.setText(GeopointFormatter.format(LAT_LON_DECMINUTE, routeItem.getPoint()));
                        holder.binding.title.setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0, 0, 0);
                        break;
                    default:
                        throw new IllegalStateException("unknown RouteItemType in RouteSortActivity");
                }
                if (data != null) {
                    holder.binding.title.setOnClickListener(v1 -> CacheDetailActivity.startActivity(listView.getContext(), data.getGeocode(), data.getName()));
                    holder.binding.detail.setOnClickListener(v1 -> CacheDetailActivity.startActivity(listView.getContext(), data.getGeocode(), data.getName()));
                }
            }
        }

        @NonNull
        @Override
        public RouteItemViewHolder onCreateViewHolder(@NonNull final ViewGroup parent, final int viewType) {
            final View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.route_sort_item, parent, false);
            final RouteItemViewHolder viewHolder = new RouteItemViewHolder(view);
            viewHolder.binding.delete.setOnClickListener(v -> removeItem(viewHolder.getBindingAdapterPosition()));
            viewHolder.binding.title.setOnLongClickListener(v1 -> setAsStart(viewHolder.getBindingAdapterPosition()));
            viewHolder.binding.detail.setOnLongClickListener(v1 -> setAsStart(viewHolder.getBindingAdapterPosition()));
            registerStartDrag(viewHolder, viewHolder.binding.drag);
            return viewHolder;
        }

        @Override
        public void onBindViewHolder(@NonNull final RouteItemViewHolder holder, final int position) {
            fillViewHolder(holder, getItem(position));
        }
    }

    @Override
    public void onCreate(@Nullable final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTheme();
        setTitle(getString(R.string.map_sort_individual_route));

        listView = new RecyclerView(this, null);
        setContentView(listView);

        originalRouteItems = DataStore.loadIndividualRoute();

        routeItemAdapter = new RouteItemListAdapter(listView);
        routeItemAdapter.setItems(originalRouteItems);

        if (savedInstanceState != null) {
            routeItemAdapter.setItems(savedInstanceState.getParcelableArrayList(SAVED_STATE_ROUTEITEMS));
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull final Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelableArrayList(SAVED_STATE_ROUTEITEMS, new ArrayList<>(routeItemAdapter.getItems()));
    }

    private void invertOrder() {
        final ArrayList<RouteItem> newRouteItems = new ArrayList<>(routeItemAdapter.getItems());
        Collections.reverse(newRouteItems);
        routeItemAdapter.setItems(newRouteItems);
    }

    /** experimental function for optimizing a route by using tsp-specific algorithm */
    private void optimizeRoute() {
        final RouteOptimizationHelper roh = new RouteOptimizationHelper(new ArrayList<>(routeItemAdapter.getItems()));
        roh.start(this, (newRouteItems) -> {
            routeItemAdapter.setItems(newRouteItems);
            routeItemAdapter.notifyDataSetChanged();
        });
    }

    private boolean setAsStart(final int position) {
        if (position < 1 || position >= routeItemAdapter.getItems().size()) {
            return false;
        }
        SimpleDialog.ofContext(this).setTitle(TextParam.id(R.string.individual_route_set_as_start_title)).setMessage(TextParam.id(R.string.individual_route_set_as_start_message)).confirm((d, v) -> {
            final ArrayList<RouteItem> newRouteItems = new ArrayList<>();
            for (int i = position; i < routeItemAdapter.getItems().size(); i++) {
                newRouteItems.add(routeItemAdapter.getItems().get(i));
            }
            for (int i = 0; i < position; i++) {
                newRouteItems.add(routeItemAdapter.getItems().get(i));
            }
            routeItemAdapter.setItems(newRouteItems);
        });
        return true;
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        getMenuInflater().inflate(R.menu.menu_ok_cancel, menu);
        menu.findItem(R.id.menu_optimize).setVisible(true);
        menu.findItem(R.id.menu_invert_order).setVisible(true);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        final int itemId = item.getItemId();
        if (itemId == R.id.menu_item_save) {
            AndroidRxUtils.andThenOnUi(Schedulers.io(), () -> DataStore.saveIndividualRoute(routeItemAdapter.getItems()), () -> {
                originalRouteItems = new ArrayList<>(routeItemAdapter.getItems());
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
        } else if (itemId == R.id.menu_optimize) {
            optimizeRoute();
            return true;
        } else if (itemId == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return false;
    }

    @Override
    public void onBackPressed() {
        if (!originalRouteItems.equals(routeItemAdapter.getItems())) {
            SimpleDialog.of(this).setTitle(R.string.confirm_unsaved_changes_title).setMessage(R.string.confirm_discard_changes).confirm((dialog, which) -> finish());
        } else {
            finish();
        }
    }
}
