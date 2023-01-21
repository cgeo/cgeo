package cgeo.geocaching;

import androidx.test.ext.junit.rules.ActivityScenarioRule;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;

import org.junit.Rule;
import org.junit.Test;
import static org.assertj.core.api.Java6Assertions.assertThat;

public class ExampleEspressoTest {

    @Rule
    public ActivityScenarioRule<ImageViewActivity> activityRule =
            new ActivityScenarioRule<>(ImageViewActivity.class);

    @Test
    public void simple() {
        //activityRule.getScenario().recreate();
        assertThat(activityRule.getScenario().getState().toString()).isEqualTo("RESUMED");
        //onView(withText("Hello world!")).check(matches(isDisplayed()));
        onView(withId(R.id.image_open_file)).check(matches(isDisplayed()));
    }
}
