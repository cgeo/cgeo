package cgeo.geocaching.permission;

import cgeo.geocaching.sensors.Sensors;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.utils.Log;

/**
 * Extension to @see PermissionGrantedCallback that (in case of granted location permission) initializes the location observables
 * and then calls executeAfter
 */
public abstract class RestartLocationPermissionGrantedCallback extends PermissionGrantedCallback {

    protected RestartLocationPermissionGrantedCallback(final PermissionRequestContext request) {
        super(request);
    }

    protected final void execute() {
        Log.d("RestartLocationPermissionGrantedCallback.execute for " + getRequestCode());
        final Sensors sensors = Sensors.getInstance();
        sensors.setupGeoDataObservables(Settings.useGooglePlayServices(), Settings.useLowPowerMode());
        sensors.setupDirectionObservable();

        executeAfter();
    }

    protected abstract void executeAfter();

}
