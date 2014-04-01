package cgeo.geocaching.sensors;

import cgeo.geocaching.utils.AngleUtils;
import cgeo.geocaching.utils.StartableHandlerThread;

import rx.Observable;
import rx.Observable.OnSubscribe;
import rx.Subscriber;
import rx.subjects.BehaviorSubject;

import android.app.Activity;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;
import android.os.Process;
import android.view.Surface;

public class DirectionProvider {

    private static final BehaviorSubject<Float> subject = BehaviorSubject.create(0.0f);

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

        // This will be removed when using a new location service. Until then, it is okay to be used.
        @SuppressWarnings("deprecation")
        @Override
        public void start(final Context context, final Handler handler) {
            if (++count == 1) {
                sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
                sensorManager.registerListener(this, sensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION), SensorManager.SENSOR_DELAY_NORMAL, handler);
            }
        }

        @Override
        public void stop() {
            if (--count == 0) {
                sensorManager.unregisterListener(this);
            }
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
     * @param activity the activity to consider when computing the rotation
     * @param direction the unadjusted direction in degrees, in the [0, 360[ range
     * @return the adjusted direction in degrees, in the [0, 360[ range
     */

    public static float getDirectionNow(final Activity activity, final float direction) {
        return AngleUtils.normalize(direction + getRotationOffset(activity));
    }

    private static int getRotationOffset(final Activity activity) {
        switch (activity.getWindowManager().getDefaultDisplay().getRotation()) {
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
