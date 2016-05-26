package cgeo.geocaching.utils;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import rx.Observable;
import rx.Observable.OnSubscribe;
import rx.Observable.Operator;
import rx.Producer;
import rx.Single;
import rx.Subscriber;
import rx.functions.Action0;
import rx.functions.Action1;
import rx.functions.Func0;
import rx.functions.Func1;
import rx.schedulers.Schedulers;
import rx.subscriptions.CompositeSubscription;
import rx.subscriptions.Subscriptions;

public class RxUtils {

    private RxUtils() {
        // Utility class, not to be instantiated
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
     * @return an observable with only the result of calling {@code func} if it is not {@code null}, none otherwise
     */
    public static <T> Observable<T> deferredNullable(@NonNull final Func0<T> func) {
        return Observable.defer(new Func0<Observable<T>>() {
            @Override
            public Observable<T> call() {
                return fromNullable(func.call());
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

        private final Func1<K, Observable<V>> func;
        private final Map<K, Observable<V>> cached = new HashMap<>();

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

    public static class DelayedUnsubscription<T> implements Operator<T, T> {

        private final long time;
        private final TimeUnit unit;

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
            return single.toBlocking().value();
        } catch (final Throwable ignored) {
            return null;
        }
    }

    /**
     * Merge two observables respecting the subscribing demand, preferring items coming from the first
     * observable over the ones from the second observable. Backpressure is propagated downstream to both
     * obervables, with one extra item which is internally buffered for the preferred observable.
     *
     * Errors from either observables are propagated as soon as they are transmitted to this operator.
     *
     * @param preferredObservable the preferred observable
     * @param otherObservable the other observable
     * @param <T> the type of the Observable content
     * @return the merged observable
     */
    @NonNull
    public static <T> Observable<T> mergePreferred(final Observable<T> preferredObservable, final Observable<T> otherObservable) {
        return Observable.create(new OnSubscribe<T>() {
            @Override
            public void call(final Subscriber<? super T> subscriber) {

                class TProducer implements Producer {

                    final TSubscriber preferredSubscriber = new TSubscriber(1);
                    final TSubscriber otherSubscriber = new TSubscriber(0);
                    final AtomicLong totalDemand = new AtomicLong(0);
                    final CompositeSubscription innerSubscriptions = new CompositeSubscription();

                    TProducer() {
                        innerSubscriptions.add(preferredObservable.subscribe(preferredSubscriber));
                        innerSubscriptions.add(otherObservable.subscribe(otherSubscriber));
                    }

                    class TSubscriber extends Subscriber<T> {

                        private final Deque<T> fetched = new LinkedList<>();
                        private final AtomicBoolean terminated = new AtomicBoolean(false);
                        private final AtomicLong demanded = new AtomicLong(1);
                        private final long extra;

                        TSubscriber(final long extra) {
                            this.extra = extra;
                        }

                        @Override
                        public void onStart() {
                            super.onStart();
                            dequeue();
                        }

                        @Override
                        public void onCompleted() {
                            terminated.set(true);
                        }

                        @Override
                        public void onError(final Throwable e) {
                            terminated.set(true);
                            subscriber.onError(e);
                            innerSubscriptions.unsubscribe();
                        }

                        @Override
                        public void onNext(final T t) {
                            synchronized (TProducer.this) {
                                fetched.offer(t);
                            }
                            demanded.decrementAndGet();
                            dequeue();
                        }

                        public void dequeue() {
                            synchronized (TProducer.this) {
                                while (!fetched.isEmpty() && totalDemand.get() > 0) {
                                    if (totalDemand.getAndDecrement() > 0) {
                                        subscriber.onNext(fetched.pop());
                                    } else {
                                        totalDemand.incrementAndGet();
                                        break;
                                    }
                                }
                                final long missing = totalDemand.get() - demanded.get() - fetched.size() + extra;
                                if (missing > 0 && !terminated.get()) {
                                    request(missing);
                                }
                            }
                        }

                        public boolean isTerminated() {
                            synchronized (TProducer.this) {
                                return fetched.isEmpty() && terminated.get();
                            }
                        }
                    }

                    @Override
                    public void request(final long n) {
                        if (n == Long.MAX_VALUE) {
                            throw new IllegalArgumentException("mergePreferred must be used with backpressure, or merge should be used instead");
                        }
                        totalDemand.addAndGet(n);
                        preferredSubscriber.dequeue();
                        otherSubscriber.dequeue();
                        if (preferredSubscriber.isTerminated() && otherSubscriber.isTerminated()) {
                            subscriber.onCompleted();
                        }
                    }
                }

                final TProducer producer = new TProducer();

                subscriber.add(producer.innerSubscriptions);
                subscriber.setProducer(producer);

            }

        });
    }
}
