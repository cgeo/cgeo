package cgeo.geocaching.sensors;

import cgeo.geocaching.CgeoApplication;
import cgeo.geocaching.utils.AngleUtils;
import cgeo.geocaching.utils.StartableHandlerThread;

import rx.Observable;
import rx.Observable.OnSubscribe;
import rx.Subscriber;
import rx.subjects.BehaviorSubject;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;
import android.os.Process;
import android.view.Surface;
import android.view.WindowManager;

public class DirectionProvider {

    private static final BehaviorSubject<Float> subject = BehaviorSubject.create(0.0f);

    private static final WindowManager windowManager = (WindowManager) CgeoApplication.getInstance().getSystemService(Context.WINDOW_SERVICE);

    static class Listener implements SensorEventListener, StartableHandlerThread.Callback {

        private int count = 0;

        private SensorManager sensorManager;

        @Override
        public void onSensorChanged(final SensorEvent event) {
            subject.onNext(event.values[0]);
        }

        @Override
        public void onAccuracyChanged(final Sensor sensor, final int accuracy) {
                /*
                * There is a bug in Android, which apparently causes this method to be called every
                * time the sensor _value_ changed, even if the _accuracy_ did not change. So logging
                * this event leads to the log being flooded with multiple entries _per second_,
                * which I experienced when running cgeo in a building (with GPS and network being
                * unreliable).
                *
                * See for example https://code.google.com/p/android/issues/detail?id=14792
                */

            //Log.i(Settings.tag, "Compass' accuracy is low (" + accuracy + ")");
        }

        @Override
        public void start(final Context context, final Handler handler) {
            if (!hasSensor(context)) {
                return;
            }
            if (++count == 1) {
                Sensor orientationSensor = getOrientationSensor(context);
                sensorManager.registerListener(this, orientationSensor, SensorManager.SENSOR_DELAY_NORMAL, handler);
            }
        }

        @Override
        public void stop() {
            if (!hasSensor) {
                return;
            }
            if (--count == 0) {
                sensorManager.unregisterListener(this);
            }
        }

        /**
         * Assume that there is an orientation sensor, unless we have really checked that
         */
        private boolean hasSensor = true;

        /**
         * Flag for one time check if there is a sensor.
         */
        private boolean hasSensorChecked = false;

        public boolean hasSensor(Context context) {
            if (hasSensorChecked == false) {
                hasSensor = getOrientationSensor(context) != null;
                hasSensorChecked = true;
            }
            return hasSensor;
        }

        // This will be removed when using a new location service. Until then, it is okay to be used.
        @SuppressWarnings("deprecation")
        private Sensor getOrientationSensor(final Context context) {
            sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
            return sensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION);
        }

    }

    private static final StartableHandlerThread handlerThread =
            new StartableHandlerThread("DirectionProvider thread", Process.THREAD_PRIORITY_BACKGROUND, new Listener());

    static {
      handlerThread.start();
    }
    static public Observable<Float> create(final Context context) {
        return Observable.create(new OnSubscribe<Float>() {
            @Override
            public void call(final Subscriber<? super Float> subscriber) {
                handlerThread.start(subscriber, context);
                subject.subscribe(subscriber);
            }
        });
    }

    /**
     * Take the phone rotation (through a given activity) in account and adjust the direction.
     *
     * @param direction the unadjusted direction in degrees, in the [0, 360[ range
     * @return the adjusted direction in degrees, in the [0, 360[ range
     */

    public static float getDirectionNow(final float direction) {
        return AngleUtils.normalize(direction + getRotationOffset());
    }

    static float reverseDirectionNow(final float direction) {
        return AngleUtils.normalize(direction - getRotationOffset());
    }

    private static int getRotationOffset() {
        switch (windowManager.getDefaultDisplay().getRotation()) {
            case Surface.ROTATION_90:
                return 90;
            case Surface.ROTATION_180:
                return 180;
            case Surface.ROTATION_270:
                return 270;
            default:
                return 0;
        }
    }

}
