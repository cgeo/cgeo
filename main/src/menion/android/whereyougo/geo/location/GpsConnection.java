/*
 * This file is part of WhereYouGo.
 *
 * WhereYouGo is free software: you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * WhereYouGo is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with WhereYouGo. If not,
 * see <http://www.gnu.org/licenses/>.
 *
 * Copyright (C) 2012 Menion <whereyougo@asamm.cz>
 */

package menion.android.whereyougo.geo.location;

import android.content.Context;
import android.location.GpsStatus;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import cgeo.geocaching.R;
import menion.android.whereyougo.audio.UtilsAudio;
import menion.android.whereyougo.gui.utils.UtilsGUI;
import menion.android.whereyougo.preferences.PreferenceValues;
import menion.android.whereyougo.preferences.Preferences;
import menion.android.whereyougo.utils.Logger;
import menion.android.whereyougo.utils.ManagerNotify;

public class GpsConnection {

    private static final String TAG = "GpsConnection";
    private LocationManager locationManager;
    private final MyLocationListener llGPS;
    private final MyLocationListener llNetwork;
    private final MyGpsListener gpsListener;
    private boolean isFixed;
    private Timer mGpsTimer;
    // temp variable for indicating whether network provider is enabled
    private boolean networkProviderEnabled;
    private boolean gpsProviderEnabled;
    private GpsStatus gpsStatus;

    public GpsConnection(Context context) {
        Logger.w(TAG, "onCreate()");

        // create listeners
        llGPS = new MyLocationListener();
        llNetwork = new MyLocationListener();
        gpsListener = new MyGpsListener();

        // init basic fixing values
        isFixed = false;

        // initialize connection
        locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        List<String> providers = locationManager.getAllProviders();

        // remove updates
        try {
            locationManager.removeUpdates(llGPS);
        } catch (Exception e) {
            Logger.w(TAG, "problem removing listeners llGPS, e:" + e);
        }
        try {
            locationManager.removeUpdates(llNetwork);
        } catch (Exception e) {
            Logger.w(TAG, "problem removing listeners llNetwork, e:" + e);
        }

        // add new listeners NETWORK
        if (providers.contains(LocationManager.NETWORK_PROVIDER)) {
            try {
                locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER,
                        Preferences.GPS_MIN_TIME * 1000, 0, llNetwork);
                networkProviderEnabled = true;
            } catch (Exception e) {
                Logger.w(TAG, "problem adding 'network' provider, e:" + e);
                networkProviderEnabled = false;
            }
        }

