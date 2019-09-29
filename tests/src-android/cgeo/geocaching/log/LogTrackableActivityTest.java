package cgeo.geocaching.log;

import android.test.suitebuilder.annotation.Suppress;

import cgeo.geocaching.R;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.assertion.ViewAssertions.doesNotExist;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withText;

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
