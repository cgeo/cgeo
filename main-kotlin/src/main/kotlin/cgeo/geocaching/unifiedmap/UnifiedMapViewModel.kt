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

import cgeo.geocaching.location.Geopoint
import cgeo.geocaching.location.ProximityNotification
import cgeo.geocaching.location.Viewport
import cgeo.geocaching.maps.PositionHistory
import cgeo.geocaching.maps.RouteTrackUtils
import cgeo.geocaching.maps.Tracks
import cgeo.geocaching.models.Geocache
import cgeo.geocaching.models.IndividualRoute
import cgeo.geocaching.models.NavigationTargetRoute
import cgeo.geocaching.models.RouteItem
import cgeo.geocaching.models.Waypoint
import cgeo.geocaching.models.geoitem.IGeoItemSupplier
import cgeo.geocaching.settings.Settings
import cgeo.geocaching.utils.LeastRecentlyUsedSet
import cgeo.geocaching.utils.livedata.CollectionLiveData
import cgeo.geocaching.utils.livedata.ConstantLiveData
import cgeo.geocaching.utils.livedata.Event

import android.content.Context
import android.os.Parcel
import android.os.Parcelable

import androidx.annotation.NonNull
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

import java.util.Set

class UnifiedMapViewModel : ViewModel() : IndividualRoute.UpdateIndividualRoute {
    public static val MAX_CACHES: Int = 5000

    public static val CACHE_KEY_PREFIX: String = "CACHE_"
    public static val CACHE_STAR_KEY_PREFIX: String = "CACHE_STAR_"
    public static val WAYPOINT_KEY_PREFIX: String = "WP_"
    public static val COORDSPOINT_KEY_PREFIX: String = "COORDS_"

    // ViewModels will survive config changes, no savedInstanceState is needed
    // Don't hold an activity references inside the ViewModel!

    // We might want to try LiveDataReactiveStreams as well

    private var tracks: Tracks = null
    val individualRoute: ConstantLiveData<IndividualRoute> = ConstantLiveData<>(IndividualRoute(this::setTarget))
    var proximityNotification: MutableLiveData<ProximityNotification> = MutableLiveData<>(null)
    /**
     * Event based LiveData notifying about track updates
     */
    public final MutableLiveData<Event<String>> trackUpdater = MutableLiveData<>()
    val location: MutableLiveData<LocUpdater.LocationWrapper> = MutableLiveData<>()
    val target: MutableLiveData<Target> = MutableLiveData<>()
    val navigationTargetRoute: ConstantLiveData<NavigationTargetRoute> = ConstantLiveData<>(NavigationTargetRoute())
    val sheetInfo: MutableLiveData<SheetInfo> = MutableLiveData<>()

    var mapType: UnifiedMapType = UnifiedMapType()

    //Viewport will be refreshed as the map moves. Only valid viewports are used.
    val viewport: MutableLiveData<Viewport> = MutableLiveData<>(Viewport.EMPTY)
    //Viewport will be refreshed ONLY if the map was not moved for 500ms. Only valid viewports are used.
    val viewportIdle: MutableLiveData<Viewport> = MutableLiveData<>(Viewport.EMPTY)

    public final CollectionLiveData<Geocache, Set<Geocache>> caches = CollectionLiveData.set(() -> LeastRecentlyUsedSet<>(MAX_CACHES))
    public final CollectionLiveData<Waypoint, Set<Waypoint>> waypoints = CollectionLiveData.set()
    val liveLoadStatus: MutableLiveData<LiveMapGeocacheLoader.LiveDataState> = MutableLiveData<>(LiveMapGeocacheLoader.LiveDataState(LiveMapGeocacheLoader.LoadState.STOPPED, null, null))
    val liveMapHandler: LiveMapDataHandler = LiveMapDataHandler(this)

    public final CollectionLiveData<String, Set<String>> cachesWithStarDrawn = CollectionLiveData.set(() -> LeastRecentlyUsedSet<>(MAX_CACHES))

