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

package menion.android.whereyougo.utils;

import java.text.SimpleDateFormat;
import java.util.Date;

import menion.android.whereyougo.preferences.PreferenceValues;
import menion.android.whereyougo.preferences.Preferences;

public class UtilsFormat {

    private static final String TAG = "UtilsFormat";
    // degreeSign sign
    private static final String degreeSign = "\u00b0";
    private static final String minuteSign = "'";
    private static final String secondSign = "''";

    private static Date mDate;
    private static final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss");
    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
    private static final SimpleDateFormat datetimeFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    public static String formatAltitude(double altitude, boolean addUnits) {
        return locus.api.android.utils.UtilsFormat.INSTANCE.formatAltitude(Preferences.FORMAT_ALTITUDE, altitude, addUnits);
    }

    public static String formatAngle(double angle) {
        return locus.api.android.utils.UtilsFormat.INSTANCE.formatAngle(Preferences.FORMAT_ANGLE, (float) ((angle % 360) + 360) % 360, false, 0);
    }

    public static String formatSpeed(double speed, boolean withoutUnits) {
        return locus.api.android.utils.UtilsFormat.INSTANCE.formatSpeed(Preferences.FORMAT_SPEED, speed, withoutUnits);
    }

    public static String formatDistance(double dist, boolean withoutUnits) {
        return locus.api.android.utils.UtilsFormat.INSTANCE.formatDistance(Preferences.FORMAT_LENGTH, dist, withoutUnits);
    }

    public static String formatDouble(double value, int precision) {
        return locus.api.android.utils.UtilsFormat.INSTANCE.formatDouble(value, precision);
    }

    public static String formatDouble(double value, int precision, int minlen) {
        return locus.api.android.utils.UtilsFormat.INSTANCE.formatDouble(value, precision, minlen);
    }

    public static String addZeros(String text, int count) {
        if (text == null || text.length() > count)
            return text;
        String res = text;
        for (int i = res.length(); i < count; i++) {
            res = "0" + res;
        }
        return res;
    }

    public static String formatLatitude(double latitude) {
        return (latitude < 0 ? "S" : "N") + " " + formatCooLatLon(Math.abs(latitude), 2);
    }

    public static String formatLongitude(double longitude) {
        return (longitude < 0 ? "W" : "E") + " " + formatCooLatLon(Math.abs(longitude), 3);
    }

    public static String formatCooByType(double lat, double lon, boolean twoLines) {
        return formatLatitude(lat) + (twoLines ? "<br />" : " ") + formatLongitude(lon);
    }

    private static String formatCooLatLon(double value, int minLen) {
        try {
            if (Preferences.FORMAT_COO_LATLON == PreferenceValues.VALUE_UNITS_COO_LATLON_DEC) {
                return formatDouble(value, Const.PRECISION, minLen) + degreeSign;
            } else if (Preferences.FORMAT_COO_LATLON == PreferenceValues.VALUE_UNITS_COO_LATLON_MIN) {
                double deg = Math.floor(value);
                double min = (value - deg) * 60;
                return formatDouble(deg, 0, 2) + degreeSign
                        + formatDouble(min, Const.PRECISION - 2, 2) + minuteSign;
            } else if (Preferences.FORMAT_COO_LATLON == PreferenceValues.VALUE_UNITS_COO_LATLON_SEC) {
                double deg = Math.floor(value);
                double min = Math.floor((value - deg) * 60.0);
                double sec = (value - deg - min / 60.0) * 3600.0;
                return formatDouble(deg, 0, 2) + degreeSign
                        + formatDouble(min, 0, 2) + minuteSign
                        + formatDouble(sec, Const.PRECISION - 2) + secondSign;
            }
        } catch (Exception e) {
            Logger.e(TAG, "formatCoordinates(" + value + ", " + minLen + "), e:"
                    + e.toString());
        }
        return "";
    }

//    public static String formatGeoPoint(GeoPoint geoPoint) {
//        return formatCooByType(geoPoint.latitude, geoPoint.longitude, false);
//    }
//
//    public static String formatGeoPointDefault(GeoPoint geoPoint) {
//        String strLatitude = Location.convert(geoPoint.latitude, Location.FORMAT_MINUTES).replace(":", degreeSign);
//        String strLongitude = Location.convert(geoPoint.longitude, Location.FORMAT_MINUTES).replace(":", degreeSign);
//        return String.format("N %s E %s", strLatitude, strLongitude);
//    }
//
    public static String formatTime(long time) {
        if (mDate == null)
            mDate = new Date();
        mDate.setTime(time);
        return timeFormat.format(mDate);
    }

    public static String formatDate(long time) {
        if (mDate == null)
            mDate = new Date();
        mDate.setTime(time);
        return dateFormat.format(mDate);
    }

    public static String formatDatetime(long time) {
        if (mDate == null)
            mDate = new Date();
        mDate.setTime(time);
        return datetimeFormat.format(mDate);
    }

    public static String formatTime(boolean full, long tripTime) {
        return formatTime(full, tripTime, true);
    }

    /**
     * updated function for time formatting as in stop watch
     */
    public static String formatTime(boolean full, long tripTime, boolean withUnits) {
        long hours = tripTime / 3600000;
        long mins = (tripTime - (hours * 3600000)) / 60000;
        double sec = (tripTime - (hours * 3600000) - mins * 60000) / 1000.0;
        if (full) {
            if (withUnits) {
                return hours + "h:" + formatDouble(mins, 0, 2) + "m:" + formatDouble(sec, 0, 2) + "s";
            } else {
                return formatDouble(hours, 0, 2) + ":" + formatDouble(mins, 0, 2) + ":"
                        + formatDouble(sec, 0, 2);
            }
        } else {
            if (hours == 0) {
                if (mins == 0) {
                    if (withUnits)
                        return formatDouble(sec, 0) + "s";
                    else
                        return formatDouble(sec, 0, 2);
                } else {
                    if (withUnits)
                        return mins + "m:" + formatDouble(sec, 0) + "s";
                    else
                        return formatDouble(mins, 0, 2) + ":" + formatDouble(sec, 0, 2);
                }
            } else {
                if (withUnits) {
                    return hours + "h:" + mins + "m";
                } else {
                    return formatDouble(hours, 0, 2) + ":" + formatDouble(mins, 0, 2) + ":"
                            + formatDouble(sec, 0, 2);
                }
            }
        }
    }
}
