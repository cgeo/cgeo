package cgeo.geocaching;

import cgeo.geocaching.geopoint.Geopoint;
import cgeo.geocaching.utils.CryptUtils;

import org.apache.commons.lang3.StringUtils;

import android.content.Context;
import android.content.SharedPreferences;
import android.location.GpsSatellite;
import android.location.GpsStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;

import java.util.Iterator;
import java.util.Locale;

public class cgGeo {

    private Context context = null;
    private cgeoapplication app = null;
    private LocationManager geoManager = null;
    private cgUpdateLoc geoUpdate = null;
    private cgBase base = null;
    private SharedPreferences prefs = null;
    private cgeoGeoListener geoNetListener = null;
    private cgeoGeoListener geoGpsListener = null;
    private cgeoGpsStatusListener geoGpsStatusListener = null;
    private Integer time = 0;
    private Integer distance = 0;
    private Location locGps = null;
    private Location locNet = null;
    private long locGpsLast = 0L;
    private boolean g4cRunning = false;
    private Geopoint lastGo4cacheCoords = null;
    public Location location = null;
    public int gps = -1;
    public Geopoint coordsNow = null;
    public Geopoint coordsBefore = null;
    public Double altitudeNow = null;
    public Float bearingNow = null;
    public Float speedNow = null;
    public Float accuracyNow = null;
    public Integer satellitesVisible = null;
    public Integer satellitesFixed = null;
    public double distanceNow = 0d;

    public cgGeo(Context contextIn, cgeoapplication appIn, cgUpdateLoc geoUpdateIn, cgBase baseIn, int timeIn, int distanceIn) {
        context = contextIn;
        app = appIn;
        geoUpdate = geoUpdateIn;
        base = baseIn;
        time = timeIn;
        distance = distanceIn;

        if (prefs == null) {
            prefs = context.getSharedPreferences(Settings.preferences, 0);
        }
        distanceNow = prefs.getFloat("dst", 0f);
        if (Double.isNaN(distanceNow)) {
            distanceNow = 0d;
        }
        if (distanceNow == 0f) {
            final SharedPreferences.Editor prefsEdit = context.getSharedPreferences(Settings.preferences, 0).edit();
            if (prefsEdit != null) {
                prefsEdit.putLong("dst-since", System.currentTimeMillis());
                prefsEdit.commit();
            }
        }

        geoNetListener = new cgeoGeoListener();
        geoNetListener.setProvider(LocationManager.NETWORK_PROVIDER);

        geoGpsListener = new cgeoGeoListener();
        geoGpsListener.setProvider(LocationManager.GPS_PROVIDER);

        geoGpsStatusListener = new cgeoGpsStatusListener();
    }

    public void initGeo() {
        location = null;
        gps = -1;
        coordsNow = null;
        altitudeNow = null;
        bearingNow = null;
        speedNow = null;
        accuracyNow = null;
        satellitesVisible = 0;
        satellitesFixed = 0;

        if (geoManager == null) {
            geoManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        }

        lastLoc();

        geoNetListener.setProvider(LocationManager.NETWORK_PROVIDER);
        geoGpsListener.setProvider(LocationManager.GPS_PROVIDER);
        geoManager.addGpsStatusListener(geoGpsStatusListener);

        try {
            geoManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, time, distance, geoNetListener);
        } catch (Exception e) {
            Log.e(Settings.tag, "There is no NETWORK location provider");
        }