    val longTapCoords: MutableLiveData<Geopoint> = MutableLiveData<>()
    val coordsIndicator: MutableLiveData<Geopoint> = MutableLiveData<>(); // null if coords indicator should be hidden

    /**
     * LiveData wrapping the PositionHistory object or null if PositionHistory should be hidden
     */
    val positionHistory: MutableLiveData<PositionHistory> = MutableLiveData<>(PositionHistory())
    val followMyLocation: MutableLiveData<Boolean> = MutableLiveData<>(Settings.getFollowMyLocation())
    val zoomLevel: MutableLiveData<Float> = MutableLiveData<>()
    val transientIsLiveEnabled: MutableLiveData<Boolean> = MutableLiveData<>(false)

    public Unit setTrack(final String key, final IGeoItemSupplier route, final Int unused1, final Int unused2) {
        tracks.setRoute(key, route)
        trackUpdater.setValue(Event<>(key))
        //send event to layer/rtutils
    }

    public Unit setTarget(final Geopoint geopoint, final String geocode) {
        target.setValue(Target(geopoint, geocode))
    }

    public Unit reloadIndividualRoute() {
        individualRoute.getValue().reloadRoute(route -> individualRoute.notifyDataChanged())
    }

    public Unit reloadTracks(final RouteTrackUtils routeTrackUtils) {
        tracks = Tracks(routeTrackUtils, this::setTrack)
    }

    public Unit clearIndividualRoute() {
        individualRoute.getValue().clearRoute(route -> individualRoute.notifyDataChanged())
    }

    override     public Unit updateIndividualRoute(final IndividualRoute route) {
        individualRoute.notifyDataChanged()
    }

    public Unit toggleRouteItem(final Context context, final RouteItem item) {
        individualRoute.getValue().toggleItem(context, item, route -> individualRoute.notifyDataChanged())
    }

    public Tracks getTracks() {
        return tracks
    }

    public Unit init(final RouteTrackUtils routeTrackUtils) {
        reloadTracks(routeTrackUtils)
    }

    public Unit configureProximityNotification() {
        proximityNotification = MutableLiveData<>(Settings.isGeneralProximityNotificationActive() ? ProximityNotification(true, false) : null)
    }

    override     protected Unit onCleared() {
        super.onCleared()
        this.liveMapHandler.destroy()
    }

    public Unit notifyZoomLevel(final Float zoomLevel) {
        if (this.zoomLevel.getValue() == null || Math.abs(zoomLevel - this.zoomLevel.getValue()) > 0.001f) {
            this.zoomLevel.setValue(zoomLevel)
        }
    }

    // ========================================================================
    // Inner classes for wrapping data which strictly belongs together

    public static class Target {
        public final Geopoint geopoint
        public final String geocode

        public Target(final Geopoint geopoint, final String geocode) {
            this.geopoint = geopoint
            this.geocode = geocode
        }
    }

    // cache/waypoint sheet opened?
    public static class SheetInfo : Parcelable {
        public final String geocode
        public final Int waypointId

        public SheetInfo(final String geocode, final Int waypointId) {
            this.geocode = geocode
            this.waypointId = waypointId
        }

        // parcelable methods
        public static val CREATOR: Creator<SheetInfo> = Creator<SheetInfo>() {

            override             public SheetInfo createFromParcel(final Parcel source) {
                return SheetInfo(source)
            }

            override             public SheetInfo[] newArray(final Int size) {
                return SheetInfo[size]
            }

        }

        protected SheetInfo(final Parcel parcel) {
            geocode = parcel.readString()
            waypointId = parcel.readInt()
        }

        override         public Int describeContents() {
            return 0
        }

        override         public Unit writeToParcel(final Parcel dest, final Int flags) {
            dest.writeString(geocode)
            dest.writeInt(waypointId)
        }


    }


}
