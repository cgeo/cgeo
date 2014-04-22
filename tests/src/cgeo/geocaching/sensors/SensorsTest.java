package cgeo.geocaching.sensors;

import static org.assertj.core.api.Assertions.assertThat;

import cgeo.geocaching.MainActivity;

import android.test.ActivityInstrumentationTestCase2;

public class SensorsTest extends ActivityInstrumentationTestCase2<MainActivity> {

    public SensorsTest() {
        super(MainActivity.class);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
    }

    public static void testGetDirectionNow() {
        final float angle = DirectionProvider.getDirectionNow(1.0f);
        assertThat(angle == 1.0f || angle == 91.0f || angle == 181.0f || angle == 271.0f).isTrue();
    }

}
