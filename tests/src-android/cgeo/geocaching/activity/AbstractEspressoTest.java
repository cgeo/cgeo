package cgeo.geocaching.activity;

import android.app.Activity;
import android.support.annotation.StringRes;
import android.test.ActivityInstrumentationTestCase2;

import static android.support.test.espresso.Espresso.onData;
import static android.support.test.espresso.Espresso.openActionBarOverflowOrOptionsMenu;
import static android.support.test.espresso.action.ViewActions.click;
import static org.hamcrest.Matchers.hasToString;
import static org.hamcrest.Matchers.startsWith;

public abstract class AbstractEspressoTest<T extends Activity> extends ActivityInstrumentationTestCase2<T> {

    public AbstractEspressoTest(final Class<T> activityClass) {
        super(activityClass);
    }

    protected final void clickActionBarItem(final int labelResourceId) {
        onData(hasToString(startsWith(getString(labelResourceId)))).perform(click());
    }

    protected final void openActionBar() {
        openActionBarOverflowOrOptionsMenu(getInstrumentation().getTargetContext());
    }

    protected final String getString(@StringRes final int resId) {
        return getActivity().getString(resId);
    }

}
