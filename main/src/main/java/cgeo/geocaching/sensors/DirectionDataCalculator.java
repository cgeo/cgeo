package cgeo.geocaching.sensors;

import cgeo.geocaching.settings.Settings;

import android.hardware.SensorManager;
import static android.hardware.SensorManager.AXIS_X;
import static android.hardware.SensorManager.AXIS_Z;

import java.util.Objects;

/**
 * Helper for calculating orientation values as well as (corrected) direction
 * out of "raw" orientation vector and rotation matrix from one of the sensors
 */
public class DirectionDataCalculator {

    private final float[] adjustedRotationMatrix;
    private final float[] orientation = new float[3];
    private final float[] adjustedOrientation = new float[3];

    private DirectionData.DeviceOrientation currentOrientation = DirectionData.DeviceOrientation.UNKNOWN;
    private DirectionData.DeviceOrientation lastMeasuredOrientation = DirectionData.DeviceOrientation.UNKNOWN;
    private long lastMeasuredOrientationSince = -1;

    public DirectionDataCalculator(final int rotMatrixLength) {
        adjustedRotationMatrix = new float[rotMatrixLength];
    }

    public DirectionData calculateDirectionData(final float[] rotationMatrix) {

        //calculate "raw" orientation data
        SensorManager.getOrientation(rotationMatrix, orientation);

        //calculate/estimate current device orientation.
        DirectionData.DeviceOrientation devOr = null;
        switch (Settings.getDeviceOrientationMode()) {
            case AUTO:
                devOr = getDeviceOrientation(calculateDeviceOrientationFrom(orientation), true);
                break;
            default:
                devOr = getDeviceOrientation(Settings.getDeviceOrientationMode(), false);
                break;
        }


        //If necessary, remap coordinates based on device orientation and recalculate direction
        switch (devOr) {
            case UPRIGHT:
                SensorManager.remapCoordinateSystem(rotationMatrix, AXIS_X, AXIS_Z, adjustedRotationMatrix);
                SensorManager.getOrientation(adjustedRotationMatrix, adjustedOrientation);
                break;
            case FLAT:
            case UNKNOWN:
            default:
                //in these case, no recalculation is necessary. direction = raw azimuth
                System.arraycopy(rotationMatrix, 0, adjustedRotationMatrix, 0, rotationMatrix.length);
                System.arraycopy(orientation, 0, adjustedOrientation, 0, orientation.length);
                break;
        }

        //collect data and create event data
        return DirectionData.createFor(adjustedOrientation[0], devOr, orientation);
    }

    private DirectionData.DeviceOrientation getDeviceOrientation(final DirectionData.DeviceOrientation devOr, final boolean applyDamping) {
        if (lastMeasuredOrientationSince < 0 || !Objects.equals(devOr, lastMeasuredOrientation)) {
            this.lastMeasuredOrientationSince = System.currentTimeMillis();
        }
        this.lastMeasuredOrientation = devOr;
        if (!applyDamping || System.currentTimeMillis() - this.lastMeasuredOrientationSince > 1000) {
            this.currentOrientation = devOr;
        }
        return this.currentOrientation;
    }

    /**
     * calculates/estimates device orientation from given (raw, uncorrected) orientation vector
     */
    private static DirectionData.DeviceOrientation calculateDeviceOrientationFrom(final float[] orientationVector) {
        if (orientationVector == null || orientationVector.length < 3) {
            return DirectionData.DeviceOrientation.UNKNOWN;
        }

        //Rule: if either pitch or roll is close to -270/-90/90/270 degree we say device is upright (either portrait or landscape)
        final double pitchSin = Math.abs(Math.sin(orientationVector[1]));
        final double rollSin = Math.abs(Math.sin(orientationVector[2]));

        final double border = 0.75d;

        return pitchSin >= border || rollSin >= border ? DirectionData.DeviceOrientation.UPRIGHT : DirectionData.DeviceOrientation.FLAT;
    }
}
