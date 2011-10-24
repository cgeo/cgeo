package cgeo.geocaching.activity;

import cgeo.geocaching.cgeo;

import android.test.ActivityInstrumentationTestCase2;

public class ProgressTest extends ActivityInstrumentationTestCase2<cgeo> {
    public ProgressTest() {
        super("cgeo.geocaching", cgeo.class);
    }

    public void testProgressWrapper() {
        final Progress progress = new Progress();

        assertFalse(progress.isShowing()); // nothing shown initially

        progress.show(getActivity(), "Title", "Message", true, null);
        assertTrue(progress.isShowing());

        progress.setMessage("Test");
        assertTrue(progress.isShowing());

        for (int i = 0; i < 2; i++) { // fault tolerant when dismissing to often
            progress.dismiss();
            assertFalse(progress.isShowing());
        }
    }
}
