package cgeo.geocaching;

import androidx.test.ext.junit.rules.ActivityScenarioRule;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;

import org.junit.Rule;
import org.junit.Test;

@SuppressWarnings("PMD.JUnitTestsShouldIncludeAssert")
public class ExampleEspressoTest {

    @Rule
    public ActivityScenarioRule<ImageViewActivity> activityRule =
            new ActivityScenarioRule<>(ImageViewActivity.class);

    @Test
    public void simple() {
        onView(withId(R.id.image_open_file)).check(matches(isDisplayed()));
    }
}
