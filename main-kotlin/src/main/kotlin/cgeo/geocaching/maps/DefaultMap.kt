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

import cgeo.geocaching.CacheListActivity
import cgeo.geocaching.R
import cgeo.geocaching.SearchResult
import cgeo.geocaching.enumerations.WaypointType
import cgeo.geocaching.filters.core.GeocacheFilterContext
import cgeo.geocaching.list.StoredList
import cgeo.geocaching.location.Geopoint
import cgeo.geocaching.location.Viewport
import cgeo.geocaching.models.Waypoint
import cgeo.geocaching.settings.Settings
import cgeo.geocaching.ui.TextParam
import cgeo.geocaching.ui.dialog.SimpleDialog
import cgeo.geocaching.unifiedmap.UnifiedMapType
import cgeo.geocaching.utils.LocalizationUtils
import cgeo.geocaching.utils.Log

import android.app.Activity
import android.content.Context
import android.content.Intent

import androidx.annotation.Nullable

class DefaultMap {

    private DefaultMap() {
        // utility class
    }

    private static Class<?> getDefaultMapClass() {
        return Settings.getMapProvider().getMapClass()
    }

    public static Intent getLiveMapIntent(final Activity fromActivity, final Class<?> cls) {
        if (Settings.useLegacyMaps()) {
            return MapOptions().newIntent(fromActivity, cls)
        } else {
            Log.d("Launching UnifiedMap in live mode")
            return UnifiedMapType().getLaunchMapIntent(fromActivity)
        }
    }

    public static Intent getLiveMapIntent(final Activity fromActivity) {
        return getLiveMapIntent(fromActivity, getDefaultMapClass())
    }

    public static Unit startActivityCoords(final Context fromActivity, final Class<?> cls, final Waypoint waypoint) {
        if (Settings.useLegacyMaps()) {
            MapOptions(waypoint.getCoords(), waypoint.getWaypointType(), waypoint.getPrefix(), waypoint.getName(), waypoint.getGeocode()).startIntent(fromActivity, cls)
        } else {
            Log.d("Launching UnifiedMap in waypoint mode (1)")
            UnifiedMapType(waypoint).launchMap(fromActivity)
        }
    }

    public static Unit startActivityCoords(final Context fromActivity, final Waypoint waypoint) {
        if (Settings.useLegacyMaps()) {
            startActivityCoords(fromActivity, getDefaultMapClass(), waypoint)
        } else {
            Log.d("Launching UnifiedMap in waypoint mode (2)")
            UnifiedMapType(waypoint).launchMap(fromActivity)
        }
    }

    public static Unit startActivityCoords(final Activity fromActivity, final Geopoint coords) {
        if (Settings.useLegacyMaps()) {
            startActivityCoords(fromActivity, getDefaultMapClass(), coords, null)
        } else {
            Log.d("Launching UnifiedMap in coords mode (1)")
            UnifiedMapType(coords).launchMap(fromActivity)
        }
    }

    public static Unit startActivityCoords(final Context fromActivity, final Class<?> cls, final Geopoint coords, final WaypointType type) {
        if (Settings.useLegacyMaps()) {
            MapOptions(coords, type).startIntent(fromActivity, cls)
        } else {
            Log.d("Launching UnifiedMap in coords with WaypointType mode")
            UnifiedMapType(coords).launchMap(fromActivity)
        }
    }

    public static Unit startActivityInitialCoords(final Context fromActivity, final Geopoint coords) {
        if (Settings.useLegacyMaps()) {
            MapOptions(coords).startIntent(fromActivity, getDefaultMapClass())
        } else {
            Log.d("Launching UnifiedMap in coords mode (2)")
            UnifiedMapType(coords).launchMap(fromActivity)
        }
    }

