package cgeo.geocaching;

import cgeo.geocaching.enumerations.LocationProviderType;
import cgeo.geocaching.geopoint.Geopoint;
import cgeo.geocaching.go4cache.Go4Cache;

import android.content.Context;
import android.location.GpsSatellite;
import android.location.GpsStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;

import java.util.Iterator;

public class cgGeo {

    private static final String LAST_LOCATION_PSEUDO_PROVIDER = "last";
    private final LocationManager geoManager = (LocationManager) cgeoapplication.getInstance().getSystemService(Context.LOCATION_SERVICE);
    private UpdateLocationCallback updateLocationCallback = null;
    private final AbstractLocationListener networkListener = new NetworkLocationListener();
    private final AbstractLocationListener gpsListener = new GpsLocationListener();
    private final GpsStatusListener gpsStatusListener = new GpsStatusListener();
    private Location locGps = null;
    private Location locNet = null;
    private long locGpsLast = 0L;
    public Location location = null;
    public LocationProviderType locationProvider = LocationProviderType.LAST;
    public Geopoint coordsNow = null;
    public Double altitudeNow = null;
    public float bearingNow = 0;
    public float speedNow = 0;
    public float accuracyNow = -1f;
    public int satellitesVisible = 0;
    public int satellitesFixed = 0;

    public cgGeo() {
        restoreLastLocation();

        geoManager.addGpsStatusListener(gpsStatusListener);

        for (AbstractLocationListener listener : new AbstractLocationListener[] { networkListener, gpsListener }) {
            try {
                geoManager.requestLocationUpdates(listener.locationProvider, 0, 0, listener);
            } catch (Exception e) {
                Log.w(Settings.tag, "There is no location provider " + listener.locationProvider);
            }
        }
    }

    public void closeGeo() {
        geoManager.removeUpdates(networkListener);
        geoManager.removeUpdates(gpsListener);
        geoManager.removeGpsStatusListener(gpsStatusListener);
    }

    public void replaceUpdate(UpdateLocationCallback callback) {
        updateLocationCallback = callback;
        fireLocationCallback();
    }

    private void fireLocationCallback() {
        if (updateLocationCallback != null) {
            updateLocationCallback.updateLocation(this);
        }
    }

    private abstract class AbstractLocationListener implements LocationListener {
        private final String locationProvider;

        protected AbstractLocationListener(String provider) {
            this.locationProvider = provider;
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
            // nothing
        }

        @Override
        public void onProviderDisabled(String provider) {
            if (provider.equals(locationProvider)) {
                geoManager.removeUpdates(this);
            }
        }

        @Override
        public void onProviderEnabled(String provider) {
            // nothing
        }
    }

    private final class GpsLocationListener extends AbstractLocationListener {

        public GpsLocationListener() {
            super(LocationManager.GPS_PROVIDER);
        }

        @Override
        public void onLocationChanged(Location location) {
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
        public void onLocationChanged(Location location) {
            locNet = location;
            selectBest(location.getProvider());
        }

    }

    private final class GpsStatusListener implements GpsStatus.Listener {

        @Override
        public void onGpsStatusChanged(int event) {
            if (event == GpsStatus.GPS_EVENT_SATELLITE_STATUS) {
                final GpsStatus status = geoManager.getGpsStatus(null);
                final Iterator<GpsSatellite> statusIterator = status.getSatellites().iterator();

                int satellites = 0;
                int fixed = 0;

                while (statusIterator.hasNext()) {
                    GpsSatellite sat = statusIterator.next();
                    if (sat.usedInFix()) {
                        fixed++;
                    }
                    satellites++;
                }

                boolean changed = false;
                if (satellites != satellitesVisible) {
                    satellitesVisible = satellites;
                    changed = true;
                }
                if (fixed != satellitesFixed) {
                    satellitesFixed = fixed;
                    changed = true;
                }

                if (changed) {
                    selectBest(null);
                }
            }
        }
    }

    private void selectBest(final String signallingProvider) {
        if (locNet != null && locGps == null) { // we have only NET
            assign(locNet);
        }
        else if ((locNet == null && locGps != null) // we have only GPS
                || (satellitesFixed > 0) // GPS seems to be fixed
                || (signallingProvider != null && signallingProvider.equals(LocationManager.GPS_PROVIDER)) // we have new location from GPS
                || locGpsLast > (System.currentTimeMillis() - 30 * 1000) // GPS was working in last 30 seconds
        ) {
            assign(locGps);
        }
        else {
            assign(locNet); // nothing else, using NET
        }
    }

    private void assignLastLocation(final Geopoint coords) {
        if (coords == null) {
            return;
        }

        locationProvider = LocationProviderType.LAST;
        coordsNow = coords;
        altitudeNow = null;
        bearingNow = 0f;
        speedNow = 0f;
        accuracyNow = 999f;

        fireLocationCallback();
    }

    private void assign(final Location loc) {
        if (loc == null) {
            locationProvider = LocationProviderType.LAST;
            return;
        }

        location = loc;

        final String provider = location.getProvider();
        if (provider.equals(LocationManager.GPS_PROVIDER)) {
            locationProvider = LocationProviderType.GPS;
        } else if (provider.equals(LocationManager.NETWORK_PROVIDER)) {
            locationProvider = LocationProviderType.NETWORK;
        } else if (provider.equalsIgnoreCase(LAST_LOCATION_PSEUDO_PROVIDER)) {
            locationProvider = LocationProviderType.LAST;
        }

        coordsNow = new Geopoint(location.getLatitude(), location.getLongitude());
        cgeoapplication.getInstance().setLastCoords(coordsNow);

        if (location.hasAltitude() && locationProvider != LocationProviderType.LAST) {
            altitudeNow = location.getAltitude() + Settings.getAltCorrection();
        } else {
            altitudeNow = null;
        }
        if (location.hasBearing() && locationProvider != LocationProviderType.LAST) {
            bearingNow = location.getBearing();
        } else {
            bearingNow = 0f;
        }
        if (location.hasSpeed() && locationProvider != LocationProviderType.LAST) {
            speedNow = location.getSpeed();
        } else {
            speedNow = 0f;
        }
        if (location.hasAccuracy() && locationProvider != LocationProviderType.LAST) {
            accuracyNow = location.getAccuracy();
        } else {
            accuracyNow = 999f;
        }

        fireLocationCallback();

        if (locationProvider == LocationProviderType.GPS || locationProvider == LocationProviderType.NETWORK) {
            Go4Cache.signalCoordinates(coordsNow);
        }
    }

    private void restoreLastLocation() {
        // restore from last location (stored by app)
        assignLastLocation(cgeoapplication.getInstance().getLastCoords());

        // restore from last location (stored by device sensors)
        for (String provider : new String[] { LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER }) {
            final Location lastLocation = geoManager.getLastKnownLocation(provider);
            if (lastLocation != null) {
                lastLocation.setProvider(LAST_LOCATION_PSEUDO_PROVIDER);
                assign(lastLocation);

                Log.i(Settings.tag, "Using last location from " + provider);
                break;
            }
        }
    }
}
