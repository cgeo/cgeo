package cgeo.geocaching;

import cgeo.geocaching.location.ProximityNotification;
import cgeo.geocaching.sensors.GeoData;

import android.os.Bundle;

// like AbstractDialogFragment, but plays beeps if close enough to reference point
public abstract class AbstractDialogFragmentWithBeep extends AbstractDialogFragment {
    protected ProximityNotification proximityNotification = null;

    /**
     * @param geo
     *            location
     */
    protected void onUpdateGeoData(final GeoData geo) {
        super.onUpdateGeoData(geo);
        if (null != proximityNotification) {
            proximityNotification.onUpdateGeoData(geo);
        }
    }

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        proximityNotification = new ProximityNotification();
    }
}
