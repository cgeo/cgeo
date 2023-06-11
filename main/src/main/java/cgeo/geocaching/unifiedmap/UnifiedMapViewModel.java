package cgeo.geocaching.unifiedmap;

import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.maps.PositionHistory;
import cgeo.geocaching.maps.RouteTrackUtils;
import cgeo.geocaching.maps.Tracks;
import cgeo.geocaching.models.IndividualRoute;
import cgeo.geocaching.models.RouteItem;
import cgeo.geocaching.models.geoitem.IGeoItemSupplier;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.utils.ConstantLiveData;
import cgeo.geocaching.utils.Event;

import android.content.Context;
import android.location.Location;

import androidx.core.util.Pair;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class UnifiedMapViewModel extends ViewModel implements IndividualRoute.UpdateIndividualRoute {

    // ViewModels will survive config changes, no savedInstanceState is needed
    // Don't hold an activity references inside the ViewModel!

    // We might want to try LiveDataReactiveStreams as well

    private Tracks tracks = null;
    public final ConstantLiveData<IndividualRoute> individualRoute = new ConstantLiveData<>(new IndividualRoute(this::setTarget));
    /**
     * Event based LiveData notifying about track updates
     */
    public final MutableLiveData<Event<String>> trackUpdater = new MutableLiveData<>();
    public final MutableLiveData<Pair<Location, Float>> positionAndHeading = new MutableLiveData<>(); // we could create our own class for better understandability, this would require to implement the equals() method though
    public final MutableLiveData<Target> target = new MutableLiveData<>();

    public final MutableLiveData<Geopoint> longTapCoords = new MutableLiveData<>();
    /**
     * LiveData wrapping the PositionHistory object or null if PositionHistory should be hidden
     */
    public final MutableLiveData<PositionHistory> positionHistory = new MutableLiveData<>(Settings.isMapTrail() ? new PositionHistory() : null);
    public final MutableLiveData<Boolean> followMyLocation = new MutableLiveData<>(Settings.getFollowMyLocation());


    public void setCurrentPositionAndHeading(final Location location, final float heading) {
        final PositionHistory ph = positionHistory.getValue();
        if (ph != null) {
            ph.rememberTrailPosition(location);
        }
        positionAndHeading.setValue(new Pair<>(location, heading));
    }

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
        tracks = new Tracks(routeTrackUtils, this::setTrack);
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
}
