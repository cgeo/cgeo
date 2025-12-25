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

package cgeo.geocaching

import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId

import org.junit.Rule
import org.junit.Test
import org.assertj.core.api.Java6Assertions.assertThat

class ExampleEspressoTest {

    @Rule
    var activityRule: ActivityScenarioRule<ImageViewActivity> =
            ActivityScenarioRule<>(ImageViewActivity.class)

    @Test
    public Unit simple() {
        //activityRule.getScenario().recreate()
        assertThat(activityRule.getScenario().getState().toString()).isEqualTo("RESUMED")
        //onView(withText("Hello world!")).check(matches(isDisplayed()))
        onView(withId(R.id.image_open_file)).check(matches(isDisplayed()))
    }
}
