package cgeo.geocaching.activity;

import cgeo.geocaching.AboutActivity;

import androidx.test.ext.junit.rules.ActivityScenarioRule;

import org.junit.Rule;
import org.junit.Test;
import static org.assertj.core.api.Java6Assertions.assertThat;

/**
 * This test uses the about activity to avoid side effects like network and GPS being triggered by the main activity.
 */
public class ProgressTest { 


    @Rule
    public ActivityScenarioRule<AboutActivity> activityRule =
            new ActivityScenarioRule<>(AboutActivity.class);

    @Test
    public void progressWrapper() {
        final Progress progress = new Progress();

        assertThat(progress.isShowing()).isFalse(); // nothing shown initially

        activityRule.getScenario().onActivity(activity -> {
            progress.show(activity, "Title", "Message", true, null);
            assertThat(progress.isShowing()).isTrue();

            progress.setMessage("Test");
            assertThat(progress.isShowing()).isTrue();

            for (int i = 0; i < 2; i++) { // fault tolerant when dismissing to often
                progress.dismiss();
                assertThat(progress.isShowing()).isFalse();
            }
        });


    }
}
