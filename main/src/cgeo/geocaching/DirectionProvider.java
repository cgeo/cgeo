package cgeo.geocaching;

import cgeo.geocaching.compatibility.Compatibility;

import rx.Observable;
import rx.Observable.OnSubscribe;
import rx.Subscriber;
import rx.Subscription;
import rx.observables.ConnectableObservable;
import rx.subjects.BehaviorSubject;
import rx.subscriptions.Subscriptions;
import rx.util.functions.Action0;

import android.app.Activity;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

public class DirectionProvider implements OnSubscribe<Float> {

    private final SensorManager sensorManager;
    private final BehaviorSubject<Float> subject = BehaviorSubject.create(0.0f);

    static public Observable<Float> create(final Context context) {
        return new DirectionProvider((SensorManager) context.getSystemService(Context.SENSOR_SERVICE)).worker.refCount();
    }

    private DirectionProvider(final SensorManager sensorManager) {
        this.sensorManager = sensorManager;
    }

    @Override
    public void call(final Subscriber<? super Float> subscriber) {
        subject.distinctUntilChanged().subscribe(subscriber);
    }

    private final ConnectableObservable<Float> worker = new ConnectableObservable<Float>(this) {
        @Override
        public Subscription connect() {
            @SuppressWarnings("deprecation")
            // This will be removed when using a new location service. Until then, it is okay to be used.
            final Sensor defaultSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION);
            final SensorEventListener listener = new SensorEventListener() {
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
            };

            sensorManager.registerListener(listener, defaultSensor, SensorManager.SENSOR_DELAY_NORMAL);
            return Subscriptions.create(new Action0() {
                @Override
                public void call() {
                    sensorManager.unregisterListener(listener);
                }
            });
        }
    };

    /**
     * Take the phone rotation (through a given activity) in account and adjust the direction.
     *
     * @param activity the activity to consider when computing the rotation
     * @param direction the unadjusted direction in degrees, in the [0, 360[ range
     * @return the adjusted direction in degrees, in the [0, 360[ range
     */

    public static float getDirectionNow(final Activity activity, final float direction) {
        return Compatibility.getDirectionNow(direction, activity);
    }

}
