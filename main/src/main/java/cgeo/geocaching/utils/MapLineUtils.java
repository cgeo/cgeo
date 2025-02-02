package cgeo.geocaching.utils;

import cgeo.geocaching.R;
import cgeo.geocaching.settings.Settings;

import android.graphics.Color;

public class MapLineUtils {

    private static final float THIN_LINE = 4f;

    private MapLineUtils() {
        // utility class
    }

    public static int restrictAlpha(final int color) {
        final int alpha = Math.min(Color.alpha(color), 48);
        return (color & 0xffffff) + (alpha << 24);
    }

    // history trail line

    public static float getHistoryLineWidth(final boolean unifiedMap) {
        return getWidth(R.string.pref_mapline_trailwidth, R.integer.default_trailwidth, unifiedMap);
    }

    public static int getTrailColor() {
        return Settings.getMapLineValue(R.string.pref_mapline_trailcolor, R.color.default_trailcolor);
    }

    // direction line

    public static float getDirectionLineWidth(final boolean unifiedMap) {
        return getWidth(R.string.pref_mapline_directionwidth, R.integer.default_directionwidth, unifiedMap);
    }

    public static int getDirectionColor() {
        return Settings.getMapLineValue(R.string.pref_mapline_directioncolor, R.color.default_directioncolor);
    }

    // route line

    public static float getRouteLineWidth(final boolean unifiedMap) {
        return getWidth(R.string.pref_mapline_routewidth, R.integer.default_routewidth, unifiedMap);
    }

    public static int getRawRouteLineWidth() {
        return Settings.getMapLineValue(R.string.pref_mapline_routewidth, R.integer.default_routewidth);
    }

    public static int getRouteColor() {
        return Settings.getMapLineValue(R.string.pref_mapline_routecolor, R.color.default_routecolor);
    }

    // track line

    public static float getTrackLineWidth(final boolean unifiedMap) {
        return getWidth(R.string.pref_mapline_trackwidth, R.integer.default_trackwidth, unifiedMap);
    }

    public static int getRawTrackLineWidth() {
        return Settings.getMapLineValue(R.string.pref_mapline_trackwidth, R.integer.default_trackwidth);
    }

    public static int getTrackColor() {
        return Settings.getMapLineValue(R.string.pref_mapline_trackcolor, R.color.default_trackcolor);
    }

    // circle line

    public static int getCircleColor() {
        return Settings.getMapLineValue(R.string.pref_mapline_circlecolor, R.color.default_circlecolor);
    }

    public static int getCircleFillColor() {
        return restrictAlpha(getCircleColor());
    }

    // geofence line

    public static int getGeofenceColor() {
        return Settings.getMapLineValue(R.string.pref_mapline_geofencecolor, R.color.default_geofencecolor);
    }

    public static int getGeofenceFillColor() {
        return restrictAlpha(getGeofenceColor());
    }

    // accuracy circle line

    public static int getAccuracyCircleColor() {
        return Settings.getMapLineValue(R.string.pref_mapline_accuracycirclecolor, R.color.default_accuracycirclecolor);
    }

    public static int getAccuracyCircleFillColor() {
        return restrictAlpha(getAccuracyCircleColor());
    }

    // other lines

    // for drawing debug relevant lines
    public static float getDebugLineWidth() {
        return THIN_LINE * DisplayUtils.getDisplayDensity();
    }

    // helper methods

    public static float getWidthFromRaw(final int rawValue, final boolean unifiedMap) {
        return (unifiedMap ? 0.75f : DisplayUtils.getDisplayDensity()) * rawValue / 2.0f;
    }

    private static float getWidth(final int prefKeyId, final int defaultValueKeyId, final boolean unifiedMap) {
        return getWidthFromRaw(Settings.getMapLineValue(prefKeyId, defaultValueKeyId), unifiedMap);
    }

}