        // add new listener GPS
        if (providers.contains(LocationManager.GPS_PROVIDER)) {
            try {
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,
                        Preferences.GPS_MIN_TIME * 1000, 0, llGPS);
                gpsProviderEnabled = true;
            } catch (Exception e) {
                Logger.w(TAG, "problem adding 'GPS' provider, e:" + e);
                gpsProviderEnabled = false;
            }
        }

        // add new listener GPS
        try {
            locationManager.addGpsStatusListener(gpsListener);
        } catch (Exception e) {
            Logger.w(TAG, "problem adding 'GPS status' listener, e:" + e);
        }

        if (networkProviderEnabled || gpsProviderEnabled) {
            ManagerNotify.toastShortMessage(context, context.getString(R.string.gps_enabled));
        } else {
            if (PreferenceValues.getCurrentActivity() != null) {
                UtilsGUI.showDialogInfo(PreferenceValues.getCurrentActivity(),
                        R.string.no_location_providers_available);
            }
            LocationState.setGpsOff(context);
            destroy();
        }
    }

    public void destroy() {
        // Logger.w(TAG, "onDestroy()");
        if (locationManager != null) {
            disableNetwork();
            locationManager.removeUpdates(llGPS);
            locationManager.removeGpsStatusListener(gpsListener);
            locationManager = null;
            // XXX missing context to notify by widget
            ManagerNotify.toastShortMessage(R.string.gps_disabled);
        }
    }

    private void disableNetwork() {
        // Logger.w(TAG, "disableNetwork() - " + networkProviderEnabled);
        if (networkProviderEnabled) {
            locationManager.removeUpdates(llNetwork);
            networkProviderEnabled = false;
        }
    }

    private void enableNetwork() {
        // Logger.w(TAG, "enableNetwork() - " + networkProviderEnabled);
        if (!networkProviderEnabled) {
            try {
                locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER,
                        Preferences.GPS_MIN_TIME * 1000, 0, llNetwork);
                networkProviderEnabled = true;
            } catch (Exception e) {
            }
        }
    }

    private synchronized void handleOnLocationChanged(Location location) {
        // Logger.d(TAG, "handleOnLocationChanged(), fix:" + isFixed + ", loc:" + location);
        if (!isFixed) {
            if (location.getProvider().equals(LocationManager.GPS_PROVIDER)) {
                if (Preferences.GPS_BEEP_ON_GPS_FIX)
                    UtilsAudio.playBeep(1);
                disableNetwork();
                isFixed = true;
            }
            LocationState.onLocationChanged(location);
        } else {
            if (location.getProvider().equals(LocationManager.GPS_PROVIDER)) {
                LocationState.onLocationChanged(location);
                setNewTimer();
            } else {
                // do not send location
            }
        }
    }

    public boolean isProviderEnabled(String provider) {
        return locationManager != null && locationManager.isProviderEnabled(provider);
    }

    private void setNewTimer() {
        if (mGpsTimer != null) {
            mGpsTimer.cancel();
        }
        mGpsTimer = new Timer();
        mGpsTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                if (Preferences.GPS_BEEP_ON_GPS_FIX)
                    UtilsAudio.playBeep(2);
                mGpsTimer = null;
                isFixed = false;
            }
        }, 60000);
    }

    private class MyGpsListener implements GpsStatus.Listener {
        public void onGpsStatusChanged(int event) {
            // Logger.i(TAG, "onGpsStatusChanged(" + event + ")");
            try {
                if (locationManager == null)
                    return;

                if (event == GpsStatus.GPS_EVENT_FIRST_FIX) {
                    // Logger.w(TAG, "onGpsStatusChanged(" + event + "), first fix");
                } else if (event == GpsStatus.GPS_EVENT_SATELLITE_STATUS) {
                    // Logger.w(TAG, "onGpsStatusChanged(" + event + "), satellite status");
                    if (gpsStatus == null)
                        gpsStatus = locationManager.getGpsStatus(null);
                    else
                        gpsStatus = locationManager.getGpsStatus(gpsStatus);

                    LocationState.onGpsStatusChanged(event, gpsStatus);
                } else if (event == GpsStatus.GPS_EVENT_STARTED) {
                    // Logger.w(TAG, "onGpsStatusChanged(" + event + "), started");
                    LocationState.onGpsStatusChanged(event, null);
                } else if (event == GpsStatus.GPS_EVENT_STOPPED) {
                    // Logger.w(TAG, "onGpsStatusChanged(" + event + "), stopped");
                    LocationState.onGpsStatusChanged(event, null);
                    // XXX this happen for unknown reason!
                }
            } catch (Exception e) {
                Logger.e(TAG, "onGpsStatusChanged(" + event + ")", e);
            }
        }
    }

    private class MyLocationListener implements LocationListener {

        public MyLocationListener() {
        }

        public void onLocationChanged(android.location.Location location) {
            handleOnLocationChanged(new Location(location));
        }

        public void onProviderDisabled(String provider) {
            // Logger.i(TAG, "onProviderDisabled(" + provider + ")");
            LocationState.onProviderDisabled(provider);
            if (locationManager != null
                    && !locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
                    && !locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                LocationState.setGpsOff(PreferenceValues.getCurrentActivity());
                destroy();
            } else if (provider.equals(LocationManager.GPS_PROVIDER)) {
                enableNetwork();
            }
        }

        public void onProviderEnabled(String provider) {
            // Logger.i(TAG, "onProviderEnabled(" + provider + ")");
            LocationState.onProviderEnabled(provider);
        }

        public void onStatusChanged(String provider, int status, Bundle extras) {
            // Logger.i(TAG, "onStatusChanged(" + provider + ", status: " + status + ", geoDataExtra: " +
            // extras + ")");
            LocationState.onStatusChanged(provider, status, extras);
        }
    }
}
