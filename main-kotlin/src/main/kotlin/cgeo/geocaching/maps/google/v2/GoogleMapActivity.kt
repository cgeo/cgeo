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

package cgeo.geocaching.maps.google.v2

import cgeo.geocaching.AbstractDialogFragment
import cgeo.geocaching.Intents
import cgeo.geocaching.R
import cgeo.geocaching.activity.AbstractNavigationBarMapActivity
import cgeo.geocaching.activity.FilteredActivity
import cgeo.geocaching.filters.core.GeocacheFilter
import cgeo.geocaching.filters.gui.GeocacheFilterActivity
import cgeo.geocaching.maps.AbstractMap
import cgeo.geocaching.maps.CGeoMap
import cgeo.geocaching.maps.MapMode
import cgeo.geocaching.maps.MapUtils
import cgeo.geocaching.maps.RouteTrackUtils
import cgeo.geocaching.maps.Tracks
import cgeo.geocaching.maps.interfaces.MapActivityImpl
import cgeo.geocaching.maps.mapsforge.v6.TargetView
import cgeo.geocaching.models.Geocache
import cgeo.geocaching.settings.Settings
import cgeo.geocaching.ui.ViewUtils
import cgeo.geocaching.utils.FilterUtils
import cgeo.geocaching.utils.LifecycleAwareBroadcastReceiver
import cgeo.geocaching.utils.Log
import cgeo.geocaching.Intents.ACTION_INVALIDATE_MAPLIST
import cgeo.geocaching.filters.gui.GeocacheFilterActivity.EXTRA_FILTER_CONTEXT
import cgeo.geocaching.maps.google.v2.GoogleMapUtils.isGoogleMapsAvailable
import cgeo.geocaching.settings.Settings.MAPROTATION_AUTO_LOWPOWER
import cgeo.geocaching.settings.Settings.MAPROTATION_AUTO_PRECISE
import cgeo.geocaching.settings.Settings.MAPROTATION_MANUAL
import cgeo.geocaching.settings.Settings.MAPROTATION_OFF

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem

import androidx.annotation.NonNull
import androidx.appcompat.app.AppCompatActivity

import com.google.android.gms.maps.MapsInitializer
import com.google.android.gms.maps.OnMapsSdkInitializedCallback
import org.apache.commons.lang3.StringUtils

// super calls are handled via mapBase (mapBase.onCreate, mapBase.onSaveInstanceState, ...)
// TODO: Why is it done like that?
//       Either merge GoogleMapActivity with CGeoMap
//       or generify our map handling so that we only have one map activity at all to avoid code duplication
@SuppressLint("MissingSuperCall")
class GoogleMapActivity : AbstractNavigationBarMapActivity() : MapActivityImpl, FilteredActivity, OnMapsSdkInitializedCallback, AbstractDialogFragment.TargetUpdateReceiver {

    private static val STATE_ROUTETRACKUTILS: String = "routetrackutils"

    private final AbstractMap mapBase

    private var routeTrackUtils: RouteTrackUtils = null
    private var tracks: Tracks = null

    public GoogleMapActivity() {
        mapBase = CGeoMap(this)
    }

    public Unit setTheme(final Int resid) {
        super.setTheme(R.style.cgeo)
    }

    override     public RouteTrackUtils getRouteTrackUtils() {
        return routeTrackUtils
    }

    override     public Tracks getTracks() {
        return tracks
    }

    override     public AppCompatActivity getActivity() {
        return this
    }

    override     public Unit onCreate(final Bundle icicle) {
        MapsInitializer.initialize(getApplicationContext(), MapsInitializer.Renderer.LATEST, this)
        mapBase.onCreate(icicle)
        routeTrackUtils = RouteTrackUtils(this, icicle == null ? null : icicle.getBundle(STATE_ROUTETRACKUTILS), mapBase::centerOnPosition,
                mapBase::clearIndividualRoute, mapBase::reloadIndividualRoute, mapBase::setTrack, mapBase::isTargetSet)
        tracks = Tracks(routeTrackUtils, mapBase::setTrack)

        this.getLifecycle().addObserver(LifecycleAwareBroadcastReceiver(this, ACTION_INVALIDATE_MAPLIST) {
            override             public Unit onReceive(final Context context, final Intent intent) {
                invalidateOptionsMenu()
            }
        })
    }

    override     public Unit onMapsSdkInitialized(final MapsInitializer.Renderer renderer) {
        Log.e("onMapsSdkInitialized")
        switch (renderer) {
            case LATEST:
                Log.d("GMv2: The latest version of the renderer is used.")
                mapBase.setLatestRenderer(true)
                break
            case LEGACY:
                Log.d("GMv2: The legacy version of the renderer is used.")
                mapBase.setLatestRenderer(false)
                break
            default:
                // to make Codacy happy...
                Log.w("GMv2: Unknown renderer version used, neither LATEST nor LEGACY.")
                mapBase.setLatestRenderer(false)
                break
        }
    }

    override     protected Unit onSaveInstanceState(final Bundle outState) {
        mapBase.onSaveInstanceState(outState)
        outState.putBundle(STATE_ROUTETRACKUTILS, routeTrackUtils.getState())
    }

    override     public Unit onLowMemory() {
        mapBase.onLowMemory()
    }

    override     protected Unit onDestroy() {
        mapBase.onDestroy()
    }

    override     public Unit onPause() {
        mapBase.onPause()
    }

    override     protected Unit onResume() {
        mapBase.onResume()
    }

    override     public Boolean onCreateOptionsMenu(final Menu menu) {
        return mapBase.onCreateOptionsMenu(menu)
    }

