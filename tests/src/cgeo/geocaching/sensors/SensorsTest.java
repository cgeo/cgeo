package cgeo.geocaching.sensors;

import cgeo.geocaching.MainActivity;

import junit.framework.Assert;

import android.test.ActivityInstrumentationTestCase2;

public class SensorsTest extends ActivityInstrumentationTestCase2<MainActivity> {

    private MainActivity activity;

    public SensorsTest() {
        super(MainActivity.class);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        activity = getActivity();
    }

    public void testGetDirectionNow() {
        final float angle = DirectionProvider.getDirectionNow(activity, 1.0f);
        Assert.assertTrue(angle == 1.0f || angle == 91.0f || angle == 181.0f || angle == 271.0f);
    }

}
