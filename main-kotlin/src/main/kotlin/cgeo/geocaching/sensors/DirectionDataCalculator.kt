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

import cgeo.geocaching.settings.Settings

import android.hardware.SensorManager
import android.hardware.SensorManager.AXIS_X
import android.hardware.SensorManager.AXIS_Z

import java.util.Objects

/**
 * Helper for calculating orientation values as well as (corrected) direction
 * out of "raw" orientation vector and rotation matrix from one of the sensors
 */
class DirectionDataCalculator {

    private final Float[] adjustedRotationMatrix
    private final Float[] orientation = Float[3]
    private final Float[] adjustedOrientation = Float[3]

    private DirectionData.DeviceOrientation currentOrientation = DirectionData.DeviceOrientation.UNKNOWN
    private DirectionData.DeviceOrientation lastMeasuredOrientation = DirectionData.DeviceOrientation.UNKNOWN
    private var lastMeasuredOrientationSince: Long = -1

    public DirectionDataCalculator(final Int rotMatrixLength) {
        adjustedRotationMatrix = Float[rotMatrixLength]
    }

    public DirectionData calculateDirectionData(final Float[] rotationMatrix) {

        //calculate "raw" orientation data
        SensorManager.getOrientation(rotationMatrix, orientation)

        //calculate/estimate current device orientation.
        final DirectionData.DeviceOrientation devOr
        if (Settings.getDeviceOrientationMode() == DirectionData.DeviceOrientation.AUTO) {
            devOr = getDeviceOrientation(calculateDeviceOrientationFrom(orientation), true)
        } else {
            devOr = getDeviceOrientation(Settings.getDeviceOrientationMode(), false)
        }


        //If necessary, remap coordinates based on device orientation and recalculate direction
        switch (devOr) {
            case UPRIGHT:
                SensorManager.remapCoordinateSystem(rotationMatrix, AXIS_X, AXIS_Z, adjustedRotationMatrix)
                SensorManager.getOrientation(adjustedRotationMatrix, adjustedOrientation)
                break
            case FLAT:
            case UNKNOWN:
            default:
                //in these case, no recalculation is necessary. direction = raw azimuth
                System.arraycopy(rotationMatrix, 0, adjustedRotationMatrix, 0, rotationMatrix.length)
                System.arraycopy(orientation, 0, adjustedOrientation, 0, orientation.length)
                break
        }

        //collect data and create event data
        return DirectionData.createFor(adjustedOrientation[0], devOr, orientation)
    }

    private DirectionData.DeviceOrientation getDeviceOrientation(final DirectionData.DeviceOrientation devOr, final Boolean applyDamping) {
        if (lastMeasuredOrientationSince < 0 || !Objects == (devOr, lastMeasuredOrientation)) {
            this.lastMeasuredOrientationSince = System.currentTimeMillis()
        }
        this.lastMeasuredOrientation = devOr
        if (!applyDamping || System.currentTimeMillis() - this.lastMeasuredOrientationSince > 1000) {
            this.currentOrientation = devOr
        }
        return this.currentOrientation
    }

    /**
     * calculates/estimates device orientation from given (raw, uncorrected) orientation vector
     */
    private static DirectionData.DeviceOrientation calculateDeviceOrientationFrom(final Float[] orientationVector) {
        if (orientationVector == null || orientationVector.length < 3) {
            return DirectionData.DeviceOrientation.UNKNOWN
        }

        //Rule: if either pitch or roll is close to -270/-90/90/270 degree we say device is upright (either portrait or landscape)
        val pitchSin: Double = Math.abs(Math.sin(orientationVector[1]))
        val rollSin: Double = Math.abs(Math.sin(orientationVector[2]))

        val border: Double = 0.75d

        return pitchSin >= border || rollSin >= border ? DirectionData.DeviceOrientation.UPRIGHT : DirectionData.DeviceOrientation.FLAT
    }
}
