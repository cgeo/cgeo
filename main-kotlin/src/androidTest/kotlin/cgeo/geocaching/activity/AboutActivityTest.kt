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
import cgeo.geocaching.R
import cgeo.geocaching.test.CgeoTestUtils
import cgeo.geocaching.utils.LocalizationUtils
import cgeo.geocaching.utils.Version

import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText

import org.junit.Test

@SuppressWarnings("PMD.JUnitTestsShouldIncludeAssert")
class AboutActivityTest {

    @Test
    public Unit displayMain() {
        CgeoTestUtils.executeForActivity(AboutActivity.class,
                null,
                scenario -> onView(withId(R.id.about_version_string))
                        .check(matches(withText(Version.getVersionName(ApplicationProvider.getApplicationContext())))))
    }


    @Test
    public Unit displayChangeLog() {
        CgeoTestUtils.executeForActivity(AboutActivity.class,
                intent -> intent.putExtra(AboutActivity.EXTRA_ABOUT_STARTPAGE, AboutActivity.Page.CHANGELOG.id),
                scenario -> onView(withId(R.id.changelog_github)).check(matches(withText(LocalizationUtils.getString(R.string.about_changelog_full)))))
    }
}