        try {
            geoManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, time, distance, geoGpsListener);
        } catch (Exception e) {
            Log.e(Settings.tag, "There is no GPS location provider");
        }
    }

    public void closeGeo() {
        if (geoManager != null && geoNetListener != null) {
            geoManager.removeUpdates(geoNetListener);
        }
        if (geoManager != null && geoGpsListener != null) {
            geoManager.removeUpdates(geoGpsListener);
        }
        if (geoManager != null) {
            geoManager.removeGpsStatusListener(geoGpsStatusListener);
        }

        final SharedPreferences.Editor prefsEdit = context.getSharedPreferences(Settings.preferences, 0).edit();
        if (prefsEdit != null && !Double.isNaN(distanceNow)) {
            prefsEdit.putFloat("dst", (float) distanceNow);
            prefsEdit.commit();
        }
    }

    public void replaceUpdate(cgUpdateLoc geoUpdateIn) {
        geoUpdate = geoUpdateIn;

        if (geoUpdate != null) {
            geoUpdate.updateLoc(this);
        }
    }

    public class cgeoGeoListener implements LocationListener {

        public String active = null;

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
            // nothing
        }

        @Override
        public void onLocationChanged(Location location) {
            if (location.getProvider().equals(LocationManager.GPS_PROVIDER)) {
                locGps = location;
                locGpsLast = System.currentTimeMillis();
            } else if (location.getProvider().equals(LocationManager.NETWORK_PROVIDER)) {
                locNet = location;
            }

            selectBest(location.getProvider());
        }

        @Override
        public void onProviderDisabled(String provider) {
            if (provider.equals(LocationManager.NETWORK_PROVIDER)) {
                if (geoManager != null && geoNetListener != null) {
                    geoManager.removeUpdates(geoNetListener);
                }
            } else if (provider.equals(LocationManager.GPS_PROVIDER)) {
                if (geoManager != null && geoGpsListener != null) {
                    geoManager.removeUpdates(geoGpsListener);
                }
            }
        }

        @Override
        public void onProviderEnabled(String provider) {
            if (provider.equals(LocationManager.NETWORK_PROVIDER)) {
                if (geoNetListener == null) {
                    geoNetListener = new cgeoGeoListener();
                }
                geoNetListener.setProvider(LocationManager.NETWORK_PROVIDER);
            } else if (provider.equals(LocationManager.GPS_PROVIDER)) {
                if (geoGpsListener == null) {
                    geoGpsListener = new cgeoGeoListener();
                }
                geoGpsListener.setProvider(LocationManager.GPS_PROVIDER);
            }
        }

        public void setProvider(String provider) {
            if (provider.equals(LocationManager.GPS_PROVIDER)) {
                if (geoManager != null && geoManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                    active = provider;
                } else {
                    active = null;
                }
            } else if (provider.equals(LocationManager.NETWORK_PROVIDER)) {
                if (geoManager != null && geoManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                    active = provider;
                } else {
                    active = null;
                }
            }
        }
    }

    public class cgeoGpsStatusListener implements GpsStatus.Listener {

        @Override
        public void onGpsStatusChanged(int event) {
            if (event == GpsStatus.GPS_EVENT_SATELLITE_STATUS) {
                GpsStatus status = geoManager.getGpsStatus(null);
                Iterator<GpsSatellite> statusIterator = status.getSatellites().iterator();

                int satellites = 0;
                int fixed = 0;

                while (statusIterator.hasNext()) {
                    GpsSatellite sat = statusIterator.next();
                    if (sat.usedInFix()) {
                        fixed++;
                    }
                    satellites++;

                    /*
                     * satellite signal strength
                     * if (sat.usedInFix()) {
                     * Log.d(Settings.tag, "Sat #" + satellites + ": " + sat.getSnr() + " FIX");
                     * } else {
                     * Log.d(Settings.tag, "Sat #" + satellites + ": " + sat.getSnr());
                     * }
                     */
                }

                boolean changed = false;
                if (satellitesVisible == null || satellites != satellitesVisible) {
                    satellitesVisible = satellites;
                    changed = true;
                }
                if (satellitesFixed == null || fixed != satellitesFixed) {
                    satellitesFixed = fixed;
                    changed = true;
                }

                if (changed) {
                    selectBest(null);
                }
            }
        }
    }

    private void selectBest(String initProvider) {
        if (locNet == null && locGps != null) { // we have only GPS
            assign(locGps);
            return;
        }

        if (locNet != null && locGps == null) { // we have only NET
            assign(locNet);
            return;
        }

        if (satellitesFixed > 0) { // GPS seems to be fixed
            assign(locGps);
            return;
        }

        if (initProvider != null && initProvider.equals(LocationManager.GPS_PROVIDER)) { // we have new location from GPS
            assign(locGps);
            return;
        }

        if (locGpsLast > (System.currentTimeMillis() - 30 * 1000)) { // GPS was working in last 30 seconds
            assign(locGps);
            return;
        }

        assign(locNet); // nothing else, using NET
    }

    private void assign(final Geopoint coords) {
        if (coords == null) {
            return;
        }

        gps = -1;
        coordsNow = coords;
        altitudeNow = null;
        bearingNow = 0f;
        speedNow = 0f;
        accuracyNow = 999f;

        if (geoUpdate != null) {
            geoUpdate.updateLoc(this);
        }
    }

    private void assign(Location loc) {
        if (loc == null) {
            gps = -1;
            return;
        }

        location = loc;

        String provider = location.getProvider();
        if (provider.equals(LocationManager.GPS_PROVIDER)) {
            gps = 1;
        } else if (provider.equals(LocationManager.NETWORK_PROVIDER)) {
            gps = 0;
        } else if (provider.equals("last")) {
            gps = -1;
        }

        coordsNow = new Geopoint(location.getLatitude(), location.getLongitude());
        app.setLastLoc(coordsNow);

        if (location.hasAltitude() && gps != -1) {
            altitudeNow = location.getAltitude() + Settings.getAltCorrection();
        } else {
            altitudeNow = null;
        }
        if (location.hasBearing() && gps != -1) {
            bearingNow = location.getBearing();
        } else {
            bearingNow = 0f;
        }
        if (location.hasSpeed() && gps != -1) {
            speedNow = location.getSpeed();
        } else {
            speedNow = 0f;
        }
        if (location.hasAccuracy() && gps != -1) {
            accuracyNow = location.getAccuracy();
        } else {
            accuracyNow = 999f;
        }

        if (gps == 1) {
            // save travelled distance only when location is from GPS
            if (coordsBefore != null && coordsNow != null) {
                final float dst = coordsBefore.distanceTo(coordsNow);

                if (dst > 0.005) {
                    distanceNow += dst;

                    coordsBefore = coordsNow;
                }
            } else if (coordsBefore == null) { // values aren't initialized
                coordsBefore = coordsNow;
            }
        }

        if (geoUpdate != null) {
            geoUpdate.updateLoc(this);
        }

        if (gps > -1) {
            (new publishLoc()).start();
        }
    }

    private class publishLoc extends Thread {

        private publishLoc() {
            setPriority(Thread.MIN_PRIORITY);
        }

        @Override
        public void run() {
            if (g4cRunning) {
                return;
            }

            if (Settings.isPublicLoc() && (lastGo4cacheCoords == null || coordsNow.distanceTo(lastGo4cacheCoords) > 0.75)) {
                g4cRunning = true;

                String action = null;
                if (app != null) {
                    action = app.getAction();
                } else {
                    action = "";
                }

                final String username = Settings.getUsername();
                if (username != null) {
                    final String latStr = String.format((Locale) null, "%.6f", coordsNow.getLatitude());
                    final String lonStr = String.format((Locale) null, "%.6f", coordsNow.getLongitude());
                    final Parameters params = new Parameters(
                            "u", username,
                            "lt", latStr,
                            "ln", lonStr,
                            "a", action,
                            "s", (CryptUtils.sha1(username + "|" + latStr + "|" + lonStr + "|" + action + "|" + CryptUtils.md5("carnero: developing your dreams"))).toLowerCase());
                    if (base.version != null) {
                        params.put("v", base.version);
                    }
                    final String res = cgBase.getResponseData(cgBase.postRequest("http://api.go4cache.com/", params));

                    if (StringUtils.isNotBlank(res)) {
                        lastGo4cacheCoords = coordsNow;
                    }
                }
            }

            g4cRunning = false;
        }
    }

    public void lastLoc() {
        assign(app.getLastCoords());

        Location lastGps = geoManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);

        if (lastGps != null) {
            lastGps.setProvider("last");
            assign(lastGps);

            Log.i(Settings.tag, "Using last location from GPS");
            return;
        }

        Location lastGsm = geoManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);

        if (lastGsm != null) {
            lastGsm.setProvider("last");
            assign(lastGsm);

            Log.i(Settings.tag, "Using last location from NETWORK");
            return;
        }
    }
}
