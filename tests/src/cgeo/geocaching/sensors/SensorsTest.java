package cgeo.geocaching.sensors;

import static org.assertj.core.api.Assertions.assertThat;

import cgeo.geocaching.MainActivity;
import cgeo.geocaching.utils.AngleUtils;

import rx.Observable;

import android.test.ActivityInstrumentationTestCase2;

import java.util.concurrent.TimeUnit;

public class SensorsTest extends ActivityInstrumentationTestCase2<MainActivity> {

    private Sensors sensors;

    public SensorsTest() {
        super(MainActivity.class);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        sensors = Sensors.getInstance();
    }

    public static void testGetDirectionNow() {
        final float angle = AngleUtils.getDirectionNow(1.0f);
        assertThat(angle == 1.0f || angle == 91.0f || angle == 181.0f || angle == 271.0f).isTrue();
    }

    private static <T> void testDataAvailability(final Observable<T> observable) {
        try {
            observable.timeout(200, TimeUnit.MILLISECONDS).first().toBlocking().single();
        } catch (final Exception ignored) {
            fail("timeout while waiting for sensor data");
        }
    }

    public void testDirectionObservable() {
        testDataAvailability(sensors.directionObservable());
    }

    public void testGeodataObservable() {
        testDataAvailability(sensors.geoDataObservable(false));
        testDataAvailability(sensors.geoDataObservable(true));
    }

}
