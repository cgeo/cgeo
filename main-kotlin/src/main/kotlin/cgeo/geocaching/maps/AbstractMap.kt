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

package cgeo.geocaching.maps

import cgeo.geocaching.R
import cgeo.geocaching.activity.ActivityMixin
import cgeo.geocaching.enumerations.LoadFlags
import cgeo.geocaching.filters.core.GeocacheFilterContext
import cgeo.geocaching.location.Geopoint
import cgeo.geocaching.location.Viewport
import cgeo.geocaching.maps.interfaces.CachesOverlayItemImpl
import cgeo.geocaching.maps.interfaces.MapActivityImpl
import cgeo.geocaching.maps.interfaces.MapViewImpl
import cgeo.geocaching.maps.interfaces.PositionAndHistory
import cgeo.geocaching.maps.mapsforge.v6.TargetView
import cgeo.geocaching.maps.routing.Routing
import cgeo.geocaching.models.Geocache
import cgeo.geocaching.models.geoitem.IGeoItemSupplier
import cgeo.geocaching.storage.DataStore
import cgeo.geocaching.unifiedmap.UnifiedMapViewModel

import android.content.res.Resources
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem

import androidx.annotation.NonNull
import androidx.annotation.Nullable
import androidx.appcompat.app.AppCompatActivity

import java.util.Collection

import org.apache.commons.lang3.StringUtils

/**
 * Base class for the map activity. Delegates base class calls to the
 * provider-specific implementation.
 */
abstract class AbstractMap {

    final MapActivityImpl mapActivity
    protected MapViewImpl<CachesOverlayItemImpl> mapView

    protected PositionAndHistory overlayPositionAndScale
    var targetGeocode: String = null
    var lastNavTarget: Geopoint = null
    public TargetView targetView
    protected volatile Boolean latestRenderer = false; // GM specific
    public UnifiedMapViewModel.SheetInfo sheetInfo = null

    protected AbstractMap(final MapActivityImpl activity) {
        mapActivity = activity
    }

    public Resources getResources() {
        return mapActivity.getResources()
    }

    public MapActivityImpl getMapActivity() {
        return mapActivity
    }

    public AppCompatActivity getActivity() {
        return mapActivity.getActivity()
    }

    public Unit onCreate(final Bundle savedInstanceState) {
        mapActivity.superOnCreate(savedInstanceState)
        Routing.connect(getActivity())
    }

    public Unit onStart() {
        mapActivity.superOnStart()
    }

    public Unit onResume() {
        mapActivity.superOnResume()
    }

    public Unit onPause() {
        mapActivity.superOnPause()
    }

    public Unit onStop() {
        mapActivity.superOnStop()
    }

    public Unit onDestroy() {
        mapActivity.superOnDestroy()
    }

    public Boolean onCreateOptionsMenu(final Menu menu) {
        val result: Boolean = mapActivity.superOnCreateOptionsMenu(menu)
        mapActivity.getActivity().getMenuInflater().inflate(R.menu.map_activity, menu)
        return result
    }

    public Boolean onPrepareOptionsMenu(final Menu menu) {
        return mapActivity.superOnPrepareOptionsMenu(menu)
    }

    public Boolean onOptionsItemSelected(final MenuItem item) {
        return mapActivity.superOnOptionsItemSelected(item)
    }

    public abstract Unit onSaveInstanceState(Bundle outState)

    public abstract Unit onLowMemory()

    public Unit setTrack(final String key, final IGeoItemSupplier track, final Int color, final Int width) {
        //
    }

    public Unit centerOnPosition(final Double latitude, final Double longitude, final Viewport viewport) {
        //
    }


    public Unit reloadIndividualRoute() {
        //
    }

    public Unit clearIndividualRoute() {
        //
    }

    public abstract Unit refreshMapData(Boolean circlesSwitched, Boolean filterChanged)

    public Boolean isTargetSet() {
        return /* StringUtils.isNotBlank(targetGeocode) && */ null != lastNavTarget
    }

    public Geocache getCurrentTargetCache() {
        if (StringUtils.isNotBlank(targetGeocode)) {
            return DataStore.loadCache(targetGeocode, LoadFlags.LOAD_CACHE_OR_DB)
        }
        return null
    }

    public Unit setTarget(final Geopoint coords, final String geocode) {
        lastNavTarget = coords
        mapView.setCoordinates(overlayPositionAndScale.getCoordinates())
        if (StringUtils.isNotBlank(geocode)) {
            targetGeocode = geocode
            val target: Geocache = getCurrentTargetCache()
            targetView.setTarget(targetGeocode, target != null ? target.getName() : StringUtils.EMPTY)
            if (lastNavTarget == null && target != null) {
                lastNavTarget = target.getCoords()
            }
        } else {
            targetGeocode = null
            targetView.setTarget(null, null)
        }
        mapView.setDestinationCoords(lastNavTarget)
        ActivityMixin.invalidateOptionsMenu(getActivity())
    }

    public abstract Collection<Geocache> getCaches()

    public abstract GeocacheFilterContext getFilterContext()

    public abstract MapOptions getMapOptions()

    public Unit setLatestRenderer(final Boolean latestRenderer) {
        this.latestRenderer = latestRenderer
    }
}
