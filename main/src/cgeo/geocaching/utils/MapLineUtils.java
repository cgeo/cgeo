package cgeo.geocaching.utils;

import cgeo.geocaching.R;
import cgeo.geocaching.settings.Settings;

public class MapLineUtils {

    private static final float THIN_LINE = 4f;

    private MapLineUtils() {
        // utility class
    }

    // history trail line

    public static float getHistoryLineWidth() {
        return getWidth(R.string.pref_mapline_trailwidth, R.integer.default_trailwidth);
    }

    public static int getTrailColor() {
        return Settings.getMapLineValue(R.string.pref_mapline_trailcolor, R.color.default_trailcolor);
    }

    // direction line

    public static float getDirectionLineWidth() {
        return getWidth(R.string.pref_mapline_directionwidth, R.integer.default_directionwidth);
    }

    public static int getDirectionColor() {
        return Settings.getMapLineValue(R.string.pref_mapline_directioncolor, R.color.default_directioncolor);
    }

    // route line

    public static float getRouteLineWidth() {
        return getWidth(R.string.pref_mapline_routewidth, R.integer.default_routewidth);
    }

    public static int getRouteColor() {
        return Settings.getMapLineValue(R.string.pref_mapline_routecolor, R.color.default_routecolor);
    }

    // track line

    public static float getTrackLineWidth() {
        return getWidth(R.string.pref_mapline_trackwidth, R.integer.default_trackwidth);
    }

    public static int getTrackColor() {
        return Settings.getMapLineValue(R.string.pref_mapline_trackcolor, R.color.default_trackcolor);
    }

    // circle line

    public static int getCircleColor() {
        return Settings.getMapLineValue(R.string.pref_mapline_circlecolor, R.color.default_circlecolor);
    }

    public static int getCircleFillColor() {
        return Settings.getMapLineValue(R.string.pref_mapline_circlefillcolor, R.color.default_circlefillcolor);
    }

    // other lines

    public static float getDefaultThinLineWidth() {
        return THIN_LINE * DisplayUtils.getDisplayDensity();
    }

    // for drawing debug relevant lines
    public static float getDebugLineWidth() {
        return THIN_LINE * DisplayUtils.getDisplayDensity();
    }

    // helper methods

    private static float getWidth(final int prefKeyId, final int defaultValueKeyId) {
        return (Settings.getMapLineValue(prefKeyId, defaultValueKeyId) / 2.0f + 1.0f) * DisplayUtils.getDisplayDensity();
    }

}
