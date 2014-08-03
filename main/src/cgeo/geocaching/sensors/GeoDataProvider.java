package cgeo.geocaching.sensors;

import cgeo.geocaching.utils.Log;
import cgeo.geocaching.utils.StartableHandlerThread;

import rx.Observable;
import rx.Observable.OnSubscribe;
import rx.Subscriber;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action0;
import rx.functions.Action1;
import rx.observables.ConnectableObservable;
import rx.subjects.BehaviorSubject;
import rx.subscriptions.CompositeSubscription;
import rx.subscriptions.Subscriptions;

import android.content.Context;
import android.location.GpsSatellite;
import android.location.GpsStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;

import java.util.concurrent.TimeUnit;

public class GeoDataProvider implements OnSubscribe<IGeoData> {

    private final LocationManager geoManager;
    private final LocationData gpsLocation = new LocationData();
    private final LocationData netLocation = new LocationData();
    private final BehaviorSubject<IGeoData> subject;
    private static final StartableHandlerThread handlerThread =
            new StartableHandlerThread("GeoDataProvider thread", android.os.Process.THREAD_PRIORITY_BACKGROUND);
    static {
        handlerThread.start();
    }

    public boolean gpsEnabled = false;
    public int satellitesVisible = 0;
    public int satellitesFixed = 0;

    private static class LocationData {
        public Location location;
        public long timestamp = 0;

        public void update(final Location location) {
            this.location = location;
            timestamp = System.currentTimeMillis();
        }

        public boolean isRecent() {
            return isValid() && System.currentTimeMillis() < timestamp + 30000;
        }

        public boolean isValid() {
            return location != null;
        }
    }

    /**
     * Build a new geo data provider object.
     * <p/>
     * There is no need to instantiate more than one such object in an application, as observers can be added
     * at will.
     *
     * @param context the context used to retrieve the system services
     */
    protected GeoDataProvider(final Context context) {
        geoManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        subject = BehaviorSubject.create(findInitialLocation());
    }

    public static Observable<IGeoData> create(final Context context) {
        final GeoDataProvider provider = new GeoDataProvider(context);
        return provider.worker.refCount();
    }

    @Override
    public void call(final Subscriber<? super IGeoData> subscriber) {
        subject.subscribe(subscriber);
    }

    final ConnectableObservable<IGeoData> worker = new ConnectableObservable<IGeoData>(this) {
        private int debugSessionCounter = 0;

        private final Object lock = new Object();
        private int count = 0;

        final private GpsStatus.Listener gpsStatusListener = new GpsStatusListener();
        final private Listener networkListener = new Listener(LocationManager.NETWORK_PROVIDER, netLocation);
        final private Listener gpsListener = new Listener(LocationManager.GPS_PROVIDER, gpsLocation);

        @Override
        public void connect(Action1<? super Subscription> connection) {
            final CompositeSubscription subscription = new CompositeSubscription();
            AndroidSchedulers.handlerThread(handlerThread.getHandler()).createWorker().schedule(new Action0() {
                @Override
                public void call() {
                    synchronized(lock) {
                        if (count++ == 0) {
                            Log.d("GeoDataProvider: starting the GPS and network listeners" + " (" + ++debugSessionCounter + ")");
                            geoManager.addGpsStatusListener(gpsStatusListener);
                            for (final Listener listener : new Listener[] { networkListener, gpsListener }) {
                                try {
                                    geoManager.requestLocationUpdates(listener.locationProvider, 0, 0, listener);
                                } catch (final Exception e) {
                                    Log.w("There is no location provider " + listener.locationProvider);
                                }
                            }
                        }
                    }

                    subscription.add(Subscriptions.create(new Action0() {
                        @Override
                        public void call() {
                            AndroidSchedulers.handlerThread(handlerThread.getHandler()).createWorker().schedule(new Action0() {
                                @Override
                                public void call() {
                                    synchronized (lock) {
                                        if (--count == 0) {
                                            Log.d("GeoDataProvider: stopping the GPS and network listeners" + " (" + debugSessionCounter + ")");
                                            geoManager.removeUpdates(networkListener);
                                            geoManager.removeUpdates(gpsListener);
                                            geoManager.removeGpsStatusListener(gpsStatusListener);
                                        }
                                    }
                                }
                            }, 2500, TimeUnit.MILLISECONDS);
                        }
                    }));
                }
            });
            connection.call(subscription);
        }
    };

