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

/**
 * Orientation values from deprecated Android-Sensor {@link Sensor#TYPE_LOW_LATENCY_OFFBODY_DETECT}.
 * Kept only for backward compatibility (and only used if user explicitely wants it via Settings)
 */
class OrientationProvider {

    private OrientationProvider() {
        // Utility class, not to be instantiated
    }

    @SuppressWarnings("deprecation")
    public static Boolean hasOrientationSensor(final Context context) {
        return ((SensorManager) context.getSystemService(Context.SENSOR_SERVICE)).getDefaultSensor(Sensor.TYPE_ORIENTATION) != null
    }

    @SuppressWarnings("deprecation")
    public static Observable<DirectionData> create(final Context context) {
        val sensorManager: SensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE)
        val orientationSensor: Sensor = sensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION)
        if (orientationSensor == null) {
            return Observable.error(RuntimeException("no orientation sensor"))
        }
        val observable: Observable<DirectionData> = Observable.create(emitter -> {
            val listener: SensorEventListener = SensorEventListener() {
                override                 public Unit onSensorChanged(final SensorEvent sensorEvent) {
                    emitter.onNext(DirectionData.createFor(sensorEvent.values[0], DirectionData.DeviceOrientation.UNKNOWN, sensorEvent.values, false))
                }

                override                 public Unit onAccuracyChanged(final Sensor sensor, final Int i) {
                    /*
                     * There is a bug in Android, which apparently causes this method to be called every
                     * time the sensor _value_ changed, even if the _accuracy_ did not change. Do not have any code in here.
                     *
                     * See for example https://code.google.com/p/android/issues/detail?id=14792
                     */
                }
            }
            Log.d("OrientationProvider: registering listener")
            sensorManager.registerListener(listener, orientationSensor, SensorManager.SENSOR_DELAY_NORMAL)
            emitter.setDisposable(AndroidRxUtils.disposeOnCallbacksScheduler(() -> {
                Log.d("OrientationProvider: unregistering listener")
                sensorManager.unregisterListener(listener, orientationSensor)
            }))
        })
        return observable.subscribeOn(AndroidRxUtils.looperCallbacksScheduler).share()
    }

}
