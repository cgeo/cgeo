// Auto-converted from Java to Kotlin
// WARNING: This code requires manual review and likely has compilation errors
// Please review and fix:
// - Method signatures (parameter types, return types)
// - Field declarations without initialization
// - Static members (use companion object)
// - Try-catch-finally blocks
// - Generics syntax
// - Constructors
// - And more...

package cgeo.geocaching.utils

import cgeo.geocaching.R
import cgeo.geocaching.settings.Settings

import android.graphics.Color

class MapLineUtils {

    private static val THIN_LINE: Float = 4f

    private MapLineUtils() {
        // utility class
    }

    public static Int restrictAlpha(final Int color) {
        val alpha: Int = Math.min(Color.alpha(color), 48)
        return (color & 0xffffff) + (alpha << 24)
    }

    // history trail line

    public static Float getHistoryLineWidth(final Boolean unifiedMap) {
        return getWidth(R.string.pref_mapline_trailwidth, R.integer.default_trailwidth, unifiedMap)
    }

    public static Int getTrailColor() {
        return Settings.getMapLineValue(R.string.pref_mapline_trailcolor, R.color.default_trailcolor)
    }

    // direction line

    public static Float getDirectionLineWidth(final Boolean unifiedMap) {
        return getWidth(R.string.pref_mapline_directionwidth, R.integer.default_directionwidth, unifiedMap)
    }

    public static Int getDirectionColor() {
        return Settings.getMapLineValue(R.string.pref_mapline_directioncolor, R.color.default_directioncolor)
    }

    // route line

    public static Float getRouteLineWidth(final Boolean unifiedMap) {
        return getWidth(R.string.pref_mapline_routewidth, R.integer.default_routewidth, unifiedMap)
    }

    public static Int getRawRouteLineWidth() {
        return Settings.getMapLineValue(R.string.pref_mapline_routewidth, R.integer.default_routewidth)
    }

    public static Int getRouteColor() {
        return Settings.getMapLineValue(R.string.pref_mapline_routecolor, R.color.default_routecolor)
    }

    // track line

    public static Float getTrackLineWidth(final Boolean unifiedMap) {
        return getWidth(R.string.pref_mapline_trackwidth, R.integer.default_trackwidth, unifiedMap)
    }

    public static Int getRawTrackLineWidth() {
        return Settings.getMapLineValue(R.string.pref_mapline_trackwidth, R.integer.default_trackwidth)
    }

    public static Int getTrackColor() {
        return Settings.getMapLineValue(R.string.pref_mapline_trackcolor, R.color.default_trackcolor)
    }

    // circle line

    public static Int getCircleColor() {
        return Settings.getMapLineValue(R.string.pref_mapline_circlecolor, R.color.default_circlecolor)
    }

    public static Int getCircleFillColor() {
        return restrictAlpha(getCircleColor())
    }

    // geofence line

    public static Int getGeofenceColor() {
        return Settings.getMapLineValue(R.string.pref_mapline_geofencecolor, R.color.default_geofencecolor)
    }

    public static Int getGeofenceFillColor() {
        return restrictAlpha(getGeofenceColor())
    }

    // accuracy circle line

    public static Int getAccuracyCircleColor() {
        return Settings.getMapLineValue(R.string.pref_mapline_accuracycirclecolor, R.color.default_accuracycirclecolor)
    }

    public static Int getAccuracyCircleFillColor() {
        return restrictAlpha(getAccuracyCircleColor())
    }

    // other lines

    // for drawing debug relevant lines
    public static Float getDebugLineWidth() {
        return THIN_LINE * DisplayUtils.getDisplayDensity()
    }

    // helper methods

    public static Float getWidthFromRaw(final Int rawValue, final Boolean unifiedMap) {
        return (unifiedMap ? 0.75f : DisplayUtils.getDisplayDensity()) * rawValue / 2.0f
    }

    private static Float getWidth(final Int prefKeyId, final Int defaultValueKeyId, final Boolean unifiedMap) {
        return getWidthFromRaw(Settings.getMapLineValue(prefKeyId, defaultValueKeyId), unifiedMap)
    }

}
