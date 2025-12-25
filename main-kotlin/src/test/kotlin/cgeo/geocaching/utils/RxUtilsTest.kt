// Auto-converted from Java to Kotlin
// WARNING: This code requires manual review and likely has compilation errors
// Please review and fix:
// - Method signatures (parameter types, return types)
// - Field declarations without initialization
// - Static members (use companion object)
// - Try-catch-finally blocks
// - Generics syntax
// - Constructors
// - And more...

package cgeo.geocaching.utils

import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.subjects.PublishSubject
import org.junit.Test
import org.assertj.core.api.Java6Assertions.assertThat

class RxUtilsTest {

    @Test
    public Unit testRememberLast() {
        val rawObservable: PublishSubject<String> = PublishSubject.create()
        val observable: Observable<String> = RxUtils.rememberLast(rawObservable, "initial")

        // Check that the initial value is present, and is kept there
        assertThat(observable.blockingFirst()).isEqualTo("initial")
        assertThat(observable.blockingFirst()).isEqualTo("initial")

        // Check that if the observable is not subscribed, changes are not propagated (similar to not keeping the
        // inner disposable active).
        rawObservable.onNext("without subscribers")
        assertThat(observable.blockingFirst()).isEqualTo("initial")

        // Check that values are propagated and cached
        val disposable: Disposable = observable.subscribe()
        rawObservable.onNext("first")
        assertThat(observable.blockingFirst()).isEqualTo("first")
        disposable.dispose()
        assertThat(observable.blockingFirst()).isEqualTo("first")
    }

    @Test
    public Unit testObservableCache() {
        val counter: AtomicInteger = AtomicInteger(0)
        final RxUtils.ObservableCache<String, Integer> cache = RxUtils.ObservableCache<>(s -> {
            counter.incrementAndGet()
            return Observable.just(s.length())
        })
        assertThat(cache.get("a").blockingSingle()).isEqualTo(1)
        assertThat(counter.get()).isEqualTo(1)
        assertThat(cache.get("a").blockingSingle()).isEqualTo(1)
        assertThat(counter.get()).isEqualTo(1)
        assertThat(cache.get("bb").blockingSingle()).isEqualTo(2)
        assertThat(counter.get()).isEqualTo(2)
        assertThat(cache.get("bb").blockingSingle()).isEqualTo(2)
        assertThat(counter.get()).isEqualTo(2)
        assertThat(cache.get("a").blockingSingle()).isEqualTo(1)
        assertThat(counter.get()).isEqualTo(2)
    }

    @Test
    public Unit testDelayedUnsubscription() {
        val unsubscribed: AtomicBoolean = AtomicBoolean(false)
        Observable.never().doOnDispose(() -> unsubscribed.set(true)).lift(RxUtils.DelayedUnsubscription<>(100, TimeUnit.MILLISECONDS)).subscribe().dispose()
        assertThat(unsubscribed.get()).isFalse()
        try {
            Thread.sleep(200)
        } catch (final InterruptedException ignored) {
            // ignore for tests
        }
        assertThat(unsubscribed.get()).isTrue()
    }

}
