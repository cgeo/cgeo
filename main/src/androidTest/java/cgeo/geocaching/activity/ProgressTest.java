package cgeo.geocaching.activity;

import cgeo.geocaching.AboutActivity;

import android.annotation.TargetApi;
import android.test.ActivityInstrumentationTestCase2;

import androidx.test.filters.Suppress;

import static org.assertj.core.api.Java6Assertions.assertThat;

/**
 * This test uses the about activity to avoid side effects like network and GPS being triggered by the main activity.
 */
@TargetApi(8)
public class ProgressTest extends ActivityInstrumentationTestCase2<AboutActivity> {
    public ProgressTest() {
        super(AboutActivity.class);
    }

    // very fishy test. Produces regular problems in CI and also locally when executed solely:
    // 'java.lang.RuntimeException: Can't create handler inside thread Thread[Instr: androidx.test.runner.AndroidJUnitRunner,5,main] that has not called Looper.prepare()'
    // see Issue #8764
    @Suppress
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
