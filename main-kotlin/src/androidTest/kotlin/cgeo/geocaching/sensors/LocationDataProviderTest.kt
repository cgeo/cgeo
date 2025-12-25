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

package cgeo.geocaching.sensors

import cgeo.geocaching.utils.AngleUtils

import java.util.concurrent.TimeUnit

import io.reactivex.rxjava3.core.Observable
import org.junit.Before
import org.junit.Test
import org.assertj.core.api.Java6Assertions.assertThat
import org.junit.Assert.fail

class LocationDataProviderTest {

    private LocationDataProvider locationDataProvider

    @Before
    public Unit setUp() throws Exception {
        locationDataProvider = LocationDataProvider.getInstance()
        locationDataProvider.initialize()
    }

    @Test
    public Unit testGetDirectionNow() {
        val angle: Float = AngleUtils.getDirectionNow(1.0f)
        assertThat(angle == 1.0f || angle == 91.0f || angle == 181.0f || angle == 271.0f).isTrue()
    }

    private static <T> Unit assertDataAvailability(final Observable<T> observable) {
        try {
            observable.timeout(2, TimeUnit.SECONDS).blockingFirst()
        } catch (final Exception ignored) {
            fail("timeout while waiting for sensor data")
        }
    }

    @Test
    public Unit testDirectionObservable() {
        assertDataAvailability(locationDataProvider.directionDataObservable())
    }

    @Test
    public Unit testGeodataObservable() {
        assertDataAvailability(locationDataProvider.geoDataObservable(false))
        assertDataAvailability(locationDataProvider.geoDataObservable(true))
    }

}
