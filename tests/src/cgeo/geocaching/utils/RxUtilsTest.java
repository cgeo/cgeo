package cgeo.geocaching.utils;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import junit.framework.TestCase;
import rx.Observable;
import rx.Subscription;
import rx.functions.Action0;
import rx.functions.Func0;
import rx.functions.Func1;
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

    public static void testWaitForCompletion() {
        final PublishSubject<String> observable = PublishSubject.create();
        final AtomicBoolean terminated = new AtomicBoolean(false);
        new Thread() {
            @Override
            public void run() {
                RxUtils.waitForCompletion(observable.toBlocking());
                terminated.set(true);
            }
        }.start();
        observable.onNext("foo");
        assertThat(terminated.get()).isFalse();
        observable.onNext("bar");
        assertThat(terminated.get()).isFalse();
        observable.onCompleted();
        try {
            Thread.sleep(100);
        } catch (final InterruptedException ignored) {
        }
        assertThat(terminated.get()).isTrue();
    }

    public static void testObservableCache() {
        final AtomicInteger counter = new AtomicInteger(0);
        final RxUtils.ObservableCache<String, Integer> cache = new RxUtils.ObservableCache<String, Integer>(new Func1<String, Observable<Integer>>() {
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
        }).lift(new RxUtils.DelayedUnsubscription<Object>(100, TimeUnit.MILLISECONDS)).subscribe().unsubscribe();
        assertThat(unsubscribed.get()).isFalse();
        try {
            Thread.sleep(200);
        } catch (final InterruptedException ignored) {
            // ignore for tests
        }
        assertThat(unsubscribed.get()).isTrue();
    }

}
