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

package menion.android.whereyougo.preferences;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.PowerManager;

import cgeo.geocaching.R;
import menion.android.whereyougo.geo.location.Location;
import menion.android.whereyougo.geo.location.LocationState;
import menion.android.whereyougo.utils.A;
import menion.android.whereyougo.utils.Logger;


public class PreferenceValues {

    // GLOBAL
    /**
     * map provider
     */
    public static final int VALUE_MAP_PROVIDER_VECTOR = 0;
    public static final int VALUE_MAP_PROVIDER_LOCUS = 1;
    // APPEARANCE
    /**
     * screen highlight mode
     */
    public static final int VALUE_HIGHLIGHT_OFF = 0;
    public static final int VALUE_HIGHLIGHT_ONLY_GPS = 1;
    public static final int VALUE_HIGHLIGHT_ALWAYS = 2;
    /**
     * font size
     */
    public static final int VALUE_FONT_SIZE_DEFAULT = 0;
    public static final int VALUE_FONT_SIZE_SMALL = 1;
    public static final int VALUE_FONT_SIZE_MEDIUM = 2;
    public static final int VALUE_FONT_SIZE_LARGE = 3;

    // SENSORS
    public static final int VALUE_SENSORS_ORIENT_FILTER_NO = 0;
    public static final int VALUE_SENSORS_ORIENT_FILTER_LIGHT = 1;
    public static final int VALUE_SENSORS_ORIENT_FILTER_MEDIUM = 2;
    public static final int VALUE_SENSORS_ORIENT_FILTER_HEAVY = 3;
    // GUIDING
    public static final int VALUE_GUIDING_WAYPOINT_SOUND_INCREASE_CLOSER = 0;
    public static final int VALUE_GUIDING_WAYPOINT_SOUND_BEEP_ON_DISTANCE = 1;
    public static final int VALUE_GUIDING_WAYPOINT_SOUND_CUSTOM_SOUND = 2;
    /**
     * navigation point
     */
    public static final int VALUE_GUIDING_ZONE_POINT_CENTER = 0;
    public static final int VALUE_GUIDING_ZONE_POINT_NEAREST = 1;
    /**
     * default latitude/longitude format
     */
    public static final int VALUE_UNITS_COO_LATLON_DEC = 0;

    // UNITS PARAMETERS
    public static final int VALUE_UNITS_COO_LATLON_MIN = 1;
    public static final int VALUE_UNITS_COO_LATLON_SEC = 2;
    /**
     * default length format
     */
    public static final int VALUE_UNITS_LENGTH_ME = 0;
    public static final int VALUE_UNITS_LENGTH_IM = 1;
    public static final int VALUE_UNITS_LENGTH_NA = 2;
    /**
     * default height format
     */
    public static final int VALUE_UNITS_ALTITUDE_METRES = 0;
    public static final int VALUE_UNITS_ALTITUDE_FEET = 1;
    /**
     * default angle format
     */
    public static final int VALUE_UNITS_SPEED_KMH = 0;
    public static final int VALUE_UNITS_SPEED_MILH = 1;
    public static final int VALUE_UNITS_SPEED_KNOTS = 2;
    /**
     * default angle format
     */
    public static final int VALUE_UNITS_ANGLE_DEGREE = 0;
    public static final int VALUE_UNITS_ANGLE_MIL = 1;
    private static final String TAG = "PreferenceValues";


    // set from onResume();
    private static Activity currentActivity;

    private static PowerManager.WakeLock wl;
    private static int wl_level = 0;

    public static void disableWakeLock() {
        Logger.w(TAG, "disableWakeLock(), wl:" + wl);
        if (wl != null) {
            wl.release();
            wl = null;
            wl_level = 0;
        }
    }

