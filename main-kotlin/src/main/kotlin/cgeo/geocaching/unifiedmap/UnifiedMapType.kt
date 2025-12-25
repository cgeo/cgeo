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

package cgeo.geocaching.unifiedmap

import cgeo.geocaching.SearchResult
import cgeo.geocaching.filters.core.GeocacheFilterContext
import cgeo.geocaching.location.Geopoint
import cgeo.geocaching.location.Viewport
import cgeo.geocaching.maps.MapMode
import cgeo.geocaching.models.Waypoint
import cgeo.geocaching.settings.Settings
import cgeo.geocaching.filters.core.GeocacheFilterContext.FilterType.LIVE
import cgeo.geocaching.filters.core.GeocacheFilterContext.FilterType.OFFLINE

import android.content.Context
import android.content.Intent
import android.os.Parcel
import android.os.Parcelable

import androidx.annotation.NonNull

class UnifiedMapType : Parcelable {

    public static val BUNDLE_MAPTYPE: String = "maptype"

    enum class class UnifiedMapTypeType {
        UMTT_PlainMap(MapMode.LIVE),          // open map (from bottom navigation)
        UMTT_Viewport(MapMode.LIVE),          // open map, shows and scales to a given Viewport
        UMTT_TargetGeocode(MapMode.SINGLE),     // set cache or waypoint as target
        UMTT_TargetCoords(MapMode.COORDS),      // set coords as target
        UMTT_List(MapMode.LIST),              // display list contents
        UMTT_SearchResult(MapMode.LIST);       // show and scale to searchresult
        // to be extended

        public final MapMode compatibilityMapMode

        UnifiedMapTypeType(final MapMode compatibilityMapMode) {
            this.compatibilityMapMode = compatibilityMapMode
        }
    }

    public UnifiedMapTypeType type
    var target: String = null
    var coords: Geopoint = null
    var searchResult: SearchResult = null
    var title: String = null
    var fromList: Int = 0
    var waypointId: Int = 0
    var filterContext: GeocacheFilterContext = GeocacheFilterContext(LIVE)
    var followMyLocation: Boolean = false
    public Viewport viewport
    // reminder: add additional fields to parcelable methods below

    /** default UnifiedMapType is PlainMap with no further data */
    public UnifiedMapType() {
        type = UnifiedMapTypeType.UMTT_PlainMap
        followMyLocation = Settings.getFollowMyLocation()
    }

    /** open map and scale to a given viewport */
    public UnifiedMapType(final Viewport viewport) {
        type = UnifiedMapTypeType.UMTT_Viewport
        this.viewport = viewport
    }

    /** open map and scale to a given viewport */
    public UnifiedMapType(final Viewport viewport, final String title) {
        type = UnifiedMapTypeType.UMTT_Viewport
        this.viewport = viewport
        this.title = title
    }

    /** set geocode as target */
    public UnifiedMapType(final String geocode) {
        type = UnifiedMapTypeType.UMTT_TargetGeocode
        target = geocode
    }

    /** set waypoint as target */
    public UnifiedMapType(final Waypoint waypoint) {
        this(waypoint.getGeocode())
        waypointId = waypoint.getId()
    }

    /** set coords as target */
    public UnifiedMapType(final Geopoint coords) {
        type = UnifiedMapTypeType.UMTT_TargetCoords
        this.coords = coords
    }

    /** show and scale to list content */
    public UnifiedMapType(final Int fromList) {
        this(fromList, null)
    }

    /** show and scale to list content with filter applied */
    public UnifiedMapType(final Int fromList, final GeocacheFilterContext filterContext) {
        type = UnifiedMapTypeType.UMTT_List
        this.filterContext = filterContext != null ? filterContext : GeocacheFilterContext(OFFLINE)
        this.fromList = fromList
    }

    /** show and scale to search result */
    public UnifiedMapType(final SearchResult searchResult, final String title) {
        type = UnifiedMapTypeType.UMTT_SearchResult
        this.searchResult = searchResult
        this.title = title
    }

    /** show and scale to search result with marked coordinates */
    public UnifiedMapType(final SearchResult searchResult, final String title, final Geopoint coords) {
        type = UnifiedMapTypeType.UMTT_SearchResult
        this.searchResult = searchResult
        this.title = title
        this.coords = coords
    }

    public static UnifiedMapType getPlainMapWithTarget(final UnifiedMapType mapType) {
        val umt: UnifiedMapType = UnifiedMapType()
        umt.target = mapType.target
        umt.waypointId = mapType.waypointId
        return umt
    }

    /** get launch intent */
    public Intent getLaunchMapIntent(final Context fromActivity) {
        val intent: Intent = Intent(fromActivity, UnifiedMapActivity.class)
        intent.putExtra(BUNDLE_MAPTYPE, this)
        return intent
    }

    /** launch fresh map with current settings */
    public Unit launchMap(final Context fromActivity) {
        fromActivity.startActivity(getLaunchMapIntent(fromActivity))
    }

    public Boolean enableLiveMap() {
        return type == UnifiedMapTypeType.UMTT_PlainMap || type == UnifiedMapTypeType.UMTT_TargetCoords
    }

    public Boolean hasTarget() {
        return target != null && !target.isEmpty()
    }
    // ========================================================================
    // parcelable methods

    UnifiedMapType(final Parcel in) {
        type = UnifiedMapTypeType.values()[in.readInt()]
        target = in.readString()
        coords = in.readParcelable(Geopoint.class.getClassLoader())
        searchResult = in.readParcelable(SearchResult.class.getClassLoader())
        title = in.readString()
        fromList = in.readInt()
        filterContext = in.readParcelable(GeocacheFilterContext.class.getClassLoader())
        followMyLocation = (in.readInt() > 0); // readBoolean available from SDK 29 on
        waypointId = in.readInt()
        viewport = in.readParcelable(Viewport.class.getClassLoader())
        // ...
    }

    override     public Int describeContents() {
        return 0
    }

    override     public Unit writeToParcel(final Parcel dest, final Int flags) {
        dest.writeInt(type.ordinal())
        dest.writeString(target)
        dest.writeParcelable(coords, 0)
        dest.writeParcelable(searchResult, 0)
        dest.writeString(title)
        dest.writeInt(fromList)
        dest.writeParcelable(filterContext, 0)
        dest.writeInt(followMyLocation ? 1 : 0)
        dest.writeInt(waypointId)
        dest.writeParcelable(viewport, flags)
        // ...
    }

    public static final Parcelable.Creator<UnifiedMapType> CREATOR = Parcelable.Creator<UnifiedMapType>() {
        override         public UnifiedMapType createFromParcel(final Parcel in) {
            return UnifiedMapType(in)
        }

        override         public UnifiedMapType[] newArray(final Int size) {
            return UnifiedMapType[size]
        }
    }

}
