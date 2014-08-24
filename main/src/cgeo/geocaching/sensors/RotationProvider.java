package cgeo.geocaching.sensors;

import cgeo.geocaching.utils.Log;
import cgeo.geocaching.utils.RxUtils.LooperCallbacks;

import rx.Observable;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

public class RotationProvider extends LooperCallbacks<Float> implements SensorEventListener {

    private final SensorManager sensorManager;
    private final Sensor rotationSensor;
    private final float[] rotationMatrix = new float[16];
    private final float[] orientation = new float[4];

    protected RotationProvider(final Context context) {
        sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        rotationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
        if (rotationSensor != null) {
            Log.d("RotationProvider: sensor found");
        } else {
            Log.w("RotationProvider: no rotation sensor on this device");
        }
    }

    @Override
    public void onSensorChanged(final SensorEvent event) {
        SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values);
        SensorManager.getOrientation(rotationMatrix, orientation);
        subscriber.onNext((float) (orientation[0] * 180 / Math.PI));
    }

    @Override
    public void onAccuracyChanged(final Sensor sensor, final int accuracy) {
    }

    @Override
    public void onStart() {
        if (rotationSensor != null) {
            Log.d("RotationProvider: starting the rotation provider");
            sensorManager.registerListener(this, rotationSensor, SensorManager.SENSOR_DELAY_NORMAL);
        } else {
            subscriber.onError(new RuntimeException("rotation sensor is absent on this device"));
        }
    }

    @Override
    public void onStop() {
        if (rotationSensor != null) {
            Log.d("RotationProvider: stopping the rotation provider");
            sensorManager.unregisterListener(this);
        }
    }

    public static Observable<Float> create(final Context context) {
        return Observable.create(new RotationProvider(context));
    }

}
