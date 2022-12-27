package cgeo.geocaching.activity;

import cgeo.geocaching.AboutActivity;
import cgeo.geocaching.R;
import cgeo.geocaching.test.CgeoTestUtils;
import cgeo.geocaching.utils.LocalizationUtils;
import cgeo.geocaching.utils.Version;

import androidx.test.core.app.ApplicationProvider;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

import org.junit.Test;

@SuppressWarnings("PMD.JUnitTestsShouldIncludeAssert")
public class AboutActivityTest {

    @Test
    public void displayMain() {
        CgeoTestUtils.executeForActivity(AboutActivity.class,
                null,
                scenario -> onView(withId(R.id.about_version_string))
                        .check(matches(withText(Version.getVersionName(ApplicationProvider.getApplicationContext())))));
    }


    @Test
    public void displayChangeLog() {
        CgeoTestUtils.executeForActivity(AboutActivity.class,
                intent -> intent.putExtra(AboutActivity.EXTRA_ABOUT_STARTPAGE, AboutActivity.Page.CHANGELOG.id),
                scenario -> onView(withId(R.id.changelog_github)).check(matches(withText(LocalizationUtils.getString(R.string.changelog_github)))));
    }
}