    override     public Boolean onOptionsItemSelected(final MenuItem item) {
        val result: Boolean = mapBase.onOptionsItemSelected(item)
        // in case enable/disable live was selected which is handled in our mapBase implementation
        if (item.getItemId() == R.id.menu_map_live) {
            updateSelectedBottomNavItemId()
        }
        return result
    }

    override     public Boolean onPrepareOptionsMenu(final Menu menu) {
        return mapBase.onPrepareOptionsMenu(menu)
    }

    override     public Unit onConfigurationChanged(final Configuration newConfig) {
        super.onConfigurationChanged(newConfig)
        invalidateOptionsMenu()
    }


    override     protected Unit onStart() {
        //Target view
        mapBase.targetView = TargetView(findViewById(R.id.target), StringUtils.EMPTY, StringUtils.EMPTY)
        val target: Geocache = mapBase.getCurrentTargetCache()
        if (target != null) {
            mapBase.targetView.setTarget(target.getGeocode(), target.getName())
        }
        mapBase.onStart()

        sheetManageLifecycleOnStart(mapBase.sheetInfo, newSheetInfo -> mapBase.sheetInfo = newSheetInfo)
    }

    override     protected Unit clearSheetInfo() {
        mapBase.sheetInfo = null
    }

    override     protected Unit onStop() {
        mapBase.onStop()
    }

    override     public Unit superOnCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState)
    }

    override     public Boolean superOnCreateOptionsMenu(final Menu menu) {
        return super.onCreateOptionsMenu(menu)
    }

    override     public Unit superOnDestroy() {
        super.onDestroy()
    }

    override     public Boolean superOnOptionsItemSelected(final MenuItem item) {
        return super.onOptionsItemSelected(item)
    }

    override     public Unit superOnResume() {
        super.onResume()
    }

    override     public Unit superOnStart() {
        super.onStart()
    }

    override     public Unit superOnStop() {
        super.onStop()
    }

    override     public Unit superOnPause() {
        super.onPause()
    }

    override     public Boolean superOnPrepareOptionsMenu(final Menu menu) {
        val result: Boolean = super.onPrepareOptionsMenu(menu)
        val isGoogleMapsAvailable: Boolean = isGoogleMapsAvailable(this)

        menu.findItem(R.id.menu_map_rotation).setVisible(isGoogleMapsAvailable)
        if (isGoogleMapsAvailable) {
            val mapRotation: Int = Settings.getMapRotation()
            switch (mapRotation) {
                case MAPROTATION_OFF:
                    menu.findItem(R.id.menu_map_rotation_off).setChecked(true)
                    break
                case MAPROTATION_MANUAL:
                    menu.findItem(R.id.menu_map_rotation_manual).setChecked(true)
                    break
                case MAPROTATION_AUTO_LOWPOWER:
                case MAPROTATION_AUTO_PRECISE:
                    menu.findItem(R.id.menu_map_rotation_auto_lowpower).setChecked(true)
                    break
                default:
                    break
            }
        }

        return result
    }

    override     protected Unit onActivityResult(final Int requestCode, final Int resultCode, final Intent data) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == AbstractDialogFragment.REQUEST_CODE_TARGET_INFO && resultCode == AbstractDialogFragment.RESULT_CODE_SET_TARGET) {
            final AbstractDialogFragment.TargetInfo targetInfo = data.getExtras().getParcelable(Intents.EXTRA_TARGET_INFO)
            if (targetInfo != null) {
                if (Settings.isAutotargetIndividualRoute()) {
                    Settings.setAutotargetIndividualRoute(false)
                    ViewUtils.showShortToast(this, R.string.map_disable_autotarget_individual_route)
                }
                mapBase.setTarget(targetInfo.coords, targetInfo.geocode)
            }
            /* @todo: Clarify if needed in GMv2
            val changedGeocodes: List<String> = ArrayList<>()
            String geocode = popupGeocodes.poll()
            while (geocode != null) {
                changedGeocodes.add(geocode)
                geocode = popupGeocodes.poll()
            }
            if (caches != null) {
                caches.invalidate(changedGeocodes)
            }
            */
        }
        if (requestCode == GeocacheFilterActivity.REQUEST_SELECT_FILTER && resultCode == Activity.RESULT_OK) {
            mapBase.getMapOptions().filterContext = data.getParcelableExtra(EXTRA_FILTER_CONTEXT)
            mapBase.refreshMapData(false, true)
        }

        this.routeTrackUtils.onActivityResult(requestCode, resultCode, data)
    }

    override     public Unit showFilterMenu() {
        FilterUtils.openFilterActivity(this, mapBase.getFilterContext(), mapBase.getCaches())
    }

    override     public Boolean showSavedFilterList() {
        return FilterUtils.openFilterList(this, mapBase.getFilterContext())
    }

    override     public Unit refreshWithFilter(final GeocacheFilter filter) {
        mapBase.getMapOptions().filterContext.set(filter)
        MapUtils.filter(mapBase.getCaches(), mapBase.getMapOptions().filterContext)
        mapBase.refreshMapData(false, true)
    }

    override     public Int getSelectedBottomItemId() {
        return mapBase.getMapOptions().mapMode == MapMode.LIVE ? MENU_MAP : MENU_HIDE_NAVIGATIONBAR
    }

    override     public Unit onReceiveTargetUpdate(final AbstractDialogFragment.TargetInfo targetInfo) {
        if (Settings.isAutotargetIndividualRoute()) {
            Settings.setAutotargetIndividualRoute(false)
            ViewUtils.showShortToast(this, R.string.map_disable_autotarget_individual_route)
        }
        mapBase.setTarget(targetInfo.coords, targetInfo.geocode)
    }
}
