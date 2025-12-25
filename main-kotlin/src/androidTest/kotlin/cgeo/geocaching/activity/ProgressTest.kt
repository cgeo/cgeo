// Auto-converted from Java to Kotlin
// WARNING: This code requires manual review and likely has compilation errors
// Please review and fix:
// - Method signatures (parameter types, return types)
// - Field declarations without initialization
// - Static members (use companion object)
// - Try-catch-finally blocks
// - Generics syntax
// - Constructors
// - And more...

package cgeo.geocaching.activity

import cgeo.geocaching.AboutActivity

import androidx.test.ext.junit.rules.ActivityScenarioRule

import org.junit.Rule
import org.junit.Test
import org.assertj.core.api.Java6Assertions.assertThat

/**
 * This test uses the about activity to avoid side effects like network and GPS being triggered by the main activity.
 */
class ProgressTest { 


    @Rule
    var activityRule: ActivityScenarioRule<AboutActivity> =
            ActivityScenarioRule<>(AboutActivity.class)

    @Test
    public Unit progressWrapper() {
        val progress: Progress = Progress()

        assertThat(progress.isShowing()).isFalse(); // nothing shown initially

        activityRule.getScenario().onActivity(activity -> {
            progress.show(activity, "Title", "Message", true, null)
            assertThat(progress.isShowing()).isTrue()

            progress.setMessage("Test")
            assertThat(progress.isShowing()).isTrue()

            for (Int i = 0; i < 2; i++) { // fault tolerant when dismissing to often
                progress.dismiss()
                assertThat(progress.isShowing()).isFalse()
            }
        })


    }
}
