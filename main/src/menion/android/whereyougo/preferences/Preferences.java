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

import android.content.Context;
import android.preference.PreferenceManager;

import cgeo.geocaching.R;
import menion.android.whereyougo.utils.Logger;
import menion.android.whereyougo.utils.Utils;

public class Preferences {

    private static final String TAG = "SettingValues";

    // global things
    /**
     * altitude format
     */
    public static int FORMAT_ALTITUDE;
    /**
     * angle format
     */
    public static int FORMAT_ANGLE;
    /**
     * latitude/longitude format
     */
    public static int FORMAT_COO_LATLON;
    /**
     * distance format
     */
    public static int FORMAT_LENGTH;
    /**
     * speed format
     */
    public static int FORMAT_SPEED;

    /**
     * root directory
     */
    public static String GLOBAL_ROOT;
    /**
     * map provider option
     */
    public static int GLOBAL_MAP_PROVIDER;
    /**
     * save game automatically option
     */
    public static boolean GLOBAL_SAVEGAME_AUTO;
    /**
     * save game slots
     */
    public static int GLOBAL_SAVEGAME_SLOTS;
    /**
     * is fullscreen enabled
     */
    public static boolean GLOBAL_DOUBLE_CLICK;
    /**
     * GC credentials
     */
    public static String GC_USERNAME;
    public static String GC_PASSWORD;
    /**
     * is status icon enabled
     */
    public static boolean APPEARANCE_STATUSBAR;
    /**
     * is fullscreen enabled
     */
    public static boolean APPEARANCE_FULLSCREEN;
    /**
     * highlight option
     */
    public static int APPEARANCE_HIGHLIGHT;
    /**
     * stretch images option
     */
    public static boolean APPEARANCE_IMAGE_STRETCH;
    /**
     * large font
     */
    public static int APPEARANCE_FONT_SIZE;

    // GPS
    public static boolean GPS;
    /**
     * automatically start GPS
     */
    public static boolean GPS_START_AUTOMATICALLY;
    /**
     * gps min time
     */
    public static int GPS_MIN_TIME;
    /**
     * beep on gps fix
     */
    public static boolean GPS_BEEP_ON_GPS_FIX;
    /**
     * altitude correction
     */
    public static double GPS_ALTITUDE_CORRECTION;
    /**
     * disable GPS when not needed
     */
    public static boolean GPS_DISABLE_WHEN_HIDE;

    // SENSORS
    /**
     * use hardware compass
     */
    public static boolean SENSOR_HARDWARE_COMPASS;
    /**
     * use hardware compass
     */
    public static boolean SENSOR_HARDWARE_COMPASS_AUTO_CHANGE;
    /**
     * use hardware compass
     */
    public static int SENSOR_HARDWARE_COMPASS_AUTO_CHANGE_VALUE;
    /**
     * use true bearing as orientation
     */
    public static boolean SENSOR_BEARING_TRUE;
    /**
     * applied filter
     */
    public static int SENSOR_ORIENT_FILTER;

    // GUIDING
    /**
     * disable gps when screen off
     */
    public static boolean GUIDING_GPS_REQUIRED;
    /**
     * enable/disable guiding sounds
     */
    public static boolean GUIDING_SOUNDS;
    /**
     * waypoint sound type
     */
    public static int GUIDING_WAYPOINT_SOUND;
    /**
     * waypoint sound distance
     */
    public static int GUIDING_WAYPOINT_SOUND_DISTANCE;
    /**
     * waypoint sound file
     */
    public static String GUIDING_WAYPOINT_SOUND_CUSTOM_SOUND_URI;
    /**
     * zone navigation point
     */
    public static int GUIDING_ZONE_NAVIGATION_POINT;
    /**
     * run if screen is turned off
     */
    public static boolean GLOBAL_RUN_SCREEN_OFF;
  /* ------------ */

    private static Context prefContext;

    public static void setContext(Context c) {
        prefContext = c;
    }

  /* ------------ */

    public static boolean comparePreferenceKey(final String prefString, final int prefId) {
        return prefString.equals(prefContext.getString(prefId));
    }

    public static String getStringPreference(final int PreferenceId) {
        String key = prefContext.getString(PreferenceId);
        return PreferenceManager.getDefaultSharedPreferences(prefContext).getString(key, "");
    }

    public static double getDecimalPreference(final int PreferenceId) {
        String key = prefContext.getString(PreferenceId);
        return Utils.parseDouble(PreferenceManager.getDefaultSharedPreferences(prefContext).getString(key, "0.0"));
    }

    public static int getNumericalPreference(final int PreferenceId) {
        String key = prefContext.getString(PreferenceId);
        return Utils.parseInt(PreferenceManager.getDefaultSharedPreferences(prefContext).getString(key, "0"));
    }

    public static boolean getBooleanPreference(final int PreferenceId) {
        String key = prefContext.getString(PreferenceId);
        return /*Utils.parseBoolean(*/ PreferenceManager.getDefaultSharedPreferences(prefContext).getBoolean(key, false) /* ) */;
    }

  /* ------------ */

    public static void setStringPreference(final int PreferenceId, final Object value) {
        String key = prefContext.getString(PreferenceId);
        PreferenceManager.getDefaultSharedPreferences(prefContext).edit().putString(key, String.valueOf(value)).commit();
    }

    public static void setPreference(final int PreferenceId, final String value) {
        String key = prefContext.getString(PreferenceId);
        PreferenceManager.getDefaultSharedPreferences(prefContext).edit().putString(key, value).commit();
    }

    public static void setPreference(final int PreferenceId, final int value) {
        String key = prefContext.getString(PreferenceId);
        PreferenceManager.getDefaultSharedPreferences(prefContext).edit().putInt(key, value).commit();
    }

