package cgeo.geocaching.sensors;

import cgeo.geocaching.utils.Log;
import cgeo.geocaching.utils.RxUtils;

import rx.Observable;
import rx.Observable.OnSubscribe;
import rx.Subscriber;
import rx.functions.Action0;
import rx.subscriptions.Subscriptions;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

public class OrientationProvider {

    private OrientationProvider() {
        // Utility class, not to be instantiated
    }

    @SuppressWarnings("deprecation")
    public static boolean hasOrientationSensor(final Context context) {
        return ((SensorManager) context.getSystemService(Context.SENSOR_SERVICE)).getDefaultSensor(Sensor.TYPE_ORIENTATION) != null;
    }

    @SuppressWarnings("deprecation")
    public static Observable<Float> create(final Context context) {
        final SensorManager sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        final Sensor orientationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION);
        if (orientationSensor == null) {
            return Observable.error(new RuntimeException("no orientation sensor"));
        }
        Log.d("OrientationProvider: sensor found");
        final Observable<Float> observable = Observable.create(new OnSubscribe<Float>() {
            @Override
            public void call(final Subscriber<? super Float> subscriber) {
                final SensorEventListener listener = new SensorEventListener() {
                    @Override
                    public void onSensorChanged(final SensorEvent sensorEvent) {
                        subscriber.onNext(sensorEvent.values[0]);
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
                Log.d("OrientationProvider: registering listener");
                sensorManager.registerListener(listener, orientationSensor, SensorManager.SENSOR_DELAY_NORMAL);
                subscriber.add(Subscriptions.create(new Action0() {
                    @Override
                    public void call() {
                        RxUtils.looperCallbacksWorker.schedule(new Action0() {
                            @Override
                            public void call() {
                                Log.d("OrientationProvider: unregistering listener");
                                sensorManager.unregisterListener(listener, orientationSensor);
                            }
                        });
                    }
                }));
            }
        });
        return observable.subscribeOn(RxUtils.looperCallbacksScheduler).share().onBackpressureLatest();
    }

}
