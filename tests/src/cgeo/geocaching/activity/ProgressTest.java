package cgeo.geocaching.activity;

import cgeo.geocaching.cgeo;

import android.test.ActivityInstrumentationTestCase2;

public class ProgressTest extends ActivityInstrumentationTestCase2<cgeo> {
    public ProgressTest() {
        super("cgeo.geocaching", cgeo.class);
    }

    public void testProgressWrapper() {
        assertFalse(Progress.isShowing()); // nothing shown initially

        Progress.show(getActivity(), "Title", "Message", true, false);
        assertTrue(Progress.isShowing());

        Progress.setMessage("Test");
        assertTrue(Progress.isShowing());

        for (int i = 0; i < 2; i++) { // fault tolerant when dismissing to often
            Progress.dismiss();
            assertFalse(Progress.isShowing());
        }
    }
}