    public static void setPreference(final int PreferenceId, final float value) {
        String key = prefContext.getString(PreferenceId);
        PreferenceManager.getDefaultSharedPreferences(prefContext).edit().putFloat(key, value).commit();
    }

    public static void setPreference(final int PreferenceId, final boolean value) {
        String key = prefContext.getString(PreferenceId);
        PreferenceManager.getDefaultSharedPreferences(prefContext).edit().putBoolean(key, value).commit();
    }

    /* Note: Default values are defined in xml/<preferences>.xml and loaded at program start */
    public static void init(Context c) {
        Logger.d(TAG, "init(" + c + ")");

        try {
            GLOBAL_ROOT = getStringPreference(R.string.pref_KEY_S_ROOT);
            GLOBAL_MAP_PROVIDER = getNumericalPreference(R.string.pref_KEY_S_MAP_PROVIDER);
            GLOBAL_SAVEGAME_AUTO = getBooleanPreference(R.string.pref_KEY_B_SAVEGAME_AUTO);
            GLOBAL_SAVEGAME_SLOTS = getNumericalPreference(R.string.pref_KEY_S_SAVEGAME_SLOTS);
            GLOBAL_DOUBLE_CLICK = getBooleanPreference(R.string.pref_KEY_B_DOUBLE_CLICK);
            GLOBAL_RUN_SCREEN_OFF = getBooleanPreference(R.string.pref_KEY_B_RUN_SCREEN_OFF);
            GC_USERNAME = getStringPreference(R.string.pref_KEY_S_GC_USERNAME);
            GC_PASSWORD = getStringPreference(R.string.pref_KEY_S_GC_PASSWORD);

            APPEARANCE_STATUSBAR = getBooleanPreference(R.string.pref_KEY_B_STATUSBAR);
            APPEARANCE_FULLSCREEN = getBooleanPreference(R.string.pref_KEY_B_FULLSCREEN);
            APPEARANCE_HIGHLIGHT = getNumericalPreference(R.string.pref_KEY_S_HIGHLIGHT);
            APPEARANCE_IMAGE_STRETCH = getBooleanPreference(R.string.pref_KEY_B_IMAGE_STRETCH);
            APPEARANCE_FONT_SIZE = getNumericalPreference(R.string.pref_KEY_S_FONT_SIZE);

            FORMAT_ALTITUDE = getNumericalPreference(R.string.pref_KEY_S_UNITS_ALTITUDE);
            FORMAT_ANGLE = getNumericalPreference(R.string.pref_KEY_S_UNITS_ANGLE);
            FORMAT_COO_LATLON = getNumericalPreference(R.string.pref_KEY_S_UNITS_COO_LATLON);
            FORMAT_LENGTH = getNumericalPreference(R.string.pref_KEY_S_UNITS_LENGTH);
            FORMAT_SPEED = getNumericalPreference(R.string.pref_KEY_S_UNITS_SPEED);

            GPS = getBooleanPreference(R.string.pref_KEY_B_GPS);
            GPS_START_AUTOMATICALLY = getBooleanPreference(R.string.pref_KEY_B_GPS_START_AUTOMATICALLY);
            GPS_MIN_TIME = getNumericalPreference(R.string.pref_KEY_S_GPS_MIN_TIME_NOTIFICATION);
            GPS_BEEP_ON_GPS_FIX = getBooleanPreference(R.string.pref_KEY_B_GPS_BEEP_ON_GPS_FIX);
            GPS_ALTITUDE_CORRECTION = getDecimalPreference(R.string.pref_KEY_S_GPS_ALTITUDE_MANUAL_CORRECTION);
            GPS_DISABLE_WHEN_HIDE = getBooleanPreference(R.string.pref_KEY_B_GPS_DISABLE_WHEN_HIDE);

            SENSOR_HARDWARE_COMPASS = getBooleanPreference(R.string.pref_KEY_B_SENSOR_HARDWARE_COMPASS);
            SENSOR_HARDWARE_COMPASS_AUTO_CHANGE = getBooleanPreference(R.string.pref_KEY_B_HARDWARE_COMPASS_AUTO_CHANGE);
            SENSOR_HARDWARE_COMPASS_AUTO_CHANGE_VALUE = getNumericalPreference(R.string.pref_KEY_S_HARDWARE_COMPASS_AUTO_CHANGE_VALUE);
            SENSOR_BEARING_TRUE = getBooleanPreference(R.string.pref_KEY_B_SENSORS_BEARING_TRUE);
            SENSOR_ORIENT_FILTER = getNumericalPreference(R.string.pref_KEY_S_SENSORS_ORIENT_FILTER);

            GUIDING_GPS_REQUIRED = getBooleanPreference(R.string.pref_KEY_B_GUIDING_GPS_REQUIRED);
            GUIDING_SOUNDS = getBooleanPreference(R.string.pref_KEY_B_GUIDING_COMPASS_SOUNDS);
            GUIDING_WAYPOINT_SOUND = getNumericalPreference(R.string.pref_KEY_S_GUIDING_WAYPOINT_SOUND);
            GUIDING_WAYPOINT_SOUND_DISTANCE = getNumericalPreference(R.string.pref_KEY_S_GUIDING_WAYPOINT_SOUND_DISTANCE);
            GUIDING_WAYPOINT_SOUND_CUSTOM_SOUND_URI = getStringPreference(R.string.pref_KEY_S_GUIDING_WAYPOINT_SOUND_CUSTOM_SOUND_URI);
            GUIDING_ZONE_NAVIGATION_POINT = getNumericalPreference(R.string.pref_KEY_S_GUIDING_ZONE_POINT);
        } catch (Exception e) {
            Logger.e(TAG, "init() - ", e);
            // TODO factory reset
        }
    }
}
