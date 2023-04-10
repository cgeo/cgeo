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

import androidx.annotation.NonNull;
import androidx.core.util.Pair;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class UnifiedMapViewModel extends ViewModel {

    // ViewModels will survive config changes, no savedInstanceState is needed
    // Don't hold an activity references inside the ViewModel!

    private Tracks tracks = null;
    private final ConstantLiveData<IndividualRoute> individualRoute = new ConstantLiveData<>(new IndividualRoute(this::setTarget));
    private final MutableLiveData<Event<String>> trackUpdater = new MutableLiveData<>();
    private final MutableLiveData<Pair<Location, Float>> positionAndHeading = new MutableLiveData<>();
    private final MutableLiveData<PositionHistory> positionHistory = new MutableLiveData<>(Settings.isMapTrail() ? new PositionHistory() : null);

    public void setCurrentPositionAndHeading(final Location location, final float heading) {
        final PositionHistory ph = positionHistory.getValue();
        if (ph != null) {
            ph.rememberTrailPosition(location);
        }
        positionAndHeading.setValue(new Pair<>(location, heading));
    }

    /**
     * Current location and heading of the user
     */
    @NonNull
    public MutableLiveData<Pair<Location, Float>> getPositionAndHeading() {

        return positionAndHeading;
    }

    /**
     * LiveData wrapping the PositionHistory object or null if PositionHistory should be hidden
     */
    @NonNull
    public MutableLiveData<PositionHistory> getPositionHistory() {
        return positionHistory;
    }

    public void setTrack(final String key, final IGeoItemSupplier route, final int unused1, final int unused2) {
        tracks.setRoute(key, route);
        trackUpdater.setValue(new Event<>(key));
        //send event to layer/rtutils
    }

    private void setTarget(Geopoint geopoint, String s) {
        //todo
    }

    public void reloadIndividualRoute() {
        individualRoute.getValue().reloadRoute(route -> individualRoute.notifyDataChanged());
    }

    public void clearIndividualRoute() {
        individualRoute.getValue().clearRoute(route -> individualRoute.notifyDataChanged());
    }

    public void toggleRouteItem(final Context context, final RouteItem item) {
        individualRoute.getValue().toggleItem(context, item, route -> individualRoute.notifyDataChanged());
    }

    public Tracks getTracks() {
        return tracks;
    }

    public ConstantLiveData<IndividualRoute> getIndividualRoute() {
        return individualRoute;
    }

    public void init(final RouteTrackUtils routeTrackUtils) {
        tracks = new Tracks(routeTrackUtils, this::setTrack);
    }

    /**
     * Event based LiveData notifying about track updates
     */
    @NonNull
    public MutableLiveData<Event<String>> getTrackUpdater() {
        return trackUpdater;
    }
}
