package cgeo.geocaching.sensors;

import cgeo.geocaching.utils.RxUtils;

import rx.Observable;
import rx.Observable.OnSubscribe;
import rx.Subscriber;
import rx.functions.Action0;
import rx.subscriptions.Subscriptions;

import android.content.Context;
import android.location.GpsSatellite;
import android.location.GpsStatus;
import android.location.GpsStatus.Listener;
import android.location.LocationManager;

public class GpsStatusProvider {

    private GpsStatusProvider() {
        // Utility class, not to be instantiated
    }

    public static class Status {
        final public boolean gpsEnabled;
        final public int satellitesVisible;
        final public int satellitesFixed;

        public Status(final boolean gpsEnabled, final int satellitesVisible, final int satellitesFixed) {
            this.gpsEnabled = gpsEnabled;
            this.satellitesVisible = satellitesVisible;
            this.satellitesFixed = satellitesFixed;
        }
    }

    private static final Status NO_GPS = new Status(false, 0, 0);

    public static Observable<Status> create(final Context context) {
        final Observable<Status> observable = Observable.create(new OnSubscribe<Status>() {
            @Override
            public void call(final Subscriber<? super Status> subscriber) {
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
                subscriber.add(Subscriptions.create(new Action0() {
                    @Override
                    public void call() {
                        RxUtils.looperCallbacksWorker.schedule(new Action0() {
                            @Override
                            public void call() {
                                geoManager.removeGpsStatusListener(listener);
                            }
                        });
                    }
                }));
            }
        });
        return observable.subscribeOn(RxUtils.looperCallbacksScheduler);
    }
}
