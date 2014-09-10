package cgeo.geocaching.activity.logtrackable;

import static com.google.android.apps.common.testing.ui.espresso.Espresso.onData;
import static com.google.android.apps.common.testing.ui.espresso.Espresso.onView;
import static com.google.android.apps.common.testing.ui.espresso.Espresso.openActionBarOverflowOrOptionsMenu;
import static com.google.android.apps.common.testing.ui.espresso.action.ViewActions.click;
import static com.google.android.apps.common.testing.ui.espresso.assertion.ViewAssertions.doesNotExist;
import static com.google.android.apps.common.testing.ui.espresso.assertion.ViewAssertions.matches;
import static com.google.android.apps.common.testing.ui.espresso.matcher.ViewMatchers.isDisplayed;
import static com.google.android.apps.common.testing.ui.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.hasToString;
import static org.hamcrest.Matchers.startsWith;

import cgeo.geocaching.R;

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

    private void clickActionBarItem(int labelResourceId) {
        onData(hasToString(startsWith(getString(labelResourceId)))).perform(click());
    }

    private void openActionBar() {
        openActionBarOverflowOrOptionsMenu(getInstrumentation().getTargetContext());
    }

    private String getString(int resId) {
        return getActivity().getString(resId);
    }

}
