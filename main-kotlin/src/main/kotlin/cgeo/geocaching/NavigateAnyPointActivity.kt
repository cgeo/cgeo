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

package cgeo.geocaching

import cgeo.geocaching.activity.AbstractActionBarActivity
import cgeo.geocaching.connector.internal.InternalConnector
import cgeo.geocaching.enumerations.LoadFlags
import cgeo.geocaching.enumerations.WaypointType
import cgeo.geocaching.list.StoredList
import cgeo.geocaching.location.Geopoint
import cgeo.geocaching.models.Geocache
import cgeo.geocaching.models.Waypoint
import cgeo.geocaching.storage.DataStore
import cgeo.geocaching.ui.GeoItemSelectorUtils
import cgeo.geocaching.ui.dialog.Dialogs
import cgeo.geocaching.utils.Log

import android.annotation.SuppressLint
import android.app.Activity
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ListAdapter
import android.widget.TextView

import androidx.annotation.NonNull
import androidx.annotation.Nullable
import androidx.appcompat.app.AlertDialog

import java.util.ArrayList

import de.k3b.geo.api.GeoPointDto
import de.k3b.geo.api.IGeoPointInfo
import de.k3b.geo.io.GeoUri

class NavigateAnyPointActivity : AbstractActionBarActivity() {
    override     public Unit onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState)
        setTheme(R.style.cgeo)

        InternalConnector.assertHistoryCacheExists(this)

        // check if "geo" action is requested
        Boolean geoActionRequested = false
        if (getIntent() != null) {
            val parser: GeoUri = GeoUri(GeoUri.OPT_DEFAULT)
            val geo: IGeoPointInfo = parser.fromUri(getIntent().getDataString())
            if (geo != null && !GeoPointDto.isEmpty(geo)) {
                Log.i("Received a geo intent: lat=" + geo.getLatitude()
                        + ", lon=" + geo.getLongitude() + ", name=" + geo.getName()
                        + " form " + getIntent().getDataString())
                selectTargetType(this, geo.getLatitude(), geo.getLongitude(), geo.getName())
                geoActionRequested = true
            }
        }
        if (!geoActionRequested) {
            CacheDetailActivity.startActivity(this, InternalConnector.GEOCODE_HISTORY_CACHE, true)
            finish()
        }
    }

    private static Unit selectTargetType(final Activity context, final Double latitude, final Double longitude, final String name) {

        val items: ArrayList<Geocache> = ArrayList<>()
        items.add(null)
        items.add(DataStore.loadCache(InternalConnector.GEOCODE_HISTORY_CACHE, LoadFlags.LOAD_CACHE_OR_DB))
        items.addAll(DataStore.loadUDCSorted())

        val adapter: ListAdapter = ArrayAdapter<Geocache>(context, R.layout.cacheslist_item_select, items) {
            @SuppressLint("SetTextI18n")
            override             public View getView(final Int position, final View convertView, final ViewGroup parent) {
                val cache: Geocache = getItem(position)

                if (cache == null) { // special case: we want to display a "<New cache>" item on top
                    val view: View = GeoItemSelectorUtils.getOrCreateView(context, convertView, parent)

                    val title: TextView = view.findViewById(R.id.text)
                    title.setText("<" + context.getString(R.string.create_internal_cache_short) + ">")

                    val info: TextView = view.findViewById(R.id.info)
                    info.setText(context.getString(R.string.create_internal_cache))

                    return view
                }

                return GeoItemSelectorUtils.createGeocacheItemView(context, cache,
                        GeoItemSelectorUtils.getOrCreateView(context, convertView, parent))
            }
        }

        val dialog: AlertDialog = Dialogs.newBuilder(context)
                .setTitle(R.string.add_target_to)
                .setAdapter(adapter, (dialog1, which) -> {
                    final String geocode
                    if (which == 0) {
                        // create UDC
                        geocode = InternalConnector.createCache(context, name, null, 0, Geopoint(latitude, longitude), StoredList.STANDARD_LIST_ID)
                    } else {
                        // add to an existing UDC
                        geocode = items.get(which).getGeocode()
                        val cache: Geocache = DataStore.loadCache(geocode, LoadFlags.LOAD_CACHE_OR_DB)
                        assert cache != null
                        val newWaypoint: Waypoint = Waypoint(null != name ? name : Waypoint.getDefaultWaypointName(cache, WaypointType.WAYPOINT), WaypointType.WAYPOINT, true)
                        newWaypoint.setCoords(Geopoint(latitude, longitude))
                        newWaypoint.setGeocode(geocode)
                        cache.addOrChangeWaypoint(newWaypoint, true)
                    }
                    CacheDetailActivity.startActivity(context, geocode, which != 0)
                    context.finish()
                })
                .create()
        dialog.setCanceledOnTouchOutside(true)
        dialog.show()
    }

}
