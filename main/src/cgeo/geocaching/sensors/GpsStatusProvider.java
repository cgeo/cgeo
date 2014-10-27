package cgeo.geocaching.sensors;

import cgeo.geocaching.sensors.GpsStatusProvider.Status;
import cgeo.geocaching.utils.Log;
import cgeo.geocaching.utils.RxUtils.LooperCallbacks;

import rx.Observable;

import android.content.Context;
import android.location.GpsSatellite;
import android.location.GpsStatus;
import android.location.LocationManager;

public class GpsStatusProvider extends LooperCallbacks<Status> {

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
    private final GpsStatus.Listener gpsStatusListener = new GpsStatusListener();
    private Status latest = new Status(false, 0, 0);

    private static final Status NO_GPS = new Status(false, 0, 0);

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
    }

    public static Observable<Status> create(final Context context) {
        return Observable.create(new GpsStatusProvider(context));
    }

    @Override
    protected void onStart() {
        Log.d("GpsStatusProvider: starting the GPS status listener");
        subject.onNext(NO_GPS);
        geoManager.addGpsStatusListener(gpsStatusListener);
    }

    @Override
    protected void onStop() {
        Log.d("GpsStatusProvider: stopping the GPS status listener");
        geoManager.removeGpsStatusListener(gpsStatusListener);
    }

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
                    throw new IllegalStateException();
            }

            subject.onNext(latest);
        }
    }

}
