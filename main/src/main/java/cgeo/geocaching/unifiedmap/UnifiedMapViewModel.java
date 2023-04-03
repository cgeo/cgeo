package cgeo.geocaching.unifiedmap;

import cgeo.geocaching.maps.PositionHistory;
import cgeo.geocaching.settings.Settings;

import android.location.Location;

import androidx.annotation.NonNull;
import androidx.core.util.Pair;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class UnifiedMapViewModel extends ViewModel {

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
}
