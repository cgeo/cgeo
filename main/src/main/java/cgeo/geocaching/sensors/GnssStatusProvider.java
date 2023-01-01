package cgeo.geocaching.sensors;

import cgeo.geocaching.permission.PermissionContext;
import cgeo.geocaching.utils.AndroidRxUtils;
import cgeo.geocaching.utils.Log;

import android.content.Context;
import android.location.GnssStatus;
import android.location.GnssStatus.Callback;
import android.location.GpsSatellite;
import android.location.GpsStatus;
import android.location.GpsStatus.Listener;
import android.location.LocationManager;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;

import androidx.annotation.RequiresApi;

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

    @RequiresApi(VERSION_CODES.N)
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

    private static Observable<Status> createGPSObservable(final Context context) {
        return Observable.create(subscriber -> {
            final LocationManager geoManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
            final Listener listener = new Listener() {
                Status latest = NO_GNSS;

                @Override
                public void onGpsStatusChanged(final int event) {
                    switch (event) {
                        case GpsStatus.GPS_EVENT_FIRST_FIX:
                        case GpsStatus.GPS_EVENT_SATELLITE_STATUS: {
                            final GpsStatus status = geoManager.getGpsStatus(null);
                            int visible = 0;
                            int fixed = 0;
                            for (final GpsSatellite satellite : status.getSatellites()) {
                                if (satellite.usedInFix()) {
                                    fixed++;
                                }
                                visible++;
                            }
                            if (visible == latest.satellitesVisible && fixed == latest.satellitesFixed) {
                                return;
                            }
                            latest = new Status(true, visible, fixed);
                            break;
                        }
                        case GpsStatus.GPS_EVENT_STARTED:
                            latest = new Status(true, 0, 0);
                            break;
                        case GpsStatus.GPS_EVENT_STOPPED:
                            latest = NO_GNSS;
                            break;
                        default:
                            subscriber.onError(new IllegalStateException());
                            return;
                    }
                    subscriber.onNext(latest);
                }
            };
            subscriber.onNext(NO_GNSS);
            if (PermissionContext.LOCATION.hasAllPermissions()) {
                Log.d("GnssStatusProvider.createGPSObservable: registering callback");
                geoManager.addGpsStatusListener(listener);
            } else {
                Log.d("GnssStatusProvider.createGPSObservable: Could not register provider, no Location permission available");
            }
            subscriber.setDisposable(AndroidRxUtils.disposeOnCallbacksScheduler(() -> geoManager.removeGpsStatusListener(listener)));
        });
    }

    public static Observable<Status> create(final Context context) {
        final Observable<Status> observable;
        if (VERSION.SDK_INT >= VERSION_CODES.N) {
            observable = createGNSSObservable(context);
        } else {
            observable = createGPSObservable(context);
        }

        return observable.subscribeOn(AndroidRxUtils.looperCallbacksScheduler);
    }
}