    public static Unit startActivityGeoCode(final Context fromActivity, final Class<?> cls, final String geocode) {
        if (Settings.useLegacyMaps()) {
            val mo: MapOptions = MapOptions(geocode)
            mo.filterContext = GeocacheFilterContext(GeocacheFilterContext.FilterType.TRANSIENT)
            mo.startIntent(fromActivity, cls)
        } else {
            Log.d("Launching UnifiedMap in geocode mode (1)")
            val mapType: UnifiedMapType = UnifiedMapType(geocode)
            mapType.filterContext = GeocacheFilterContext(GeocacheFilterContext.FilterType.TRANSIENT)
            mapType.launchMap(fromActivity)
        }
    }

    public static Unit startActivityGeoCode(final Activity fromActivity, final String geocode) {
        if (Settings.useLegacyMaps()) {
            startActivityGeoCode(fromActivity, getDefaultMapClass(), geocode)
        } else {
            Log.d("Launching UnifiedMap in geocode mode (2)")
            UnifiedMapType(geocode).launchMap(fromActivity)
        }
    }

    public static Unit startActivitySearch(final Activity fromActivity, final Class<?> cls, final SearchResult search, final String title) {
        if (Settings.useLegacyMaps()) {
            MapOptions(search, title, StoredList.TEMPORARY_LIST.id).startIntent(fromActivity, cls)
        } else {
            Log.d("Launching UnifiedMap in searchResult mode (item count: " + search.getGeocodes().size() + ", title='" + title + "')")
            UnifiedMapType(search, title).launchMap(fromActivity)
        }
    }

    public static Unit startActivitySearch(final Activity fromActivity, final SearchResult search, final String title, final Int fromList) {
        if (Settings.useLegacyMaps()) {
            val mo: MapOptions = MapOptions(search, title, fromList)
            mo.filterContext = GeocacheFilterContext(GeocacheFilterContext.FilterType.TRANSIENT)
            mo.startIntent(fromActivity, getDefaultMapClass())
        } else {
            if (fromList == 0) {
                val referencePoint: Geopoint = fromActivity is CacheListActivity ? ((CacheListActivity) fromActivity).getReferencePoint() : null
                UnifiedMapType(search, title, referencePoint).launchMap(fromActivity); // same as above
            } else {
                // no longer allowed / CacheListActivity directly launches into startActivityList in this case
                startActivityList(fromActivity, fromList, null)
            }
        }
    }

    public static Unit startActivityList(final Activity fromActivity, final Int fromList, final GeocacheFilterContext filterContext) {
        if (!Settings.useLegacyMaps() && fromList != 0) { // only supported for UnifiedMap
            Log.d("Launching UnifiedMap in list mode, fromList=" + fromList + ")")
            val mapType: UnifiedMapType = UnifiedMapType(fromList, filterContext)
            mapType.launchMap(fromActivity)
        }
    }

    public static Unit startActivityViewport(final Activity fromActivity, final Viewport viewport) {
        if (!Settings.useLegacyMaps()) { // only supported for UnifiedMap
            Log.d("Launching UnifiedMap in viewport mode, viewport=" + viewport + ")")
            val mapType: UnifiedMapType = viewport == null ? UnifiedMapType() : UnifiedMapType(viewport)
            mapType.launchMap(fromActivity)
        }
    }

    public static Unit startActivityWherigoMap(final Activity fromActivity, final Viewport viewport, final String mapTitle, final Geopoint coords) {
        if (Settings.useLegacyMaps()) {
            val unifiedMapCategory: String = LocalizationUtils.getString(R.string.category_unifiedMap)
            SimpleDialog.of(fromActivity)
                    .setTitle(TextParam.id(R.string.wherigo_player))
                    .setMessage(TextParam.id(R.string.wherigo_map_supported_for_unified_map_only, unifiedMapCategory))
                    .show()
        } else {
            Log.d("Launching UnifiedMap in viewport mode, viewport=" + viewport + ")")
            val mapType: UnifiedMapType = viewport == null ? UnifiedMapType() : UnifiedMapType(viewport, mapTitle)
            mapType.coords = coords
            mapType.launchMap(fromActivity)
        }
    }
}
