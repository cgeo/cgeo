package cgeo.geocaching;

import cgeo.geocaching.compatibility.Compatibility;

import android.app.Activity;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

public class cgDirection {
    private cgDirection dir = null;
    private Context context = null;
    private SensorManager sensorManager = null;
    private cgeoSensorListener sensorListener = null;
    private cgUpdateDir dirUpdate = null;

    public Float directionNow = null;

    public cgDirection(Context contextIn, cgUpdateDir dirUpdateIn) {
        context = contextIn;
        dirUpdate = dirUpdateIn;
        sensorListener = new cgeoSensorListener();
    }

    public void initDir() {
        dir = this;

        if (sensorManager == null) {
            sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        }
        sensorManager.registerListener(sensorListener, sensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION), SensorManager.SENSOR_DELAY_NORMAL);
    }

    public void closeDir() {
        if (sensorManager != null && sensorListener != null) {
            sensorManager.unregisterListener(sensorListener);
        }
    }

    public void replaceUpdate(cgUpdateDir dirUpdateIn) {
        dirUpdate = dirUpdateIn;

        if (dirUpdate != null && directionNow != null) {
            dirUpdate.updateDir(dir);
        }
    }

    private class cgeoSensorListener implements SensorEventListener {
        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
            /*
             * There is a bug in Android, which appearently causes this method to be called every
             * time the sensor _value_ changed, even if the _accuracy_ did not change. So logging
             * this event leads to the log being flooded with multiple entries _per second_,
             * which I experienced when running cgeo in a building (with GPS and network being
             * unreliable).
             *
             * See for example https://code.google.com/p/android/issues/detail?id=14792
             */

            //Log.i(cgSettings.tag, "Compass' accuracy is low (" + accuracy + ")");
        }

        @Override
        public void onSensorChanged(SensorEvent event) {
            directionNow = Compatibility.getDirectionNow(event.values[0], (Activity) context);

            if (dirUpdate != null && directionNow != null) {
                dirUpdate.updateDir(dir);
            }
        }
    }
}
