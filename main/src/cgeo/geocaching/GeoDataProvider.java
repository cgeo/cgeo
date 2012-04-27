package cgeo.geocaching;

import cgeo.geocaching.enumerations.LocationProviderType;
import cgeo.geocaching.geopoint.Geopoint;
import cgeo.geocaching.go4cache.Go4Cache;
import cgeo.geocaching.utils.Log;
import cgeo.geocaching.utils.MemorySubject;

import android.content.Context;
import android.location.GpsSatellite;
import android.location.GpsStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * Provide information about the user location. This class should be instantiated only once per application.
 */
class GeoDataProvider extends MemorySubject<IGeoData> {

    private static final String LAST_LOCATION_PSEUDO_PROVIDER = "last";
    private final LocationManager geoManager;
    private final GpsStatus.Listener gpsStatusListener = new GpsStatusListener();
    private LocationData gpsLocation = new LocationData(LocationProviderType.GPS);
    private LocationData netLocation = new LocationData(LocationProviderType.NETWORK);
    private final Listener networkListener = new Listener(LocationManager.NETWORK_PROVIDER, netLocation);
    private final Listener gpsListener = new Listener(LocationManager.GPS_PROVIDER, gpsLocation);
    private GeoData current = new GeoData();
    private final Unregisterer unregisterer = new Unregisterer();

    private static class LocationData {
        public Location location;
        public LocationProviderType provider;
        public long timestamp = 0;

        LocationData(final LocationProviderType provider) {
            this.provider = provider;
        }

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

    private static class GeoData implements Cloneable, IGeoData {
        public Location location = null;
        public int satellitesVisible = 0;
        public int satellitesFixed = 0;
        public boolean gpsEnabled = false;

        @Override
        public Location getLocation() {
            return location;
        }

        private LocationProviderType getLocationProviderType(final String provider) {
            if (provider.equals(LocationManager.GPS_PROVIDER)) {
                return LocationProviderType.GPS;
            }
            if (provider.equals(LocationManager.NETWORK_PROVIDER)) {
                return LocationProviderType.NETWORK;
            }
            return LocationProviderType.LAST;
        }

        @Override
        public LocationProviderType getLocationProvider() {
            return getLocationProviderType(location != null ? location.getProvider() : LAST_LOCATION_PSEUDO_PROVIDER);
        }

        @Override
        public Geopoint getCoords() {
            return new Geopoint(location);
        }

        @Override
        public double getAltitude() {
            return location.getAltitude();
        }

        @Override
        public float getBearing() {
            return location.getBearing();
        }

        @Override
        public boolean getGpsEnabled() {
            return gpsEnabled;
        }

        @Override
        public float getSpeed() {
            return location.getSpeed();
        }

        @Override
        public float getAccuracy() {
            return location.getAccuracy();
        }

        @Override
        public int getSatellitesVisible() {
            return satellitesVisible;
        }

        @Override
        public int getSatellitesFixed() {
            return satellitesFixed;
        }

        public IGeoData makeImmutable() {
            try {
                return (IGeoData) clone();
            } catch (final CloneNotSupportedException e) {
                // This cannot happen
                return null;
            }
        }
    }

    private class Unregisterer extends Thread {

        private boolean unregisterRequested = false;
        private ArrayBlockingQueue<Boolean> queue = new ArrayBlockingQueue<Boolean>(1);

        public void cancelUnregister() {
            try {
                queue.put(false);
            } catch (final InterruptedException e) {
                // Do nothing
            }
        }

        public void lateUnregister() {
            try {
                queue.put(true);
            } catch (final InterruptedException e) {
                // Do nothing
            }
        }

        @Override
        public void run() {
            try {
                while (true) {
                    if (unregisterRequested) {
                        final Boolean element = queue.poll(2500, TimeUnit.MILLISECONDS);
                        if (element == null) {
                            // Timeout
                            unregisterListeners();
                            unregisterRequested = false;
                        } else {
                            unregisterRequested = element;
                        }
                    } else {
                        unregisterRequested = queue.take();
                    }
                }
            } catch (final InterruptedException e) {
                // Do nothing
            }
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
    GeoDataProvider(final Context context) {
        geoManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        unregisterer.start();
        // Start with an empty GeoData just in case someone queries it before we get
        // a chance to get any information.
        notifyObservers(new GeoData());
    }

    private void registerListeners() {
        geoManager.addGpsStatusListener(gpsStatusListener);

        for (final Listener listener : new Listener[] { networkListener, gpsListener }) {
            try {
                geoManager.requestLocationUpdates(listener.locationProvider, 0, 0, listener);
            } catch (final Exception e) {
                Log.w("There is no location provider " + listener.locationProvider);
            }
        }
    }

    private synchronized void unregisterListeners() {
        // This method must be synchronized because it will be called asynchronously from the Unregisterer thread.
        // We check that no observers have been re-added to prevent a race condition.
        if (sizeObservers() == 0) {
            geoManager.removeUpdates(networkListener);
            geoManager.removeUpdates(gpsListener);
            geoManager.removeGpsStatusListener(gpsStatusListener);
        }
    }

    @Override
    protected void onFirstObserver() {
        unregisterer.cancelUnregister();
        registerListeners();
    }

    @Override
    protected void onLastObserver() {
        unregisterer.lateUnregister();
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
                case GpsStatus.GPS_EVENT_SATELLITE_STATUS: {
                    final GpsStatus status = geoManager.getGpsStatus(null);
                    int satellites = 0;
                    int fixed = 0;
                    for (final GpsSatellite satellite : status.getSatellites()) {
                        if (satellite.usedInFix()) {
                            fixed++;
                        }
                        satellites++;
                    }
                    if (satellites != current.satellitesVisible || fixed != current.satellitesFixed) {
                        current.satellitesVisible = satellites;
                        current.satellitesFixed = fixed;
                        changed = true;
                    }
                    break;
                }
                case GpsStatus.GPS_EVENT_STARTED:
                    if (!current.gpsEnabled) {
                        current.gpsEnabled = true;
                        current.satellitesFixed = 0;
                        current.satellitesVisible = 0;
                        changed = true;
                    }
                    break;
                case GpsStatus.GPS_EVENT_STOPPED:
                    if (current.gpsEnabled) {
                        current.gpsEnabled = false;
                        current.satellitesFixed = 0;
                        current.satellitesVisible = 0;
                        changed = true;
                    }
                    break;
            }

            if (changed) {
                selectBest();
            }
        }
    }

    private LocationData best() {
        if (gpsLocation.isRecent() || !netLocation.isValid()) {
            return gpsLocation;
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
        if (locationData.location == null) {
            return;
        }

        current.location = locationData.location;
        notifyObservers(current.makeImmutable());

        Go4Cache.signalCoordinates(current.getCoords());
    }

}
