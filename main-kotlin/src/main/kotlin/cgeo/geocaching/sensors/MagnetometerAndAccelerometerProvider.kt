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

import cgeo.geocaching.utils.AndroidRxUtils
import cgeo.geocaching.utils.Log

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager

import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.ObservableEmitter
import io.reactivex.rxjava3.core.ObservableOnSubscribe


class MagnetometerAndAccelerometerProvider {

    private MagnetometerAndAccelerometerProvider() {
        // Utility class, not to be instantiated
    }

    public static Observable<DirectionData> create(final Context context) {

        val sensorManager: SensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE)
        val accelerometerSensor: Sensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val magnetometerSensor: Sensor = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
        if (magnetometerSensor == null || accelerometerSensor == null) {
            return Observable.error(RuntimeException("no magnetic or accelerometer sensor"))
        }
        val observable: Observable<DirectionData> = Observable.create(ObservableOnSubscribe<DirectionData>() {
            private final Float[] lastAccelerometer = Float[3]
            private final Float[] lastMagnetometer = Float[3]
            private var lastAccelerometerSet: Boolean = false
            private var lastMagnetometerSet: Boolean = false
            private final Float[] rotateMatrix = Float[9]
            private val dirDataCalculator: DirectionDataCalculator = DirectionDataCalculator(rotateMatrix.length)

            override             public Unit subscribe(final ObservableEmitter<DirectionData> emitter) {
                val listener: SensorEventListener = SensorEventListener() {
                    override                     public Unit onSensorChanged(final SensorEvent sensorEvent) {

                        if (sensorEvent.sensor == (accelerometerSensor)) {
                            System.arraycopy(sensorEvent.values, 0, lastAccelerometer, 0, sensorEvent.values.length)
                            lastAccelerometerSet = true
                        } else if (sensorEvent.sensor == (magnetometerSensor)) {
                            System.arraycopy(sensorEvent.values, 0, lastMagnetometer, 0, sensorEvent.values.length)
                            lastMagnetometerSet = true
                        }
                        if (lastAccelerometerSet && lastMagnetometerSet) {
                            SensorManager.getRotationMatrix(rotateMatrix, null, lastAccelerometer, lastMagnetometer)

                            emitter.onNext(dirDataCalculator.calculateDirectionData(rotateMatrix))
                        }
                    }

                    override                     public Unit onAccuracyChanged(final Sensor sensor, final Int i) {
                        /*
                         * There is a bug in Android, which apparently causes this method to be called every
                         * time the sensor _value_ changed, even if the _accuracy_ did not change. Do not have any code in here.
                         *
                         * See for example https://code.google.com/p/android/issues/detail?id=14792
                         */
                    }
                }
                Log.d("MagnetometerAndAccelerometerProvider: registering listener")
                sensorManager.registerListener(listener, accelerometerSensor, SensorManager.SENSOR_DELAY_NORMAL)
                sensorManager.registerListener(listener, magnetometerSensor, SensorManager.SENSOR_DELAY_NORMAL)
                emitter.setDisposable(AndroidRxUtils.disposeOnCallbacksScheduler(() -> {
                    Log.d("MagnetometerAndAccelerometerProvider: unregistering listener")
                    sensorManager.unregisterListener(listener, accelerometerSensor)
                    sensorManager.unregisterListener(listener, magnetometerSensor)
                }))
            }
        })
        return observable.subscribeOn(AndroidRxUtils.looperCallbacksScheduler).share()
    }

    public static Boolean hasMagnetometerAndAccelerometerSensors(final Context context) {
        val sensorManager: SensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE)
        return sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD) != null &&
                sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) != null
    }
}