    public static void enableWakeLock() {
        try {
            boolean disable = false;
            int new_level = 0;
            if (!Preferences.GLOBAL_RUN_SCREEN_OFF) {
                if (!existCurrentActivity()) {
                    disable = true;
                } else {
                    if (Preferences.APPEARANCE_HIGHLIGHT == VALUE_HIGHLIGHT_OFF) {
                        disable = true;
                    } else if (Preferences.APPEARANCE_HIGHLIGHT == VALUE_HIGHLIGHT_ONLY_GPS) {
                        if (!LocationState.isActuallyHardwareGpsOn()) {
                            disable = true;
                        }
                    }
                }
            }

            if ((Preferences.APPEARANCE_HIGHLIGHT == VALUE_HIGHLIGHT_ONLY_GPS || Preferences.APPEARANCE_HIGHLIGHT == VALUE_HIGHLIGHT_ALWAYS) && existCurrentActivity()) {
                new_level = PowerManager.SCREEN_BRIGHT_WAKE_LOCK;
            } else {
                if (Preferences.GLOBAL_RUN_SCREEN_OFF) {
                    new_level = PowerManager.PARTIAL_WAKE_LOCK;
                }
            }
            Logger.w(TAG, "enableWakeLock(), dis:" + disable + " level:" + new_level + ", wl:" + wl + " current level:" + wl_level);
            if ((disable || new_level == 0) && wl != null) {
                disableWakeLock();
            } else if (!disable && new_level != wl_level) {
                if (wl != null) {
                    wl.release();
                }
                PowerManager pm = (PowerManager) A.getApp().getSystemService(Context.POWER_SERVICE);
                if (pm != null) {
                    wl = pm.newWakeLock(new_level, "whereyougo:" + TAG);
                    wl.acquire();
                    wl_level = new_level;
                }
            }
            // Logger.w(TAG, "enableWakeLock(), res:" + wl);
        } catch (Exception e) {
            Logger.e(TAG, "enableWakeLock(), e:" + e.toString());
        }
    }

    public static boolean existCurrentActivity() {
        return currentActivity != null;
    }


    public static int getApplicationVersionActual() {
        try {
            return A.getApp().getPackageManager().getPackageInfo(A.getApp().getPackageName(), 0).versionCode;
        } catch (NameNotFoundException e) {
            Logger.e(TAG, "getApplicationVersionActual()", e);
            return 0;
        }
    }

    public static int getApplicationVersionLast() {
        try {
            return Preferences.getNumericalPreference(R.string.pref_KEY_S_APPLICATION_VERSION_LAST);
        } catch (ClassCastException e) {
            // workaround for old settings, which stores this key as integer
            Logger.e(TAG, "getNumericalPreference( R.string.pref_KEY_S_APPLICATION_VERSION_LAST ) return 0", e);
            return 0;
        }
    }

    public static void setApplicationVersionLast(int lastVersion) {
        Preferences.setStringPreference(R.string.pref_KEY_S_APPLICATION_VERSION_LAST, lastVersion);
    }

    public static Activity getCurrentActivity() {
        return currentActivity == null ? A.getMain() : currentActivity;
    }

    public static void setCurrentActivity(Activity activity) {
        PreferenceValues.currentActivity = activity;
    }

    public static String getLanguageCode() {
        // language for info dialog html files
        // only english supported
        Logger.w(TAG, "getLanguageCode() - " + "en");
        return Locale.getString(R.string.pref_language_en_shortcut);
    }

    public static Location getLastKnownLocation() {
        Location lastKnownLocation = new Location(TAG);
        lastKnownLocation.setLatitude(Preferences.getDecimalPreference(R.string.pref_KEY_F_LAST_KNOWN_LOCATION_LATITUDE));
        lastKnownLocation.setLongitude(Preferences.getDecimalPreference(R.string.pref_KEY_F_LAST_KNOWN_LOCATION_LONGITUDE));
        lastKnownLocation.setAltitude(Preferences.getDecimalPreference(R.string.pref_KEY_F_LAST_KNOWN_LOCATION_ALTITUDE));
        return lastKnownLocation;
    }

    public static void setLastKnownLocation() {
        try {
            Preferences.setStringPreference(R.string.pref_KEY_F_LAST_KNOWN_LOCATION_LATITUDE,
                    LocationState.getLocation().getLatitude());
            Preferences.setStringPreference(R.string.pref_KEY_F_LAST_KNOWN_LOCATION_LONGITUDE,
                    LocationState.getLocation().getLongitude());
            Preferences.setStringPreference(R.string.pref_KEY_F_LAST_KNOWN_LOCATION_ALTITUDE,
                    LocationState.getLocation().getAltitude());
        } catch (Exception e) {
            Logger.e(TAG, "setLastKnownLocation()", e);
        }
    }
}
