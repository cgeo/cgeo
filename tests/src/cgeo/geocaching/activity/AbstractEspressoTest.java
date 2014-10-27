package cgeo.geocaching.activity;

import static com.google.android.apps.common.testing.ui.espresso.Espresso.onData;
import static com.google.android.apps.common.testing.ui.espresso.Espresso.openActionBarOverflowOrOptionsMenu;
import static com.google.android.apps.common.testing.ui.espresso.action.ViewActions.click;
import static org.hamcrest.Matchers.hasToString;
import static org.hamcrest.Matchers.startsWith;

import android.app.Activity;
import android.test.ActivityInstrumentationTestCase2;

public abstract class AbstractEspressoTest<T extends Activity> extends ActivityInstrumentationTestCase2<T> {

    public AbstractEspressoTest(Class<T> activityClass) {
        super(activityClass);
    }

    protected final void clickActionBarItem(int labelResourceId) {
        onData(hasToString(startsWith(getString(labelResourceId)))).perform(click());
    }

    protected final void openActionBar() {
        openActionBarOverflowOrOptionsMenu(getInstrumentation().getTargetContext());
    }

    protected final String getString(int resId) {
        return getActivity().getString(resId);
    }

}
