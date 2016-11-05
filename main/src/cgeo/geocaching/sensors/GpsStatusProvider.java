package cgeo.geocaching.sensors;

import cgeo.geocaching.utils.AndroidRxUtils;

import android.content.Context;
import android.location.GpsSatellite;
import android.location.GpsStatus;
import android.location.GpsStatus.Listener;
import android.location.LocationManager;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;

public class GpsStatusProvider {

    private GpsStatusProvider() {
        // Utility class, not to be instantiated
    }

    public static class Status {
        public final boolean gpsEnabled;
        public final int satellitesVisible;
        public final int satellitesFixed;

        public Status(final boolean gpsEnabled, final int satellitesVisible, final int satellitesFixed) {
            this.gpsEnabled = gpsEnabled;
            this.satellitesVisible = satellitesVisible;
            this.satellitesFixed = satellitesFixed;
        }
    }

    private static final Status NO_GPS = new Status(false, 0, 0);

    public static Observable<Status> create(final Context context) {
        final Observable<Status> observable = Observable.create(new ObservableOnSubscribe<Status>() {
            @Override
            public void subscribe(final ObservableEmitter<Status> subscriber) throws Exception {
                final LocationManager geoManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
                final Listener listener = new Listener() {
                    Status latest = new Status(false, 0, 0);

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
                                latest = new Status(false, 0, 0);
                                break;
                            default:
                                subscriber.onError(new IllegalStateException());
                                return;
                        }
                        subscriber.onNext(latest);
                    }
                };
                subscriber.onNext(NO_GPS);
                geoManager.addGpsStatusListener(listener);
                subscriber.setDisposable(AndroidRxUtils.disposeOnCallbacksScheduler(new Runnable() {
                    @Override
                    public void run() {
                        geoManager.removeGpsStatusListener(listener);
                    }
                }));
            }
        });
        return observable.subscribeOn(AndroidRxUtils.looperCallbacksScheduler);
    }
}
