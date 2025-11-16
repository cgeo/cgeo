package cgeo.geocaching.sensors;

import cgeo.geocaching.permission.PermissionContext;
import cgeo.geocaching.utils.AndroidRxUtils;
import cgeo.geocaching.utils.Log;

import android.content.Context;
import android.location.GnssStatus;
import android.location.GnssStatus.Callback;
import android.location.LocationManager;

import io.reactivex.rxjava3.core.Observable;

public class GnssStatusProvider {

    private static final Status NO_GNSS = new Status(false, 0, 0);

    private GnssStatusProvider() {
        // Utility class, not to be instantiated
    }

    public static class Status {
        public final boolean gnssEnabled;
        public final int satellitesVisible;
        public final int satellitesFixed;

        public Status(final boolean gnssEnabled, final int satellitesVisible, final int satellitesFixed) {
            this.gnssEnabled = gnssEnabled;
            this.satellitesVisible = satellitesVisible;
            this.satellitesFixed = satellitesFixed;
        }
    }

    private static Observable<Status> createGNSSObservable(final Context context) {
        return Observable.create(emitter -> {
            final LocationManager geoManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
            final Callback callback = new Callback() {
                Status latest = NO_GNSS;

                @Override
                public void onSatelliteStatusChanged(final GnssStatus status) {
                    final int visible = status.getSatelliteCount();
                    int fixed = 0;
                    for (int satelliteIndex = 0; satelliteIndex < visible; satelliteIndex++) {
                        if (status.usedInFix(satelliteIndex)) {
                            fixed++;
                        }
                    }
                    if (visible == latest.satellitesVisible && fixed == latest.satellitesFixed) {
                        return;
                    }
                    latest = new Status(true, visible, fixed);
                    emitter.onNext(latest);
                }

                @Override
                public void onStarted() {
                    latest = new Status(true, 0, 0);
                    emitter.onNext(latest);
                }

                @Override
                public void onStopped() {
                    latest = NO_GNSS;
                    emitter.onNext(latest);
                }
            };
            emitter.onNext(NO_GNSS);
            if (PermissionContext.LOCATION.hasAllPermissions()) {
                Log.d("GnssStatusProvider.createGNSSObservable: registering callback");
                geoManager.registerGnssStatusCallback(callback);
            } else {
                Log.d("GnssStatusProvider.createGNSSObservable: Could not register provider, no Location permission available");
            }
            emitter.setDisposable(AndroidRxUtils.disposeOnCallbacksScheduler(() -> geoManager.unregisterGnssStatusCallback(callback)));
        });
    }

    public static Observable<Status> create(final Context context) {
        final Observable<Status> observable;
        observable = createGNSSObservable(context);
        return observable.subscribeOn(AndroidRxUtils.looperCallbacksScheduler);
    }
}
