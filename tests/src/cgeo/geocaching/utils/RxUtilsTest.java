package cgeo.geocaching.utils;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import junit.framework.TestCase;
import rx.Observable;
import rx.Single;
import rx.Subscription;
import rx.functions.Action0;
import rx.functions.Func0;
import rx.functions.Func1;
import rx.observers.TestSubscriber;
import rx.schedulers.TestScheduler;
import rx.subjects.PublishSubject;

public class RxUtilsTest extends TestCase {

    public static void testRememberLast() {
        final PublishSubject<String> rawObservable = PublishSubject.create();
        final Observable<String> observable = RxUtils.rememberLast(rawObservable, "initial");

        // Check that the initial value is present, and is kept there
        assertThat(observable.toBlocking().first()).isEqualTo("initial");
        assertThat(observable.toBlocking().first()).isEqualTo("initial");

        // Check that if the observable is not subscribed, changes are not propagated (similar to not keeping the
        // inner subscription active).
        rawObservable.onNext("without subscribers");
        assertThat(observable.toBlocking().first()).isEqualTo("initial");

        // Check that new values are propagated and cached
        final Subscription subscription = observable.subscribe();
        rawObservable.onNext("first");
        assertThat(observable.toBlocking().first()).isEqualTo("first");
        subscription.unsubscribe();
        assertThat(observable.toBlocking().first()).isEqualTo("first");
    }

    public static void testFromNullable() {
        final Observable<String> fromNull = RxUtils.fromNullable(null);
        assertThat(fromNull.toBlocking().getIterator().hasNext()).isFalse();

        final Observable<String> fromNonNull = RxUtils.fromNullable("foo");
        assertThat(fromNonNull.toBlocking().single()).isEqualTo("foo");
    }

    public static void testDeferredNullable() {
        final Observable<String> fromNull = RxUtils.deferredNullable(new Func0<String>() {
            @Override
            public String call() {
                return null;
            }
        });
        assertThat(fromNull.toBlocking().getIterator().hasNext()).isFalse();

        final Observable<String> fromNonNull = RxUtils.deferredNullable(new Func0<String>() {
            @Override
            public String call() {
                return "foo";
            }
        });
        assertThat(fromNonNull.toBlocking().single()).isEqualTo("foo");
    }

    public static void testObservableCache() {
        final AtomicInteger counter = new AtomicInteger(0);
        final RxUtils.ObservableCache<String, Integer> cache = new RxUtils.ObservableCache<>(new Func1<String, Observable<Integer>>() {
            @Override
            public Observable<Integer> call(final String s) {
                counter.incrementAndGet();
                return Observable.just(s.length());
            }
        });
        assertThat(cache.get("a").toBlocking().single()).isEqualTo(1);
        assertThat(counter.get()).isEqualTo(1);
        assertThat(cache.get("a").toBlocking().single()).isEqualTo(1);
        assertThat(counter.get()).isEqualTo(1);
        assertThat(cache.get("bb").toBlocking().single()).isEqualTo(2);
        assertThat(counter.get()).isEqualTo(2);
        assertThat(cache.get("bb").toBlocking().single()).isEqualTo(2);
        assertThat(counter.get()).isEqualTo(2);
        assertThat(cache.get("a").toBlocking().single()).isEqualTo(1);
        assertThat(counter.get()).isEqualTo(2);
    }

    public static void testDelayedUnsubscription() {
        final AtomicBoolean unsubscribed = new AtomicBoolean(false);
        Observable.never().doOnUnsubscribe(new Action0() {
            @Override
            public void call() {
                unsubscribed.set(true);
            }
        }).lift(new RxUtils.DelayedUnsubscription<>(100, TimeUnit.MILLISECONDS)).subscribe().unsubscribe();
        assertThat(unsubscribed.get()).isFalse();
        try {
            Thread.sleep(200);
        } catch (final InterruptedException ignored) {
            // ignore for tests
        }
        assertThat(unsubscribed.get()).isTrue();
    }

    public static void testNullableSingleValue() {
        final Single<Integer> single = Single.just(42);
        assertThat(RxUtils.nullableSingleValue(single)).isEqualTo(42);

        final Single<Integer> errorSingle = Single.error(new RuntimeException("error-ed single"));
        assertThat(RxUtils.nullableSingleValue(errorSingle)).isNull();
    }

    public static void testMergePreferredImmediate() {
        final Observable<Integer> o1 = Observable.range(0, 5);
        final Observable<Integer> o2 = Observable.range(5, 5);
        // take(100) is here to add some backpressure
        final List<Integer> merged = RxUtils.mergePreferred(o1, o2).take(100).toList().toBlocking().single();
        final List<Integer> expected = new LinkedList<>();
        for (int i = 0; i < 10; i++) {
            expected.add(i);
        }
        assertThat(merged).isEqualTo(expected);
    }

    public static void testMergePreferredDelayed() {
        final TestScheduler scheduler = new TestScheduler();
        final Observable<Integer> o1 = Observable.range(2, 3).delay(100, TimeUnit.MILLISECONDS, scheduler);
        final Observable<Integer> o2 = Observable.range(10, 5);
        final Observable<Integer> merged = RxUtils.mergePreferred(o1, o2);
        final TestSubscriber<Integer> subscriber = new TestSubscriber<>(0);
        merged.subscribe(subscriber);
        subscriber.assertNoValues();
        subscriber.requestMore(1);
        subscriber.assertValues(10);  // From non-preferred, preferred is not ready
        subscriber.requestMore(1);
        subscriber.assertValues(10, 11);  // From non-preferred, preferred is not ready
        scheduler.advanceTimeBy(120, TimeUnit.MILLISECONDS);
        subscriber.requestMore(100);
        subscriber.assertValues(10, 11, 2, 3, 4, 12, 13, 14);  // From non-preferred (preferred has completed)
        subscriber.assertCompleted();
    }

    public static void testMergePreferredError() {
        {
            final PublishSubject<Integer> o1 = PublishSubject.create();
            final PublishSubject<Integer> o2 = PublishSubject.create();
            final Observable<Integer> merged = RxUtils.mergePreferred(o1, o2);
            final TestSubscriber<Integer> subscriber = new TestSubscriber<>(0);
            merged.subscribe(subscriber);
            o1.onNext(1);
            o1.onNext(2);
            o2.onNext(11);
            o2.onNext(12);
            o2.onNext(13);
            subscriber.assertNoValues();
            subscriber.requestMore(3);
            subscriber.assertValues(1, 2, 11);
            o1.onNext(3);
            subscriber.requestMore(100);
            subscriber.assertValues(1, 2, 11, 3, 12, 13);
            o1.onError(new RuntimeException());
            subscriber.assertError(RuntimeException.class);
        }
        {
            final PublishSubject<Integer> o1 = PublishSubject.create();
            final PublishSubject<Integer> o2 = PublishSubject.create();
            final Observable<Integer> merged = RxUtils.mergePreferred(o1, o2);
            final TestSubscriber<Integer> subscriber = new TestSubscriber<>(0);
            merged.subscribe(subscriber);
            o1.onNext(1);
            o1.onNext(2);
            o2.onNext(11);
            o2.onNext(12);
            o2.onNext(13);
            subscriber.assertNoValues();
            subscriber.requestMore(3);
            subscriber.assertValues(1, 2, 11);
            o1.onNext(3);
            subscriber.requestMore(100);
            subscriber.assertValues(1, 2, 11, 3, 12, 13);
            o2.onError(new RuntimeException());
            subscriber.assertError(RuntimeException.class);
        }
    }

}
