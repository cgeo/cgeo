package cgeo.geocaching.sensors;

import cgeo.geocaching.utils.AndroidRxUtils;
import cgeo.geocaching.utils.Log;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

import io.reactivex.rxjava3.core.Observable;


public class RotationProvider {

    /**
     * On some Samsung devices, {@link SensorManager#getRotationMatrixFromVector} throws an exception if the rotation
     * vector has more than 4 elements.
     * <p/>
     * This will be detected and remembered after the first occurrence of the exception. Concurrent access
     * is not a problem as this variable can only go from {@code false} to {@code true} and being {@code false}
     * instead of {@code true} is innocuous and will be changed immediately when needed.
     *
     * @see <a href="http://stackoverflow.com/a/22138449">this Stack Overflow answer</a>
     */
    private static boolean isTruncationNeeded = false;

    private RotationProvider() {
        // Utility class, not to be instantiated
    }

    public static Observable<DirectionData> create(final Context context) {
        final SensorManager sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        final Sensor rotationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
        if (rotationSensor == null) {
            Log.w("RotationProvider: no rotation sensor on this device");
            return Observable.error(new RuntimeException("no rotation sensor"));
        }
        final Observable<DirectionData> observable = Observable.create(emitter -> {
            final SensorEventListener listener = new SensorEventListener() {

                private final float[] rotationMatrix = new float[16];
                private final DirectionDataCalculator dirDataCalculator = new DirectionDataCalculator(rotationMatrix.length);
                private final float[] values = new float[4];

                @Override
                public void onSensorChanged(final SensorEvent event) {
                    if (isTruncationNeeded) {
                        // Since only the four first elements are used (and accepted), we truncate the vector.
                        System.arraycopy(event.values, 0, values, 0, 4);
                        SensorManager.getRotationMatrixFromVector(rotationMatrix, values);
                    } else {
                        try {
                            SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values);
                        } catch (final IllegalArgumentException ignored) {
                            Log.d("installing workaround for mismatched number of values in rotation vector");
                            // Install workaround and retry
                            isTruncationNeeded = true;
                            onSensorChanged(event);
                            return;
                        }
                    }
                    emitter.onNext(dirDataCalculator.calculateDirectionData(rotationMatrix));
                }

                @Override
                public void onAccuracyChanged(final Sensor sensor, final int accuracy) {
                    // empty
                }

            };
            Log.d("RotationProvider: registering listener");
            sensorManager.registerListener(listener, rotationSensor, SensorManager.SENSOR_DELAY_NORMAL);
            emitter.setDisposable(AndroidRxUtils.disposeOnCallbacksScheduler(() -> {
                Log.d("RotationProvider: unregistering listener");
                sensorManager.unregisterListener(listener, rotationSensor);
            }));
        });
        return observable.subscribeOn(AndroidRxUtils.looperCallbacksScheduler).share();
    }

    public static boolean hasRotationSensor(final Context context) {
        return ((SensorManager) context.getSystemService(Context.SENSOR_SERVICE)).getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR) != null;
    }

}
