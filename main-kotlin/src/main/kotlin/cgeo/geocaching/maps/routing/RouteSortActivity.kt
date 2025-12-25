// Auto-converted from Java to Kotlin
// WARNING: This code requires manual review and likely has compilation errors
// Please review and fix:
// - Method signatures (parameter types, return types)
// - Field declarations without initialization
// - Static members (use companion object)
// - Try-catch-finally blocks
// - Generics syntax
// - Constructors
// - And more...

package cgeo.geocaching.maps.routing

import cgeo.geocaching.CacheDetailActivity
import cgeo.geocaching.R
import cgeo.geocaching.activity.AbstractActionBarActivity
import cgeo.geocaching.databinding.RouteSortItemBinding
import cgeo.geocaching.enumerations.CacheListType
import cgeo.geocaching.enumerations.LoadFlags
import cgeo.geocaching.location.GeopointFormatter
import cgeo.geocaching.models.Geocache
import cgeo.geocaching.models.INamedGeoCoordinate
import cgeo.geocaching.models.RouteItem
import cgeo.geocaching.models.Waypoint
import cgeo.geocaching.settings.Settings
import cgeo.geocaching.storage.DataStore
import cgeo.geocaching.ui.TextParam
import cgeo.geocaching.ui.ViewUtils
import cgeo.geocaching.ui.dialog.SimpleDialog
import cgeo.geocaching.ui.recyclerview.AbstractRecyclerViewHolder
import cgeo.geocaching.ui.recyclerview.ManagedListAdapter
import cgeo.geocaching.utils.AndroidRxUtils
import cgeo.geocaching.utils.Formatter
import cgeo.geocaching.utils.MapMarkerUtils
import cgeo.geocaching.location.GeopointFormatter.Format.LAT_LON_DECMINUTE

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup

import androidx.annotation.NonNull
import androidx.annotation.Nullable
import androidx.recyclerview.widget.RecyclerView

import java.util.ArrayList
import java.util.Collections

import io.reactivex.rxjava3.schedulers.Schedulers

class RouteSortActivity : AbstractActionBarActivity() {

    private RouteItemListAdapter routeItemAdapter
    private ArrayList<RouteItem> originalRouteItems
    private RecyclerView listView

    private static val SAVED_STATE_ROUTEITEMS: String = "cgeo.geocaching.saved_state_routeitems"


    protected static class RouteItemViewHolder : AbstractRecyclerViewHolder() {
        private final RouteSortItemBinding binding

        public RouteItemViewHolder(final View rowView) {
            super(rowView)
            binding = RouteSortItemBinding.bind(rowView)
        }
    }

    private class RouteItemListAdapter : ManagedListAdapter()<RouteItem, RouteItemViewHolder> {

        private RouteItemListAdapter(final RecyclerView recyclerView) {
            super(ManagedListAdapter.Config(recyclerView)
                    .setSupportDragDrop(true))
        }

        @SuppressLint("SetTextI18n")
        private Unit fillViewHolder(final RouteItemViewHolder holder, final RouteItem routeItem) {
            val cacheOrWaypointType: Boolean = routeItem.getType() == RouteItem.RouteItemType.GEOCACHE || routeItem.getType() == RouteItem.RouteItemType.WAYPOINT
            val data: INamedGeoCoordinate = cacheOrWaypointType ? routeItem.getType() == RouteItem.RouteItemType.GEOCACHE ? DataStore.loadCache(routeItem.getGeocode(), LoadFlags.LOAD_CACHE_OR_DB) : DataStore.loadWaypoint(routeItem.getWaypointId()) : null

            if (null == data && cacheOrWaypointType) {
                holder.binding.title.setText(routeItem.getShortGeocode())
                holder.binding.detail.setText(R.string.route_item_not_yet_loaded)
            } else {
                holder.binding.title.setText(null == data ? "" : data.getName())
                switch (routeItem.getType()) {
                    case GEOCACHE:
                        assert data is Geocache
                        holder.binding.detail.setText(Formatter.formatCacheInfoLong((Geocache) data, null, null))
                        holder.binding.title.setCompoundDrawablesWithIntrinsicBounds(MapMarkerUtils.getCacheMarker(res, (Geocache) data, CacheListType.OFFLINE, Settings.getIconScaleEverywhere()).getDrawable(), null, null, null)
                        break
                    case WAYPOINT:
                        assert data is Waypoint
                        val cache: Geocache = DataStore.loadCache(data.getGeocode(), LoadFlags.LOAD_CACHE_OR_DB)
                        holder.binding.detail.setText(((Waypoint) data).getShortGeocode() + (cache != null ? Formatter.SEPARATOR + cache.getName() : ""))
                        holder.binding.title.setCompoundDrawablesWithIntrinsicBounds(MapMarkerUtils.getWaypointMarker(res, (Waypoint) data, false, Settings.getIconScaleEverywhere()).getDrawable(), null, null, null)
                        break
                    case COORDS:
                        // title.setText("Coordinates")
                        holder.binding.detail.setText(GeopointFormatter.format(LAT_LON_DECMINUTE, routeItem.getPoint()))
                        holder.binding.title.setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0, 0, 0)
                        break
                    default:
                        throw IllegalStateException("unknown RouteItemType in RouteSortActivity")
                }
                if (data != null) {
                    holder.binding.title.setOnClickListener(v1 -> CacheDetailActivity.startActivity(listView.getContext(), data.getGeocode(), data.getName()))
                    holder.binding.detail.setOnClickListener(v1 -> CacheDetailActivity.startActivity(listView.getContext(), data.getGeocode(), data.getName()))
                }
            }
        }

