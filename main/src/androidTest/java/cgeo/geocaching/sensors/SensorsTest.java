package cgeo.geocaching.sensors;

import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.utils.AngleUtils;

import java.util.concurrent.TimeUnit;

import io.reactivex.rxjava3.core.Observable;
import org.junit.Before;
import org.junit.Test;
import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.junit.Assert.fail;

public class SensorsTest {

    private Sensors sensors;

    @Before
    public void setUp() throws Exception {
        sensors = Sensors.getInstance();
        sensors.setupGeoDataObservables(Settings.useGooglePlayServices(), Settings.useLowPowerMode());
        sensors.setupDirectionObservable();
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
        assertDataAvailability(sensors.directionDataObservable());
    }

    @Test
    public void testGeodataObservable() {
        assertDataAvailability(sensors.geoDataObservable(false));
        assertDataAvailability(sensors.geoDataObservable(true));
    }

}
