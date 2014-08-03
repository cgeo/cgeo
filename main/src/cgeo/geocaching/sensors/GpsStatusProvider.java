package cgeo.geocaching.sensors;

import cgeo.geocaching.sensors.GpsStatusProvider.Status;
import cgeo.geocaching.utils.Log;
import cgeo.geocaching.utils.RxUtils;

import rx.Observable;
import rx.Observable.OnSubscribe;
import rx.Subscriber;
import rx.Subscription;
import rx.functions.Action0;
import rx.functions.Action1;
import rx.observables.ConnectableObservable;
import rx.subjects.BehaviorSubject;

import android.content.Context;
import android.location.GpsSatellite;
import android.location.GpsStatus;
import android.location.LocationManager;

import java.util.concurrent.atomic.AtomicInteger;

public class GpsStatusProvider implements OnSubscribe<Status> {

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

    private final LocationManager geoManager;
    private final BehaviorSubject<Status> subject;

    private Status latest = new Status(false, 0, 0);

    /**
     * Build a new gps status provider object.
     * <p/>
     * There is no need to instantiate more than one such object in an application, as observers can be added
     * at will.
     *
     * @param context the context used to retrieve the system services
     */
    protected GpsStatusProvider(final Context context) {
        geoManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        subject = BehaviorSubject.create(latest);
    }

    public static Observable<Status> create(final Context context) {
        final GpsStatusProvider provider = new GpsStatusProvider(context);
        return provider.worker.refCount();
    }

    @Override
    public void call(final Subscriber<? super Status> subscriber) {
        subject.subscribe(subscriber);
    }

    final ConnectableObservable<Status> worker = new ConnectableObservable<Status>(this) {
        private final AtomicInteger count = new AtomicInteger(0);
        private final GpsStatus.Listener gpsStatusListener = new GpsStatusListener();

        @Override
        public void connect(Action1<? super Subscription> connection) {
            connection.call(RxUtils.looperCallbacksSchedule(count,
                    new Action0() {
                        @Override
                        public void call() {
                            Log.d("GpsStatusProvider: starting the GPS status listener");
                            geoManager.addGpsStatusListener(gpsStatusListener);
                        }
                    },
                    new Action0() {
                        @Override
                        public void call() {
                            Log.d("GpsStatusProvider: stopping the GPS status listener");
                            geoManager.removeGpsStatusListener(gpsStatusListener);
                        }
                    }));
        }
    };

    private final class GpsStatusListener implements GpsStatus.Listener {

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
                    latest = new Status(latest.gpsEnabled, visible, fixed);
                    break;
                }
                case GpsStatus.GPS_EVENT_STARTED:
                    latest = new Status(true, latest.satellitesVisible, latest.satellitesFixed);
                    break;
                case GpsStatus.GPS_EVENT_STOPPED:
                    latest = new Status(false, 0, 0);
                    break;
                default:
                    throw new IllegalStateException();
            }

            subject.onNext(latest);
        }
    }

}
