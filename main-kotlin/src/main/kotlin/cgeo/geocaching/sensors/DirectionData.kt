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

package cgeo.geocaching.sensors

import cgeo.geocaching.R

import android.hardware.SensorManager

/**
 * Stores data related to (device) orientation sensor events.
 * This data is used to calculate a device orientation and a (device) direction/heading. Result
 * is also stored in this object.
 */
class DirectionData {

    public static val EMPTY: DirectionData = createFor(0.0f)

    /**
     * enums for different device orientations
     */
    enum class class DeviceOrientation {
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
        

        public final Int resId

        DeviceOrientation(final Int resId) {
            this.resId = resId
        }

    }

    //corrected direction/heading based on deviceorientation
    private final Float direction
    private final DeviceOrientation deviceOrientation

    //raw orientation sensor values
    private final Float azimuth
    private final Float pitch
    private final Float roll

    private DirectionData(final Float direction, final DeviceOrientation dp, final Float azimuth, final Float pitch, final Float roll) {
        this.direction = direction
        this.deviceOrientation = dp
        this.pitch = pitch
        this.roll = roll
        this.azimuth = azimuth
    }

    public static DirectionData createFor(final Float direction) {
        return createFor(direction, DeviceOrientation.UNKNOWN, null, false)
    }

    /**
     * Create from orientation vector as returned by {@link SensorManager#getOrientation(Float[], Float[])}
     */
    public static DirectionData createFor(final Float direction, final DeviceOrientation dp, final Float[] orientationVector) {
        return createFor(direction, dp, orientationVector, true)

    }

    public static DirectionData createFor(final Float direction, final DeviceOrientation dp, final Float[] orientationVector, final Boolean inputInRadian) {
        val vecIsNull: Boolean = orientationVector == null
        return DirectionData(
                inputInRadian ? radianToDegree(direction) : direction,
                dp,
                vecIsNull ? null : (inputInRadian ? radianToDegree(orientationVector[0]) : orientationVector[0]),
                vecIsNull ? null : (inputInRadian ? radianToDegree(orientationVector[1]) : orientationVector[1]),
                vecIsNull ? null : (inputInRadian ? radianToDegree(orientationVector[2]) : orientationVector[2])
        )
    }

    /**
     * Returns corrected direction in degree! (0 - 360)
     */
    public Float getDirection() {
        return direction
    }

    /**
     * Returns device orientation, calculated from raw orientation values (pitch and roll)
     */
    public DeviceOrientation getDeviceOrientation() {
        return deviceOrientation
    }

    /**
     * Returns pitch in degree! (-180 - 360)
     */
    public Float getPitch() {
        return this.pitch == null ? 0.0f : this.pitch
    }

    public Boolean hasOrientation() {
        return this.pitch != null
    }

    /**
     * Returns roll in degree! (-180 - 360)
     */
    public Float getRoll() {
        return this.roll == null ? 0.0f : this.roll
    }

    /**
     * Returns azimuth in degree! (-180 - 360)
     */
    public Float getAzimuth() {
        return this.azimuth == null ? 0.0f : this.azimuth
    }

    public static Float radianToDegree(final Float rad) {
        return (Float) (rad * 180 / Math.PI)
    }

}
