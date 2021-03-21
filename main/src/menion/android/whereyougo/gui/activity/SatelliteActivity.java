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

package menion.android.whereyougo.gui.activity;

import android.location.LocationManager;
import android.os.Bundle;
import android.view.ViewGroup.LayoutParams;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.ToggleButton;

import java.util.ArrayList;

import cgeo.geocaching.R;
import menion.android.whereyougo.geo.location.ILocationEventListener;
import menion.android.whereyougo.geo.location.Location;
import menion.android.whereyougo.geo.location.LocationState;
import menion.android.whereyougo.geo.location.Point2D;
import menion.android.whereyougo.geo.location.SatellitePosition;
import menion.android.whereyougo.geo.orientation.Orientation;
import menion.android.whereyougo.gui.extension.activity.CustomActivity;
import menion.android.whereyougo.gui.extension.dialog.CustomDialog;
import menion.android.whereyougo.gui.view.Satellite2DView;
import menion.android.whereyougo.preferences.PreferenceValues;
import menion.android.whereyougo.preferences.Preferences;
import menion.android.whereyougo.utils.A;
import menion.android.whereyougo.utils.Logger;
import menion.android.whereyougo.utils.ManagerNotify;
import menion.android.whereyougo.utils.Utils;
import menion.android.whereyougo.utils.UtilsFormat;

/**
 * @author menion
 * @since 25.1.2010 2010
 */
public class SatelliteActivity extends CustomActivity implements ILocationEventListener {

    private static final String TAG = "SatelliteScreen";
    private final ArrayList<SatellitePosition> satellites = new ArrayList<>();
    private Satellite2DView satelliteView;
    private ToggleButton buttonGps;

    private void createLayout() {
        LinearLayout llSkyplot = (LinearLayout) findViewById(R.id.linear_layout_skyplot);
        llSkyplot.removeAllViews();

        // return and add view to first linearLayout
        satelliteView = new Satellite2DView(SatelliteActivity.this, satellites);
        llSkyplot.addView(satelliteView, LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);

        // change colors for 3.0+
        if (Utils.isAndroid30OrMore()) {
            findViewById(R.id.linear_layout_bottom_3).setBackgroundColor(CustomDialog.BOTTOM_COLOR_A3);
        }

        // and final bottom buttons
        buttonGps = (ToggleButton) findViewById(R.id.btn_gps_on_off);
        buttonGps.setChecked(LocationState.isActuallyHardwareGpsOn());
        buttonGps.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (!isChecked) {
                LocationState.setGpsOff(SatelliteActivity.this);

                // disable satellites on screen
                satellites.clear();
                satelliteView.invalidate();
            } else {
                LocationState.setGpsOn(SatelliteActivity.this);
            }

            onGpsStatusChanged(0, null);
            PreferenceValues.enableWakeLock();
        });

        ToggleButton buttonCompass = (ToggleButton) findViewById(R.id.btn_compass_on_off);
        buttonCompass.setChecked(Preferences.SENSOR_HARDWARE_COMPASS);
        buttonCompass.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                ManagerNotify.toastLongMessage(R.string.pref_sensors_compass_hardware_desc);
                Preferences.SENSOR_HARDWARE_COMPASS = isChecked;
                Preferences.setPreference(R.string.pref_KEY_B_SENSOR_HARDWARE_COMPASS, Preferences.SENSOR_HARDWARE_COMPASS);
                A.getRotator().manageSensors();
            }
        });
    }

    @Override
    public String getName() {
        return TAG;
    }

    public int getPriority() {
        return ILocationEventListener.PRIORITY_MEDIUM;
    }

    @Override
    public boolean isRequired() {
        return false;
    }

    public void notifyGpsDisable() {
        buttonGps.setChecked(false);
    }

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.satellite_screen_activity);

        createLayout();
    }

    public void onGpsStatusChanged(int event, ArrayList<SatellitePosition> gpsStatus) {
        try {
            Point2D.Int num = setSatellites(gpsStatus);
            satelliteView.invalidate();
            ((TextView) findViewById(R.id.text_view_satellites)).setText(num.x + " | " + num.y);
        } catch (Exception e) {
            Logger.e(TAG, "onGpsStatusChanged(" + event + ", " + gpsStatus + "), e:" + e.toString());
        }
    }

    public void onLocationChanged(final Location location) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                String provider = location.getProvider();
                switch (provider) {
                    case LocationManager.GPS_PROVIDER:
                        provider = getString(R.string.provider_gps);
                        break;
                    case LocationManager.NETWORK_PROVIDER:
                        provider = getString(R.string.provider_network);
                        break;
                    default:
                        provider = "-";
                        break;
                }
                ((TextView) findViewById(R.id.text_view_provider)).setText(provider);
                ((TextView) findViewById(R.id.text_view_latitude)).setText(UtilsFormat
                        .formatLatitude(location.getLatitude()));
                ((TextView) findViewById(R.id.text_view_longitude)).setText(UtilsFormat
                        .formatLongitude(location.getLongitude()));
                ((TextView) findViewById(R.id.text_view_altitude)).setText(UtilsFormat.formatAltitude(
                        location.getAltitude(), true));
                ((TextView) findViewById(R.id.text_view_accuracy)).setText(UtilsFormat.formatDistance(
                        location.getAccuracy(), false));
                ((TextView) findViewById(R.id.text_view_speed)).setText(UtilsFormat.formatSpeed(
                        location.getSpeed(), false));
                ((TextView) findViewById(R.id.text_view_declination)).setText(UtilsFormat
                        .formatAngle(Orientation.getDeclination()));
                long lastFix = LocationState.getLastFixTime();
                if (lastFix > 0) {
                    ((TextView) findViewById(R.id.text_view_time_gps)).setText(UtilsFormat
                            .formatTime(lastFix));
                } else {
                    ((TextView) findViewById(R.id.text_view_time_gps)).setText("~");
                }
            }
        });


    }

    @Override
    protected void onResume() {
        super.onResume();
        onLocationChanged(LocationState.getLocation());
        onGpsStatusChanged(0, null);
    }

    public void onStart() {
        super.onStart();
        LocationState.addLocationChangeListener(this);
        if (buttonGps.isChecked() && !LocationState.isActuallyHardwareGpsOn())
            notifyGpsDisable();
    }

    public void onStatusChanged(String provider, int state, Bundle extra) {
    }

    public void onStop() {
        super.onStop();
        LocationState.removeLocationChangeListener(this);
    }

    private Point2D.Int setSatellites(ArrayList<SatellitePosition> sats) {
        synchronized (satellites) {
            Point2D.Int satCount = new Point2D.Int();
            satellites.clear();
            if (sats != null) {
                for (int i = 0; i < sats.size(); i++) {
                    SatellitePosition sat = sats.get(i);
                    if (sat.isFixed())
                        satCount.x++;
                    satCount.y++;
                    satellites.add(sat);
                }
            }
            return satCount;
        }
    }
}
