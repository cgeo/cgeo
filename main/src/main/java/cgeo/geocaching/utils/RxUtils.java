package cgeo.geocaching.utils;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.ObservableOperator;
import io.reactivex.rxjava3.core.Observer;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.functions.Function;
import io.reactivex.rxjava3.internal.disposables.CancellableDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class RxUtils {

    private RxUtils() {
        // Utility class, not to be instantiated
    }

    public static <T> Observable<T> rememberLast(final Observable<T> observable, final T initialValue) {
        final AtomicReference<T> lastValue = new AtomicReference<>(initialValue);
        return observable.doOnNext(lastValue::set).startWith(Observable.defer(() -> {
            final T last = lastValue.get();
            return last != null ? Observable.just(last) : Observable.empty();
        })).replay(1).refCount();
    }

    /**
     * Cache the last value of observables so that every key is associated to only one of them.
     *
     * @param <K> the type of the key
     * @param <V> the type of the value
     */
    public static class ObservableCache<K, V> {

        private final Function<K, Observable<V>> func;
        private final Map<K, Observable<V>> cached = new HashMap<>();

        /**
         * Create a new observables cache.
         *
         * @param func the function transforming a key into an observable
         */
        public ObservableCache(final Function<K, Observable<V>> func) {
            this.func = func;
        }

        /**
         * Get the observable corresponding to a key. If the key has not already been
         * seen, the function passed to the constructor will be called to build the observable
         * <p/>
         * If the observable has already emitted values, only the last one will be remembered.
         * <p/>
         * If the function throws an exception, it will be returned in the observable.
         *
         * @param key the key
         * @return the observable corresponding to the key
         */
        public synchronized Observable<V> get(final K key) {
            if (cached.containsKey(key)) {
                return cached.get(key);
            }
            try {
                final Observable<V> value = func.apply(key).replay(1).refCount();
                cached.put(key, value);
                return value;
            } catch (final Throwable t) {
                final Observable<V> error = Observable.error(t);
                cached.put(key, error);
                return error;
            }
        }

    }

    public static class DelayedUnsubscription<T> implements ObservableOperator<T, T> {

        private final long time;
        private final TimeUnit unit;

        public DelayedUnsubscription(final long time, final TimeUnit unit) {
            this.time = time;
            this.unit = unit;
        }

        @Override
        public Observer<? super T> apply(final Observer<? super T> observer) throws Exception {
            final AtomicBoolean canceled = new AtomicBoolean();

            return new Observer<T>() {

                @Override
                public void onSubscribe(final Disposable d) {
                    observer.onSubscribe(new CancellableDisposable(() -> {
                        canceled.set(true);
                        Schedulers.computation().scheduleDirect(d::dispose, time, unit);
                    }));
                }

                @Override
                public void onComplete() {
                    if (!canceled.get()) {
                        observer.onComplete();
                    }
                }

                @Override
                public void onError(final Throwable e) {
                    if (!canceled.get()) {
                        observer.onError(e);
                    }
                }

                @Override
                public void onNext(final T t) {
                    if (!canceled.get()) {
                        observer.onNext(t);
                    }
                }
            };
        }
    }

}
