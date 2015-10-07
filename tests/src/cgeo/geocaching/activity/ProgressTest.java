package cgeo.geocaching.activity;

import static org.assertj.core.api.Assertions.assertThat;

import cgeo.geocaching.AboutActivity;

import android.annotation.TargetApi;
import android.test.ActivityInstrumentationTestCase2;

/**
 * This test uses the about activity to avoid side effects like network and GPS being triggered by the main activity.
 *
 */
@TargetApi(8)
public class ProgressTest extends ActivityInstrumentationTestCase2<AboutActivity> {
    public ProgressTest() {
        super(AboutActivity.class);
    }

    public void testProgressWrapper() {
        final Progress progress = new Progress();

        assertThat(progress.isShowing()).isFalse(); // nothing shown initially

        progress.show(getActivity(), "Title", "Message", true, null);
        assertThat(progress.isShowing()).isTrue();

        progress.setMessage("Test");
        assertThat(progress.isShowing()).isTrue();

        for (int i = 0; i < 2; i++) { // fault tolerant when dismissing to often
            progress.dismiss();
            assertThat(progress.isShowing()).isFalse();
        }
    }
}
