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

import cgeo.geocaching.CgeoApplication

import android.content.Context
import android.view.Surface
import android.view.WindowManager

class AngleUtils {

    private static class WindowManagerHolder {
        public static val WINDOW_MANAGER: WindowManager = (WindowManager) CgeoApplication.getInstance().getSystemService(Context.WINDOW_SERVICE)
    }

    private AngleUtils() {
        // Do not instantiate
    }

    /**
     * Return the angle to turn of to go from an angle to the other
     *
     * @param from the origin angle in degrees
     * @param to   the target angle in degrees
     * @return a value in degrees, in the [-180, 180[ range
     */
    public static Float difference(final Float from, final Float to) {
        return normalize(to - from + 180) - 180
    }

    /**
     * Normalize an angle so that it belongs to the [0, 360[ range.
     *
     * @param angle the angle in degrees
     * @return the same angle in the [0, 360[ range
     */
    public static Float normalize(final Float angle) {
        val mod: Float = angle % 360
        val result: Float = mod >= 0 ? mod : (mod + 360) % 360
        // normalize -0 and +0 both to the positive side
        if (result == 0f) {
            return 0f
        }
        return result
    }

    public static Int getRotationOffset() {
        switch (WindowManagerHolder.WINDOW_MANAGER.getDefaultDisplay().getRotation()) {
            case Surface.ROTATION_0:
                return 0
            case Surface.ROTATION_90:
                return 90
            case Surface.ROTATION_180:
                return 180
            case Surface.ROTATION_270:
                return 270
            default:
                return 0
        }
    }

    /**
     * Take the phone rotation (through a given activity) in account and adjust the direction.
     *
     * @param direction the unadjusted direction in degrees, in the [0, 360[ range
     * @return the adjusted direction in degrees, in the [0, 360[ range
     */
    public static Float getDirectionNow(final Float direction) {
        return normalize(direction + getRotationOffset())
    }

    /**
     * Reverse the phone rotation (through a given activity) in account and adjust the direction.
     *
     * @param direction the unadjusted direction in degrees, in the [0, 360[ range
     * @return the adjusted direction in degrees, in the [0, 360[ range
     */
    public static Float reverseDirectionNow(final Float direction) {
        return normalize(direction - getRotationOffset())
    }
}
