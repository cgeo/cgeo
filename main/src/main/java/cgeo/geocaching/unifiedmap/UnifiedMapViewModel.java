package cgeo.geocaching.unifiedmap;

import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.location.ProximityNotification;
import cgeo.geocaching.location.Viewport;
import cgeo.geocaching.maps.PositionHistory;
import cgeo.geocaching.maps.RouteTrackUtils;
import cgeo.geocaching.maps.Tracks;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.models.IndividualRoute;
import cgeo.geocaching.models.RouteItem;
import cgeo.geocaching.models.Waypoint;
import cgeo.geocaching.models.geoitem.IGeoItemSupplier;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.storage.DataStore;
import cgeo.geocaching.utils.LeastRecentlyUsedSet;
import cgeo.geocaching.utils.livedata.CollectionLiveData;
import cgeo.geocaching.utils.livedata.ConstantLiveData;
import cgeo.geocaching.utils.livedata.Event;

import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.Set;

public class UnifiedMapViewModel extends ViewModel implements IndividualRoute.UpdateIndividualRoute {
    public static final int MAX_CACHES = 2000;

    public static final String CACHE_KEY_PREFIX = "CACHE_";
    public static final String CACHE_STAR_KEY_PREFIX = "CACHE_STAR_";
    public static final String WAYPOINT_KEY_PREFIX = "WP_";
    public static final String COORDSPOINT_KEY_PREFIX = "COORDS_";

    // ViewModels will survive config changes, no savedInstanceState is needed
    // Don't hold an activity references inside the ViewModel!

    // We might want to try LiveDataReactiveStreams as well

    private Tracks tracks = null;
    public final ConstantLiveData<IndividualRoute> individualRoute = new ConstantLiveData<>(new IndividualRoute(this::setTarget));
    public MutableLiveData<ProximityNotification> proximityNotification = new MutableLiveData<>(null);
    /**
     * Event based LiveData notifying about track updates
     */
    public final MutableLiveData<Event<String>> trackUpdater = new MutableLiveData<>();
    public final MutableLiveData<LocUpdater.LocationWrapper> location = new MutableLiveData<>();
    public final MutableLiveData<Target> target = new MutableLiveData<>();
    public final MutableLiveData<SheetInfo> sheetInfo = new MutableLiveData<>();

    @NonNull public UnifiedMapType mapType = new UnifiedMapType();

    //Viewport will be refreshed as the map moves. Only valid viewports are used.
    public final MutableLiveData<Viewport> viewport = new MutableLiveData<>(Viewport.EMPTY);
    //Viewport will be refreshed ONLY if the map was not moved for 500ms. Only valid viewports are used.
    public final MutableLiveData<Viewport> viewportIdle = new MutableLiveData<>(Viewport.EMPTY);

    public final CollectionLiveData<Geocache, Set<Geocache>> caches = CollectionLiveData.set(() -> new LeastRecentlyUsedSet<>(MAX_CACHES + DataStore.getAllCachesCount()));
    public final CollectionLiveData<Waypoint, Set<Waypoint>> waypoints = CollectionLiveData.set();
    public final MutableLiveData<LiveMapGeocacheLoader.LiveDataState> liveLoadStatus = new MutableLiveData<>(new LiveMapGeocacheLoader.LiveDataState(LiveMapGeocacheLoader.LoadState.STOPPED, null, null));
    public final LiveMapDataHandler liveMapHandler = new LiveMapDataHandler(this);

    public final CollectionLiveData<String, Set<String>> cachesWithStarDrawn = CollectionLiveData.set(() -> new LeastRecentlyUsedSet<>(MAX_CACHES));

    public final MutableLiveData<Geopoint> longTapCoords = new MutableLiveData<>();
    public final MutableLiveData<Geopoint> coordsIndicator = new MutableLiveData<>(); // null if coords indicator should be hidden

    /**
     * LiveData wrapping the PositionHistory object or null if PositionHistory should be hidden
     */
    public final MutableLiveData<PositionHistory> positionHistory = new MutableLiveData<>(new PositionHistory());
    public final MutableLiveData<Boolean> followMyLocation = new MutableLiveData<>(Settings.getFollowMyLocation());
    public final MutableLiveData<Float> zoomLevel = new MutableLiveData<>();
    public final MutableLiveData<Boolean> transientIsLiveEnabled = new MutableLiveData<>(false);

    public void setTrack(final String key, final IGeoItemSupplier route, final int unused1, final int unused2) {
        tracks.setRoute(key, route);
        trackUpdater.setValue(new Event<>(key));
        //send event to layer/rtutils
    }

    public void setTarget(final Geopoint geopoint, final String geocode) {
        target.setValue(new Target(geopoint, geocode));
    }

    public void reloadIndividualRoute() {
        individualRoute.getValue().reloadRoute(route -> individualRoute.notifyDataChanged());
    }

    public void reloadTracks(final RouteTrackUtils routeTrackUtils) {
        tracks = new Tracks(routeTrackUtils, this::setTrack);
    }

    public void clearIndividualRoute() {
        individualRoute.getValue().clearRoute(route -> individualRoute.notifyDataChanged());
    }

    @Override
    public void updateIndividualRoute(final IndividualRoute route) {
        individualRoute.notifyDataChanged();
    }

    public void toggleRouteItem(final Context context, final RouteItem item) {
        individualRoute.getValue().toggleItem(context, item, route -> individualRoute.notifyDataChanged());
    }

    public Tracks getTracks() {
        return tracks;
    }

    public void init(final RouteTrackUtils routeTrackUtils) {
        reloadTracks(routeTrackUtils);
    }

    public void configureProximityNotification() {
        proximityNotification = new MutableLiveData<>(Settings.isGeneralProximityNotificationActive() ? new ProximityNotification(true, false) : null);
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        this.liveMapHandler.destroy();
    }

    public void notifyZoomLevel(final float zoomLevel) {
        if (this.zoomLevel.getValue() == null || Math.abs(zoomLevel - this.zoomLevel.getValue()) > 0.001f) {
            this.zoomLevel.setValue(zoomLevel);
        }
    }

    // ========================================================================
    // Inner classes for wrapping data which strictly belongs together

    public static class Target {
        public final Geopoint geopoint;
        public final String geocode;

        public Target(final Geopoint geopoint, final String geocode) {
            this.geopoint = geopoint;
            this.geocode = geocode;
        }
    }

    // cache/waypoint sheet opened?
    public static class SheetInfo implements Parcelable {
        public final String geocode;
        public final int waypointId;

        public SheetInfo(final String geocode, final int waypointId) {
            this.geocode = geocode;
            this.waypointId = waypointId;
        }

        // parcelable methods
        public static final Creator<SheetInfo> CREATOR = new Creator<SheetInfo>() {

            @Override
            public SheetInfo createFromParcel(final Parcel source) {
                return new SheetInfo(source);
            }

            @Override
            public SheetInfo[] newArray(final int size) {
                return new SheetInfo[size];
            }

        };

        protected SheetInfo(final Parcel parcel) {
            geocode = parcel.readString();
            waypointId = parcel.readInt();
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(final Parcel dest, final int flags) {
            dest.writeString(geocode);
            dest.writeInt(waypointId);
        }


    }


}
