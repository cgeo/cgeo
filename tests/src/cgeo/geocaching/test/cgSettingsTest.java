package cgeo.geocaching.test;

import android.test.ActivityInstrumentationTestCase2;
import cgeo.geocaching.cgSettings;
import cgeo.geocaching.cgeo;

public class cgSettingsTest extends ActivityInstrumentationTestCase2<cgeo> {

    private cgeo activity;

    public cgSettingsTest() {
        super("cgeo.geocaching", cgeo.class);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        activity = getActivity();
    }

    /**
     * loads new empty settings, this should work fine without an exception (once there was an exception because of the empty map file string)
     */
    public void testSettingsException() {
        final cgSettings settings = new cgSettings(activity, activity.getSharedPreferences("not existing preferences", 0));

        // assert that we really created new settings
        assertNull(settings.getMapFile());
    }
}
