package cgeo.geocaching.utils;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import io.reactivex.Maybe;
import io.reactivex.Observable;
import io.reactivex.ObservableOperator;
import io.reactivex.Observer;
import io.reactivex.Single;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Cancellable;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.internal.disposables.CancellableDisposable;

public class RxUtils {

    private RxUtils() {
        // Utility class, not to be instantiated
    }

    public static<T> Observable<T> rememberLast(final Observable<T> observable, final T initialValue) {
        final AtomicReference<T> lastValue = new AtomicReference<>(initialValue);
        return observable.doOnNext(new Consumer<T>() {
            @Override
            public void accept(final T value) {
                lastValue.set(value);
            }
        }).startWith(Observable.defer(new Callable<Observable<T>>() {
            @Override
            public Observable<T> call() {
                final T last = lastValue.get();
                return last != null ? Observable.just(last) : Observable.<T>empty();
            }
        })).replay(1).refCount();
    }

    /**
     * Transform a nullable value into a Maybe
     *
     * @param value the value to be returned, or {@code null}
     * @return a Maybe with only {@code value} if it is not {@code null}, empty otherwise
     */
    @NonNull
    public static <T> Maybe<T> fromNullable(@Nullable final T value) {
        return value != null ? Maybe.just(value) : Maybe.<T>empty();
    }

    /**
     * Transform a nullable return value into a Maybe
     *
     * @param func the function to call
     * @return a Maybe with only the result of calling {@code func} if it is not {@code null}, empty otherwise
     */
    public static <T> Maybe<T> deferredNullable(@NonNull final Callable<T> func) {
        return Maybe.defer(new Callable<Maybe<T>>() {
            @Override
            public Maybe<T> call() {
                try {
                    return fromNullable(func.call());
                } catch (final Exception e) {
                    return Maybe.error(e);
                }
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
            } catch (final Exception e) {
                final Observable<V> error = Observable.error(e);
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
                    observer.onSubscribe(new CancellableDisposable(new Cancellable() {
                        @Override
                        public void cancel() throws Exception {
                            canceled.set(true);
                            AndroidRxUtils.computationScheduler.scheduleDirect(new Runnable() {
                                @Override
                                public void run() {
                                    d.dispose();
                                }
                            }, time, unit);
                        }
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

    /**
     * Block until a Single is resolved, and return either the value if it succeeds or `null` if it fails.
     *
     * @param single the single to wait for
     * @param <T> the type of the Single content
     * @return the resolved value or `null` if an error occurred
     */
    @Nullable
    public static <T> T nullableSingleValue(final Single<T> single) {
        try {
            return single.blockingGet();
        } catch (final Throwable ignored) {
            return null;
        }
    }

}
