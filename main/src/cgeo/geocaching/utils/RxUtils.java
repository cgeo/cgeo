package cgeo.geocaching.utils;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

import rx.Observable;
import rx.Observable.Operator;
import rx.Scheduler;
import rx.Scheduler.Worker;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action0;
import rx.functions.Action1;
import rx.functions.Func0;
import rx.functions.Func1;
import rx.internal.util.RxThreadFactory;
import rx.observables.BlockingObservable;
import rx.schedulers.Schedulers;
import rx.subscriptions.Subscriptions;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Process;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

public class RxUtils {

    private RxUtils() {
        // Utility class, not to be instantiated
    }

    public final static Scheduler computationScheduler = Schedulers.computation();

    public static final Scheduler networkScheduler = Schedulers.from(Executors.newFixedThreadPool(10, new RxThreadFactory("network-")));

    public static final Scheduler refreshScheduler = Schedulers.from(Executors.newFixedThreadPool(3, new RxThreadFactory("refresh-")));

    private static final HandlerThread looperCallbacksThread =
            new HandlerThread("looper callbacks", Process.THREAD_PRIORITY_DEFAULT);

    static {
        looperCallbacksThread.start();
    }

    public static final Looper looperCallbacksLooper = looperCallbacksThread.getLooper();
    public static final Scheduler looperCallbacksScheduler = AndroidSchedulers.handlerThread(new Handler(looperCallbacksLooper));
    public static final Worker looperCallbacksWorker = looperCallbacksScheduler.createWorker();

    public static <T> void waitForCompletion(final BlockingObservable<T> observable) {
        observable.lastOrDefault(null);
    }

    public static void waitForCompletion(final Observable<?>... observables) {
        waitForCompletion(Observable.merge(observables).toBlocking());
    }

    public static<T> Observable<T> rememberLast(final Observable<T> observable, final T initialValue) {
        final AtomicReference<T> lastValue = new AtomicReference<>(initialValue);
        return observable.doOnNext(new Action1<T>() {
            @Override
            public void call(final T value) {
                lastValue.set(value);
            }
        }).startWith(Observable.defer(new Func0<Observable<T>>() {
            @Override
            public Observable<T> call() {
                final T last = lastValue.get();
                return last != null ? Observable.just(last) : Observable.<T>empty();
            }
        })).replay(1).refCount();
    }

    public static <T> void andThenOnUi(final Scheduler scheduler, final Func0<T> background, final Action1<T> foreground) {
        scheduler.createWorker().schedule(new Action0() {
            @Override
            public void call() {
                final T value = background.call();
                AndroidSchedulers.mainThread().createWorker().schedule(new Action0() {
                    @Override
                    public void call() {
                        foreground.call(value);
                    }
                });
            }
        });
    }

    public static void andThenOnUi(final Scheduler scheduler, final Action0 background, final Action0 foreground) {
        scheduler.createWorker().schedule(new Action0() {
            @Override
            public void call() {
                background.call();
                AndroidSchedulers.mainThread().createWorker().schedule(foreground);
            }
        });
    }

    /**
     * Cache the last value of observables so that every key is associated to only one of them.
     *
     * @param <K> the type of the key
     * @param <V> the type of the value
     */
    public static class ObservableCache<K, V> {

        final private Func1<K, Observable<V>> func;
        final private Map<K, Observable<V>> cached = new HashMap<>();

        /**
         * Create a new observables cache.
         *
         * @param func the function transforming a key into an observable
         */
        public ObservableCache(final Func1<K, Observable<V>> func) {
            this.func = func;
        }

        /**
         * Get the observable corresponding to a key. If the key has not already been
         * seen, the function passed to the constructor will be called to build the observable
         * <p/>
         * If the observable has already emitted values, only the last one will be remembered.
         *
         * @param key the key
         * @return the observable corresponding to the key
         */
        public synchronized Observable<V> get(final K key) {
            if (cached.containsKey(key)) {
                return cached.get(key);
            }
            final Observable<V> value = func.call(key).replay(1).refCount();
            cached.put(key, value);
            return value;
        }

    }

    /**
     * Transform a nullable value into an observable with 0 or 1 element.
     *
     * @param value the value to be returned, or {@code null}
     * @return an observable with only {@code value} if it is not {@code null}, none otherwise
     */
    @NonNull
    public static <T> Observable<T> fromNullable(@Nullable final T value) {
        return value != null ? Observable.just(value) : Observable.<T>empty();
    }

    /**
     * Transform a nullable return value into an observable with 0 or 1 element.
     *
     * @param func the function to call
     * @return  an observable with only the result of calling {@code func} if it is not {@code null}, none otherwise
     */
    public static <T> Observable<T> deferredNullable(@NonNull final Func0<T> func) {
        return Observable.defer(new Func0<Observable<T>>() {
            @Override
            public Observable<T> call() {
                return fromNullable(func.call());
            }
        });
    }

    public static class DelayedUnsubscription<T> implements Operator<T, T> {

        final private long time;
        final private TimeUnit unit;

        public DelayedUnsubscription(final long time, final TimeUnit unit) {
            this.time = time;
            this.unit = unit;
        }

        @Override
        public Subscriber<? super T> call(final Subscriber<? super T> subscriber) {
            final Subscriber<T> transformed = new Subscriber<T>(subscriber, false) {

                @Override
                public void onCompleted() {
                    if (!subscriber.isUnsubscribed()) {
                        subscriber.onCompleted();
                    }
                }

                @Override
                public void onError(final Throwable e) {
                    if (!subscriber.isUnsubscribed()) {
                        subscriber.onError(e);
                    }
                }

                @Override
                public void onNext(final T t) {
                    if (!subscriber.isUnsubscribed()) {
                        subscriber.onNext(t);
                    }
                }
            };
            subscriber.add(Subscriptions.create(new Action0() {
                @Override
                public void call() {
                    Schedulers.computation().createWorker().schedule(new Action0() {
                        @Override
                        public void call() {
                            transformed.unsubscribe();
                        }
                    }, time, unit);
                }
            }));
            transformed.add(subscriber);
            return transformed;
        }
    }

}
