package cgeo.geocaching.sensors;

import cgeo.geocaching.utils.RxUtils.LooperCallbacks;

import rx.Observable;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

public class DirectionProvider extends LooperCallbacks<Float> implements SensorEventListener {

    private final SensorManager sensorManager;
    private final Sensor orientationSensor;

    @SuppressWarnings("deprecation")
    protected DirectionProvider(final Context context) {
        sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        orientationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION);
    }

    @Override
    public void onSensorChanged(final SensorEvent event) {
            subscriber.onNext(event.values[0]);
    }

    @Override
    public void onAccuracyChanged(final Sensor sensor, final int accuracy) {
        /*
         * There is a bug in Android, which apparently causes this method to be called every
         * time the sensor _value_ changed, even if the _accuracy_ did not change. Do not have any code in here.
         *
         * See for example https://code.google.com/p/android/issues/detail?id=14792
         */
    }

    @Override
    public void onStart() {
        if (orientationSensor != null) {
            sensorManager.registerListener(this, orientationSensor, SensorManager.SENSOR_DELAY_NORMAL);
        }
    }

    @Override
    public void onStop() {
        if (orientationSensor != null) {
            sensorManager.unregisterListener(this);
        }
    }

    public static Observable<Float> create(final Context context) {
        return Observable.create(new DirectionProvider(context));
    }

}