    private IGeoData findInitialLocation() {
        final Location initialLocation = new Location("initial");
        try {
            // Try to find a sensible initial location from the last locations known to Android.
            final Location lastGpsLocation = geoManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            final Location lastNetworkLocation = geoManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);

            // If both providers are non-null, take the most recent one
            if (lastGpsLocation != null && lastNetworkLocation != null) {
                if (lastGpsLocation.getTime() >= lastNetworkLocation.getTime()) {
                    copyCoords(initialLocation, lastGpsLocation);
                } else {
                    copyCoords(initialLocation, lastNetworkLocation);
                }
            } else if (lastGpsLocation != null) {
                copyCoords(initialLocation, lastGpsLocation);
            } else if (lastNetworkLocation != null) {
                copyCoords(initialLocation, lastNetworkLocation);
            } else {
                Log.i("GeoDataProvider: no last known location available");
            }
        } catch (final Exception e) {
            // This error is non-fatal as its only consequence is that we will start with a dummy location
            // instead of a previously known one.
            Log.e("GeoDataProvider: error when retrieving last known location", e);
        }
        // Start with an historical GeoData just in case someone queries it before we get
        // a chance to get any information.
        return new GeoData(initialLocation, false, 0, 0);
    }

    private static void copyCoords(final Location target, final Location source) {
        target.setLatitude(source.getLatitude());
        target.setLongitude(source.getLongitude());
    }

    private class Listener implements LocationListener {
        private final String locationProvider;
        private final LocationData locationData;

        Listener(final String locationProvider, final LocationData locationData) {
            this.locationProvider = locationProvider;
            this.locationData = locationData;
        }

        @Override
        public void onStatusChanged(final String provider, final int status, final Bundle extras) {
            // nothing
        }

        @Override
        public void onProviderDisabled(final String provider) {
            // nothing
        }

        @Override
        public void onProviderEnabled(final String provider) {
            // nothing
        }

        @Override
        public void onLocationChanged(final Location location) {
            locationData.update(location);
            selectBest();
        }
    }

    private final class GpsStatusListener implements GpsStatus.Listener {

        @Override
        public void onGpsStatusChanged(final int event) {
            boolean changed = false;
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
                    if (visible != satellitesVisible || fixed != satellitesFixed) {
                        satellitesVisible = visible;
                        satellitesFixed = fixed;
                        changed = true;
                    }
                    break;
                }
                case GpsStatus.GPS_EVENT_STARTED:
                    if (!gpsEnabled) {
                        gpsEnabled = true;
                        changed = true;
                    }
                    break;
                case GpsStatus.GPS_EVENT_STOPPED:
                    if (gpsEnabled) {
                        gpsEnabled = false;
                        satellitesFixed = 0;
                        satellitesVisible = 0;
                        changed = true;
                    }
                    break;
                default:
                    throw new IllegalStateException();
            }

            if (changed) {
                selectBest();
            }
        }
    }

    private LocationData best() {
        if (gpsLocation.isRecent() || !netLocation.isValid()) {
            return gpsLocation.isValid() ? gpsLocation : null;
        }
        if (!gpsLocation.isValid()) {
            return netLocation;
        }
        return gpsLocation.timestamp > netLocation.timestamp ? gpsLocation : netLocation;
    }

    private void selectBest() {
        assign(best());
    }

    private void assign(final LocationData locationData) {
        if (locationData == null) {
            return;
        }

        // We do not necessarily get signalled when satellites go to 0/0.
        final int visible = gpsLocation.isRecent() ? satellitesVisible : 0;
        final IGeoData current = new GeoData(locationData.location, gpsEnabled, visible, satellitesFixed);
        subject.onNext(current);
    }

}
