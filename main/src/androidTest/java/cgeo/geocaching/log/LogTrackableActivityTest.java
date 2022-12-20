package cgeo.geocaching.log;

import cgeo.geocaching.R;

import androidx.test.filters.Suppress;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.doesNotExist;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

// disabled because of issues with testing on Lollipop
@Suppress
public class LogTrackableActivityTest extends AbstractLogTrackableActivityTest {

    public void testInsertNameExists() throws Exception {
        openActionBar();
        clickActionBarItem(R.string.log_add);

        onView(withText(getString(R.string.init_signature_template_name))).check(matches(isDisplayed()));
    }

    public void testInsertNumberNotExists() throws Exception {
        openActionBar();
        clickActionBarItem(R.string.log_add);

        onView(withText(getString(R.string.init_signature_template_number))).check(doesNotExist());
    }
}