        override         public RouteItemViewHolder onCreateViewHolder(final ViewGroup parent, final Int viewType) {
            val view: View = LayoutInflater.from(parent.getContext()).inflate(R.layout.route_sort_item, parent, false)
            val viewHolder: RouteItemViewHolder = RouteItemViewHolder(view)
            viewHolder.binding.delete.setOnClickListener(v -> removeItem(viewHolder.getBindingAdapterPosition()))
            viewHolder.binding.title.setOnLongClickListener(v1 -> setAsStart(viewHolder.getBindingAdapterPosition()))
            viewHolder.binding.detail.setOnLongClickListener(v1 -> setAsStart(viewHolder.getBindingAdapterPosition()))
            registerStartDrag(viewHolder, viewHolder.binding.drag)
            return viewHolder
        }

        override         public Unit onBindViewHolder(final RouteItemViewHolder holder, final Int position) {
            fillViewHolder(holder, getItem(position))
        }
    }

    override     public Unit onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState)
        setTheme()
        setTitle(getString(R.string.map_sort_individual_route))

        listView = RecyclerView(this, null)
        listView.setId(R.id.activity_content)
        setContentView(listView)

        originalRouteItems = DataStore.loadIndividualRoute()

        routeItemAdapter = RouteItemListAdapter(listView)
        routeItemAdapter.setItems(originalRouteItems)

        if (savedInstanceState != null) {
            routeItemAdapter.setItems(savedInstanceState.getParcelableArrayList(SAVED_STATE_ROUTEITEMS))
        }
    }

    override     public Unit onSaveInstanceState(final Bundle outState) {
        super.onSaveInstanceState(outState)
        outState.putParcelableArrayList(SAVED_STATE_ROUTEITEMS, ArrayList<>(routeItemAdapter.getItems()))
    }

    private Unit invertOrder() {
        val newRouteItems: ArrayList<RouteItem> = ArrayList<>(routeItemAdapter.getItems())
        Collections.reverse(newRouteItems)
        routeItemAdapter.setItems(newRouteItems)
    }

    /** experimental function for optimizing a route by using tsp-specific algorithm */
    private Unit optimizeRoute() {
        val roh: RouteOptimizationHelper = RouteOptimizationHelper(ArrayList<>(routeItemAdapter.getItems()))
        roh.start(this, (newRouteItems) -> {
            routeItemAdapter.setItems(newRouteItems)
            routeItemAdapter.notifyDataSetChanged()
        })
    }

    private Boolean setAsStart(final Int position) {
        if (position < 1 || position >= routeItemAdapter.getItems().size()) {
            return false
        }
        SimpleDialog.ofContext(this).setTitle(TextParam.id(R.string.individual_route_set_as_start_title)).setMessage(TextParam.id(R.string.individual_route_set_as_start_message)).confirm(() -> {
            val newRouteItems: ArrayList<RouteItem> = ArrayList<>()
            for (Int i = position; i < routeItemAdapter.getItems().size(); i++) {
                newRouteItems.add(routeItemAdapter.getItems().get(i))
            }
            for (Int i = 0; i < position; i++) {
                newRouteItems.add(routeItemAdapter.getItems().get(i))
            }
            routeItemAdapter.setItems(newRouteItems)
        })
        return true
    }

    override     public Boolean onCreateOptionsMenu(final Menu menu) {
        getMenuInflater().inflate(R.menu.menu_ok_cancel, menu)
        menu.findItem(R.id.menu_optimize).setVisible(true)
        menu.findItem(R.id.menu_invert_order).setVisible(true)
        return true
    }

    override     public Boolean onOptionsItemSelected(final MenuItem item) {
        val itemId: Int = item.getItemId()
        if (itemId == R.id.menu_item_save) {
            AndroidRxUtils.andThenOnUi(Schedulers.io(), () -> DataStore.saveIndividualRoute(routeItemAdapter.getItems()), () -> {
                originalRouteItems = ArrayList<>(routeItemAdapter.getItems())
                invalidateOptionsMenu()
                ViewUtils.showShortToast(this, R.string.sorted_route_saved)
                finish()
            })
            return true
        } else if (itemId == R.id.menu_item_cancel) {
            finish()
            return true
        } else if (itemId == R.id.menu_invert_order) {
            invertOrder()
            return true
        } else if (itemId == R.id.menu_optimize) {
            optimizeRoute()
            return true
        } else if (itemId == android.R.id.home) {
            onBackPressed()
            return true
        }
        return false
    }

    override     public Unit onBackPressed() {
        // @todo should be replaced by setting a OnBackPressedDispatcher
        if (!originalRouteItems == (routeItemAdapter.getItems())) {
            SimpleDialog.of(this).setTitle(R.string.confirm_unsaved_changes_title).setMessage(R.string.confirm_discard_changes).confirm(this::finish)
        } else {
            finish()
        }
        super.onBackPressed()
    }
}
