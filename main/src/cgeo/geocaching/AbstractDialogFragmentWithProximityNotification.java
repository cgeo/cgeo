package cgeo.geocaching;

import cgeo.geocaching.location.ProximityNotificationByCoords;
import cgeo.geocaching.sensors.GeoData;
import cgeo.geocaching.settings.Settings;

import android.os.Bundle;

public abstract class AbstractDialogFragmentWithProximityNotification extends AbstractDialogFragment {
    protected ProximityNotificationByCoords proximityNotification = null;

    @Override
    protected void onUpdateGeoData(final GeoData geo) {
        super.onUpdateGeoData(geo);
        if (null != proximityNotification) {
            proximityNotification.onUpdateGeoData(geo);
        }
    }

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        proximityNotification = Settings.isSpecificProximityNotificationActive() ? new ProximityNotificationByCoords() : null;
    }
}
