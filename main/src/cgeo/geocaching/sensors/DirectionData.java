package cgeo.geocaching.sensors;

import cgeo.geocaching.R;

import android.hardware.SensorManager;

/**
 * Stores data related to (device) orientation sensor events.
 * This data is used to calculate a device orientation and a (device) direction/heading. Result
 * is also stored in this object.
 */
public class DirectionData {

    public static final DirectionData EMPTY = createFor(0.0f);

    /**
     * enums for different device orientations
     */
    public enum DeviceOrientation {
        UNKNOWN(R.string.do_unknown),
        /**
         * special value for 'auto selection' mode.
         */
        AUTO(R.string.do_auto),
        /**
         * device is "lying" near-flat somewhere (portrait or landscape).
         */
        FLAT(R.string.do_flat),
        /**
         * device is standing "upright" (portrait or landscape).
         */
        UPRIGHT(R.string.do_upright),
        ;

        public final int resId;

        DeviceOrientation(final int resId) {
            this.resId = resId;
        }

    }

    //corrected direction/heading based on deviceorientation
    private final float direction;
    private final DeviceOrientation deviceOrientation;

    //raw orientation sensor values
    private final Float azimuth;
    private final Float pitch;
    private final Float roll;

    private DirectionData(final float direction, final DeviceOrientation dp, final Float azimuth, final Float pitch, final Float roll) {
        this.direction = direction;
        this.deviceOrientation = dp;
        this.pitch = pitch;
        this.roll = roll;
        this.azimuth = azimuth;
    }

    public static DirectionData createFor(final float direction) {
        return createFor(direction, DeviceOrientation.UNKNOWN, null, false);
    }

    /**
     * Create from orientation vector as returned by {@link SensorManager#getOrientation(float[], float[])}
     */
    public static DirectionData createFor(final float direction, final DeviceOrientation dp, final float[] orientationVector) {
        return createFor(direction, dp, orientationVector, true);

    }

    public static DirectionData createFor(final float direction, final DeviceOrientation dp, final float[] orientationVector, final boolean inputInRadian) {
        final boolean vecIsNull = orientationVector == null;
        return new DirectionData(
                inputInRadian ? radianToDegree(direction) : direction,
                dp,
                vecIsNull ? null : (inputInRadian ? radianToDegree(orientationVector[0]) : orientationVector[0]),
                vecIsNull ? null : (inputInRadian ? radianToDegree(orientationVector[1]) : orientationVector[1]),
                vecIsNull ? null : (inputInRadian ? radianToDegree(orientationVector[2]) : orientationVector[2])
        );
    }

    /**
     * Returns corrected direction in degree! (0 - 360)
     */
    public float getDirection() {
        return direction;
    }

    /**
     * Returns device orientation, calculated from raw orientation values (pitch and roll)
     */
    public DeviceOrientation getDeviceOrientation() {
        return deviceOrientation;
    }

    /**
     * Returns pitch in degree! (-180 - 360)
     */
    public float getPitch() {
        return this.pitch == null ? 0.0f : this.pitch;
    }

    public boolean hasOrientation() {
        return this.pitch != null;
    }

    /**
     * Returns roll in degree! (-180 - 360)
     */
    public float getRoll() {
        return this.roll == null ? 0.0f : this.roll;
    }

    /**
     * Returns azimuth in degree! (-180 - 360)
     */
    public float getAzimuth() {
        return this.azimuth == null ? 0.0f : this.azimuth;
    }

    public static float radianToDegree(final float rad) {
        return (float) (rad * 180 / Math.PI);
    }

}
