package cgeo.geocaching.sensors;

import cgeo.geocaching.utils.AngleUtils;

import java.util.concurrent.TimeUnit;

import io.reactivex.rxjava3.core.Observable;
import org.junit.Before;
import org.junit.Test;
import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.junit.Assert.fail;

public class LocationDataProviderTest {

    private LocationDataProvider locationDataProvider;

    @Before
    public void setUp() throws Exception {
        locationDataProvider = LocationDataProvider.getInstance();
        locationDataProvider.initialize();
    }

    @Test
    public void testGetDirectionNow() {
        final float angle = AngleUtils.getDirectionNow(1.0f);
        assertThat(angle == 1.0f || angle == 91.0f || angle == 181.0f || angle == 271.0f).isTrue();
    }

    private static <T> void assertDataAvailability(final Observable<T> observable) {
        try {
            observable.timeout(2, TimeUnit.SECONDS).blockingFirst();
        } catch (final Exception ignored) {
            fail("timeout while waiting for sensor data");
        }
    }

    @Test
    public void testDirectionObservable() {
        assertDataAvailability(locationDataProvider.directionDataObservable());
    }

    @Test
    public void testGeodataObservable() {
        assertDataAvailability(locationDataProvider.geoDataObservable(false));
        assertDataAvailability(locationDataProvider.geoDataObservable(true));
    }

}
