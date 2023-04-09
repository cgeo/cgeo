package cgeo.geocaching.unifiedmap;

import cgeo.geocaching.maps.PositionHistory;
import cgeo.geocaching.maps.RouteTrackUtils;
import cgeo.geocaching.maps.Tracks;
import cgeo.geocaching.models.geoitem.IGeoItemSupplier;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.utils.Event;

import android.location.Location;

import androidx.annotation.NonNull;
import androidx.core.util.Pair;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class UnifiedMapViewModel extends ViewModel {

    // ViewModels will survive config changes, no savedInstanceState is needed

    private Tracks tracks = null;
    private final MutableLiveData<Event<Tracks.Track>> trackUpdater = new MutableLiveData<>();
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
        trackUpdater.setValue(new Event<>(tracks.getTrack(key)));
        //send event to layer/rtutils
    }

    public void onMapChanged(final RouteTrackUtils routeTrackUtils) {
        tracks = new Tracks(routeTrackUtils, this::setTrack);
    }

    /**
     * Event based LiveData notifying about track updates
     */
    @NonNull
    public MutableLiveData<Event<Tracks.Track>> getTrackUpdater() {
        return trackUpdater;
    }
}
