package cgeo.geocaching.utils;

public class AngleUtils {

    private AngleUtils() {
        // Do not instantiate
    }

    /**
     * Return the angle to turn of to go from an angle to the other
     *
     * @param from the origin angle in degrees
     * @param to the target angle in degreees
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
}
