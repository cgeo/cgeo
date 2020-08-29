package cgeo.geocaching.sensors;

import cgeo.geocaching.utils.AndroidRxUtils;
import cgeo.geocaching.utils.Log;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.ObservableEmitter;
import io.reactivex.rxjava3.core.ObservableOnSubscribe;


public class MagnetometerAndAccelerometerProvider {

    private MagnetometerAndAccelerometerProvider() {
        // Utility class, not to be instantiated
    }

    public static Observable<DirectionData> create(final Context context) {

        final SensorManager sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        final Sensor accelerometerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        final Sensor magnetometerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        if (magnetometerSensor == null || accelerometerSensor == null) {
            return Observable.error(new RuntimeException("no magnetic or accelerometer sensor"));
        }
        final Observable<DirectionData> observable = Observable.create(new ObservableOnSubscribe<DirectionData>() {
            private final float[] lastAccelerometer = new float[3];
            private final float[] lastMagnetometer = new float[3];
            private boolean lastAccelerometerSet = false;
            private boolean lastMagnetometerSet = false;
            private final float[] rotateMatrix = new float[9];
            private final DirectionDataCalculator dirDataCalculator = new DirectionDataCalculator(rotateMatrix.length);

            @Override
            public void subscribe(final ObservableEmitter<DirectionData> emitter) {
                final SensorEventListener listener = new SensorEventListener() {
                    @Override
                    public void onSensorChanged(final SensorEvent sensorEvent) {

                        if (sensorEvent.sensor.equals(accelerometerSensor)) {
                            System.arraycopy(sensorEvent.values, 0, lastAccelerometer, 0, sensorEvent.values.length);
                            lastAccelerometerSet = true;
                        } else if (sensorEvent.sensor.equals(magnetometerSensor)) {
                            System.arraycopy(sensorEvent.values, 0, lastMagnetometer, 0, sensorEvent.values.length);
                            lastMagnetometerSet = true;
                        }
                        if (lastAccelerometerSet && lastMagnetometerSet) {
                            SensorManager.getRotationMatrix(rotateMatrix, null, lastAccelerometer, lastMagnetometer);

                            emitter.onNext(dirDataCalculator.calculateDirectionData(rotateMatrix));
                        }
                    }

                    @Override
                    public void onAccuracyChanged(final Sensor sensor, final int i) {
                        /*
                         * There is a bug in Android, which apparently causes this method to be called every
                         * time the sensor _value_ changed, even if the _accuracy_ did not change. Do not have any code in here.
                         *
                         * See for example https://code.google.com/p/android/issues/detail?id=14792
                         */
                    }
                };
                Log.d("MagnetometerAndAccelerometerProvider: registering listener");
                sensorManager.registerListener(listener, accelerometerSensor, SensorManager.SENSOR_DELAY_NORMAL);
                sensorManager.registerListener(listener, magnetometerSensor, SensorManager.SENSOR_DELAY_NORMAL);
                emitter.setDisposable(AndroidRxUtils.disposeOnCallbacksScheduler(() -> {
                    Log.d("MagnetometerAndAccelerometerProvider: unregistering listener");
                    sensorManager.unregisterListener(listener, accelerometerSensor);
                    sensorManager.unregisterListener(listener, magnetometerSensor);
                }));
            }
        });
        return observable.subscribeOn(AndroidRxUtils.looperCallbacksScheduler).share();
    }

    public static boolean hasMagnetometerAndAccelerometerSensors(final Context context) {
        final SensorManager sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        return sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD) != null &&
                sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) != null;
    }
}
