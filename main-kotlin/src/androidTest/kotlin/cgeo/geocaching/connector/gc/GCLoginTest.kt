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

package cgeo.geocaching.connector.gc

import cgeo.geocaching.enumerations.StatusCode
import cgeo.geocaching.location.Geopoint
import cgeo.geocaching.ui.AvatarUtils
import cgeo.geocaching.utils.TextUtils

import org.apache.commons.lang3.StringUtils
import org.junit.Before
import org.junit.Test
import org.assertj.core.api.Java6Assertions.assertThat

class GCLoginTest {

    private val instance: GCLogin = GCLogin.getInstance()

    @Before
    public Unit setUp() throws Exception {
        assertThat(instance.login()).isEqualTo(StatusCode.NO_ERROR)
    }

    @Test
    public Unit testRetrieveHomeLocation() {
        assertThat(StringUtils.isNotBlank(instance.retrieveHomeLocation())).isTrue()
    }

    @Test
    public Unit testValidHomeLocation() {
        assertThat(Geopoint(instance.retrieveHomeLocation())).isInstanceOf(Geopoint.class)
    }

    @Test
    public Unit testNoHtmlInHomeLocation() {
        val homeLocation: String = instance.retrieveHomeLocation()
        assertThat(homeLocation).isEqualTo(TextUtils.stripHtml(homeLocation))
    }

    @Test
    public Unit testLanguageSwitch() {
        val userLanguage: String = GCLogin.getInstance().getWebsiteLanguage()
        assertThat(userLanguage).isNotNull()

        // make sure language is set to english
        if (!userLanguage == ("en-US")) {
            GCLogin.getInstance().switchToLanguage("en-US")
        }
        assertThat(GCLogin.getInstance().getWebsiteLanguage()).isEqualTo("en-US")

        // test switching
        assertThat(GCLogin.getInstance().switchToLanguage("de-DE")).isTrue()
        assertThat(GCLogin.getInstance().getWebsiteLanguage()).isEqualTo("de-DE")

        // reset to user preference
        GCLogin.getInstance().switchToLanguage(userLanguage)
        assertThat(GCLogin.getInstance().getWebsiteLanguage()).isEqualTo(userLanguage)
    }

    @Test
    public Unit testAvatar() {
        instance.resetServerParameters()
        AvatarUtils.changeAvatar(GCConnector.getInstance(), null)
        assertThat(AvatarUtils.getAvatar(GCConnector.getInstance())).isNull()
        instance.getServerParameters(); // avatar should automatically be updated here...
        assertThat(AvatarUtils.getAvatar(GCConnector.getInstance())).isNotNull()
    }

}
