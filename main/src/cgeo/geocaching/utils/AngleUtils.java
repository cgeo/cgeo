package cgeo.geocaching.utils;

import cgeo.geocaching.CgeoApplication;

import android.content.Context;
import android.view.Surface;
import android.view.WindowManager;

public final class AngleUtils {

    private static class WindowManagerHolder {
        public static final WindowManager WINDOW_MANAGER = (WindowManager) CgeoApplication.getInstance().getSystemService(Context.WINDOW_SERVICE);
    }

    private AngleUtils() {
        // Do not instantiate
    }

    /**
     * Return the angle to turn of to go from an angle to the other
     *
     * @param from
     *            the origin angle in degrees
     * @param to
     *            the target angle in degrees
     * @return a value in degrees, in the [-180, 180[ range
     */
    public static float difference(final float from, final float to) {
        return normalize(to - from + 180) - 180;
    }

    /**
     * Normalize an angle so that it belongs to the [0, 360[ range.
     * @param angle the angle in degrees
     * @return the same angle in the [0, 360[ range
     */
    public static float normalize(final float angle) {
        return (angle >= 0 ? angle : (360 - ((-angle) % 360))) % 360;
    }

    public static int getRotationOffset() {
        switch (WindowManagerHolder.WINDOW_MANAGER.getDefaultDisplay().getRotation()) {
            case Surface.ROTATION_90:
                return 90;
            case Surface.ROTATION_180:
                return 180;
            case Surface.ROTATION_270:
                return 270;
            default:
                return 0;
        }
    }

    /**
     * Take the phone rotation (through a given activity) in account and adjust the direction.
     *
     * @param direction the unadjusted direction in degrees, in the [0, 360[ range
     * @return the adjusted direction in degrees, in the [0, 360[ range
     */
    public static float getDirectionNow(final float direction) {
        return normalize(direction + getRotationOffset());
    }

    /**
     * Reverse the phone rotation (through a given activity) in account and adjust the direction.
     *
     * @param direction the unadjusted direction in degrees, in the [0, 360[ range
     * @return the adjusted direction in degrees, in the [0, 360[ range
     */
    public static float reverseDirectionNow(final float direction) {
        return normalize(direction - getRotationOffset());
    }
}
