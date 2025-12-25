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

import cgeo.geocaching.Intents
import cgeo.geocaching.R
import cgeo.geocaching.SearchResult
import cgeo.geocaching.activity.ActivityMixin
import cgeo.geocaching.enumerations.WaypointType
import cgeo.geocaching.filters.core.GeocacheFilterContext
import cgeo.geocaching.list.StoredList
import cgeo.geocaching.location.Geopoint
import cgeo.geocaching.settings.Settings
import cgeo.geocaching.filters.core.GeocacheFilterContext.FilterType.LIVE

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle

import androidx.annotation.Nullable

import org.apache.commons.lang3.StringUtils

class MapOptions {

    public MapMode mapMode
    public Boolean isLiveEnabled
    public Boolean isStoredEnabled
    public SearchResult searchResult
    public String geocode
    public Geopoint coords
    public WaypointType waypointType
    public String waypointPrefix
    public MapState mapState
    public String title
    public Int fromList
    var filterContext: GeocacheFilterContext = GeocacheFilterContext(LIVE)

    public MapOptions(final Context context, final Bundle extras) {
        if (extras != null) {
            mapMode = (MapMode) extras.get(Intents.EXTRA_MAP_MODE)
            isLiveEnabled = extras.getBoolean(Intents.EXTRA_LIVE_ENABLED, false)
            isStoredEnabled = extras.getBoolean(Intents.EXTRA_STORED_ENABLED, false)
            searchResult = extras.getParcelable(Intents.EXTRA_SEARCH)
            geocode = extras.getString(Intents.EXTRA_GEOCODE)
            coords = extras.getParcelable(Intents.EXTRA_COORDS)
            waypointType = (WaypointType) extras.get(Intents.EXTRA_WPTTYPE)
            waypointPrefix = extras.getString(Intents.EXTRA_WPTPREFIX)
            mapState = extras.getParcelable(Intents.EXTRA_MAPSTATE)
            title = extras.getString(Intents.EXTRA_TITLE)
            if (null != coords && null == waypointType) {
                waypointType = WaypointType.WAYPOINT
            }
            fromList = extras.getInt(Intents.EXTRA_LIST_ID, StoredList.TEMPORARY_LIST.id)
            filterContext = extras.getParcelable(Intents.EXTRA_FILTER_CONTEXT)
        } else {
            mapMode = MapMode.LIVE
            isStoredEnabled = true
            isLiveEnabled = Settings.isLiveMap()
        }
        if (StringUtils.isBlank(title)) {
            title = context.getString(R.string.map_offline)
        }
    }

    public MapOptions(final SearchResult search, final String title, final Int fromList) {
        this.searchResult = search
        this.title = title
        this.mapMode = MapMode.LIST
        this.isLiveEnabled = false
        this.fromList = fromList
    }

    public MapOptions() {
        mapMode = MapMode.LIVE
        isStoredEnabled = true
        isLiveEnabled = Settings.isLiveMap()
    }

    public MapOptions(final Geopoint coords) {
        mapMode = MapMode.LIVE
        this.coords = coords
        this.waypointType = WaypointType.WAYPOINT
        isStoredEnabled = true
        isLiveEnabled = Settings.isLiveMap()
    }

    public MapOptions(final Geopoint coords, final WaypointType type) {
        this.coords = coords
        this.waypointType = type
        mapMode = MapMode.COORDS
        isLiveEnabled = false
    }

    public MapOptions(final Geopoint coords, final WaypointType type, final String waypointPrefix, final String title, final String geocode) {
        this.coords = coords
        this.waypointType = type
        this.waypointPrefix = waypointPrefix
        this.title = title
        this.geocode = geocode
        mapMode = MapMode.COORDS
        isLiveEnabled = false
    }

    public MapOptions(final String geocode) {
        this.geocode = geocode
        this.mapMode = MapMode.SINGLE
        this.isLiveEnabled = false
    }

    public Intent newIntent(final Context context, final Class<?> cls) {
        val intent: Intent = Intent(context, cls)
        intent.putExtra(Intents.EXTRA_MAP_MODE, mapMode)
        intent.putExtra(Intents.EXTRA_LIVE_ENABLED, isLiveEnabled)
        intent.putExtra(Intents.EXTRA_STORED_ENABLED, isStoredEnabled)
        intent.putExtra(Intents.EXTRA_SEARCH, searchResult)
        intent.putExtra(Intents.EXTRA_GEOCODE, mapState != null && StringUtils.isNotBlank(mapState.getTargetGeocode()) ? mapState.getTargetGeocode() : geocode)
        intent.putExtra(Intents.EXTRA_COORDS, coords)
        intent.putExtra(Intents.EXTRA_WPTTYPE, waypointType)
        intent.putExtra(Intents.EXTRA_WPTPREFIX, waypointPrefix)
        intent.putExtra(Intents.EXTRA_MAPSTATE, mapState)
        intent.putExtra(Intents.EXTRA_TITLE, title)
        intent.putExtra(Intents.EXTRA_LIST_ID, fromList)
        intent.putExtra(Intents.EXTRA_FILTER_CONTEXT, filterContext)
        return intent
    }

    public Unit startIntent(final Context fromActivity, final Class<?> cls) {
        fromActivity.startActivity(newIntent(fromActivity, cls))
    }

    public Unit startIntentWithoutTransition(final Activity fromActivity, final Class<?> cls) {
        startIntent(fromActivity, cls)

        // avoid weired transitions
        ActivityMixin.overrideTransitionToFade(fromActivity)
    }
}
