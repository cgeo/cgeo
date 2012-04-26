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

import java.util.Iterator;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * Provide information about the user location. This class should be instantiated only once per application.
 */
class GeoDataProvider extends MemorySubject<IGeoData> {

    private static final String LAST_LOCATION_PSEUDO_PROVIDER = "last";
    private final LocationManager geoManager;
    private final AbstractLocationListener networkListener = new NetworkLocationListener();
    private final AbstractLocationListener gpsListener = new GpsLocationListener();
    private final GpsStatusListener gpsStatusListener = new GpsStatusListener();
    private Location locGps = null;
    private Location locNet = null;
    private long locGpsLast = 0L;
    private GeoData current = new GeoData();
    private final Unregisterer unregisterer = new Unregisterer();

    private static class GeoData implements Cloneable, IGeoData {
        public Location location = null;
        public LocationProviderType locationProvider = LocationProviderType.LAST;
        public Geopoint coordsNow = null;
        public Double altitudeNow = null;
        public float bearingNow = 0;
        public float speedNow = 0;
        public float accuracyNow = -1f;
        public int satellitesVisible = 0;
        public int satellitesFixed = 0;
        public boolean gpsEnabled = false;

        @Override
        public Location getLocation() {
            return location;
        }

        @Override
        public LocationProviderType getLocationProvider() {
            return locationProvider;
        }

        @Override
        public Geopoint getCoordsNow() {
            return coordsNow;
        }

        @Override
        public Double getAltitudeNow() {
            return altitudeNow;
        }

        @Override
        public float getBearingNow() {
            return bearingNow;
        }

        @Override
        public boolean getGpsEnabled() {
            return gpsEnabled;
        }

        @Override
        public float getSpeedNow() {
            return speedNow;
        }

        @Override
        public float getAccuracyNow() {
            return accuracyNow;
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
    };

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
    public GeoDataProvider(final Context context) {
        geoManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        unregisterer.start();
        // Start with an empty GeoData just in case someone queries it before we get
        // a chance to get any information.
        notifyObservers(new GeoData());
    }

    private void registerListeners() {
        geoManager.addGpsStatusListener(gpsStatusListener);

        for (final AbstractLocationListener listener : new AbstractLocationListener[] { networkListener, gpsListener }) {
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

    private static abstract class AbstractLocationListener implements LocationListener {
        private final String locationProvider;

        protected AbstractLocationListener(final String provider) {
            locationProvider = provider;
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
    }

    private final class GpsLocationListener extends AbstractLocationListener {

        public GpsLocationListener() {
            super(LocationManager.GPS_PROVIDER);
        }

        @Override
        public void onLocationChanged(final Location location) {
            locGps = location;
            locGpsLast = System.currentTimeMillis();
            selectBest(location.getProvider());
        }
    }

    private final class NetworkLocationListener extends AbstractLocationListener {

        protected NetworkLocationListener() {
            super(LocationManager.NETWORK_PROVIDER);
        }

        @Override
        public void onLocationChanged(final Location location) {
            locNet = location;
            selectBest(location.getProvider());
        }

    }

    private final class GpsStatusListener implements GpsStatus.Listener {

        @Override
        public void onGpsStatusChanged(final int event) {
            boolean changed = false;
            if (event == GpsStatus.GPS_EVENT_SATELLITE_STATUS) {
                final GpsStatus status = geoManager.getGpsStatus(null);
                final Iterator<GpsSatellite> statusIterator = status.getSatellites().iterator();

                int satellites = 0;
                int fixed = 0;

                while (statusIterator.hasNext()) {
                    final GpsSatellite sat = statusIterator.next();
                    if (sat.usedInFix()) {
                        fixed++;
                    }
                    satellites++;
                }

                if (satellites != current.satellitesVisible) {
                    current.satellitesVisible = satellites;
                    changed = true;
                }
                if (fixed != current.satellitesFixed) {
                    current.satellitesFixed = fixed;
                    changed = true;
                }
            } else if (event == GpsStatus.GPS_EVENT_STARTED && !current.gpsEnabled) {
                current.gpsEnabled = true;
                current.satellitesFixed = 0;
                current.satellitesVisible = 0;
                changed = true;
            } else if (event == GpsStatus.GPS_EVENT_STOPPED && current.gpsEnabled) {
                current.gpsEnabled = false;
                current.satellitesFixed = 0;
                current.satellitesVisible = 0;
                changed = true;
            }

            if (changed) {
                selectBest(null);
            }
        }
    }

    private void selectBest(final String signallingProvider) {
        if (locNet != null && locGps == null) { // we have only NET
            assign(locNet);
        }
        else if ((locNet == null && locGps != null) // we have only GPS
                || (current.satellitesFixed > 0) // GPS seems to be fixed
                || (signallingProvider != null && signallingProvider.equals(LocationManager.GPS_PROVIDER)) // we have new location from GPS
                || locGpsLast > System.currentTimeMillis() - 30 * 1000 // GPS was working in last 30 seconds
        ) {
            assign(locGps);
        }
        else {
            assign(locNet); // nothing else, using NET
        }
        notifyObservers(current.makeImmutable());
    }

    private void assignLastLocation(final Geopoint coords) {
        if (coords == null) {
            return;
        }

        current.locationProvider = LocationProviderType.LAST;
        current.coordsNow = coords;
        current.altitudeNow = null;
        current.bearingNow = 0f;
        current.speedNow = 0f;
        current.accuracyNow = 999f;

        notifyObservers(current.makeImmutable());
    }

    private void assign(final Location loc) {
        if (loc == null) {
            current.locationProvider = LocationProviderType.LAST;
            return;
        }

        current.location = loc;

        final String provider = current.location.getProvider();
        if (provider.equals(LocationManager.GPS_PROVIDER)) {
            current.locationProvider = LocationProviderType.GPS;
        } else if (provider.equals(LocationManager.NETWORK_PROVIDER)) {
            current.locationProvider = LocationProviderType.NETWORK;
        } else if (provider.equalsIgnoreCase(LAST_LOCATION_PSEUDO_PROVIDER)) {
            current.locationProvider = LocationProviderType.LAST;
        }

        current.coordsNow = new Geopoint(current.location.getLatitude(), current.location.getLongitude());

        final Location location = current.location;
        final LocationProviderType locationProvider = current.locationProvider;
        current.altitudeNow = location.hasAltitude() && locationProvider != LocationProviderType.LAST ? location.getAltitude() + Settings.getAltCorrection() : null;
        current.bearingNow = location.hasBearing() && locationProvider != LocationProviderType.LAST ? location.getBearing() : 0f;
        current.speedNow = location.hasSpeed() && locationProvider != LocationProviderType.LAST ? location.getSpeed() : 0f;
        current.accuracyNow = location.hasAccuracy() && locationProvider != LocationProviderType.LAST ? location.getAccuracy() : 999f;

        notifyObservers(current.makeImmutable());

        if (locationProvider == LocationProviderType.GPS || locationProvider == LocationProviderType.NETWORK) {
            // FIXME: should use observer pattern as well
            Go4Cache.signalCoordinates(current.coordsNow);
        }
    }

}
