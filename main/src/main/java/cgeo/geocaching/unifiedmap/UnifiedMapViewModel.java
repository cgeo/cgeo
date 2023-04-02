package cgeo.geocaching.unifiedmap;

import android.location.Location;

import androidx.core.util.Pair;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class UnifiedMapViewModel extends ViewModel {

    private final MutableLiveData<Pair<Location, Float>> positionAndHeading = new MutableLiveData<>();

    public void setCurrentPositionAndHeading(final Location location, final float heading) {
        positionAndHeading.setValue(new Pair<>(location, heading));
    }

    public MutableLiveData<Pair<Location, Float>> getPositionAndHeading() {
        return positionAndHeading;
    }

}
