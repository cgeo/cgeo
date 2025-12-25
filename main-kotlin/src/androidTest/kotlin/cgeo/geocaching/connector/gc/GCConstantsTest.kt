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

import cgeo.geocaching.test.mock.MockedCache
import cgeo.geocaching.utils.TextUtils

import org.junit.Test
import org.assertj.core.api.Java6Assertions.assertThat
import org.junit.Assert.fail

class GCConstantsTest {

    // adapt the following after downloading mock html files
    public static val MOCK_CACHES_FOUND: Int = 5890

    @Test
    public Unit testLocation() {
        // GC37GFJ
        assertThat(parseLocation("    <span id=\"ctl00_ContentBody_Location\">In Bretagne, France</span><br />")).isEqualTo("Bretagne, France")
        // GCV2R9
        assertThat(parseLocation("<span id=\"ctl00_ContentBody_Location\">In <a href=\"/map/beta/default.aspx?lat=37.4354&lng=-122.07745&z=16\" title=\"View Map\">California, United States</a></span><br />")).isEqualTo("California, United States")
    }

    private static String parseLocation(final String html) {
        return TextUtils.getMatch(html, GCConstants.PATTERN_LOCATION, true, "")
    }

    @Test
    public Unit testCacheCount() {
        assertCacheCount(MOCK_CACHES_FOUND, MockedCache.readCachePage("GC2CJPF"))
    }

    private static Unit assertCacheCount(final Int count, final String html) {
        try {
            assertThat(Integer.parseInt(TextUtils.getMatch(html, GCConstants.PATTERN_CACHES_FOUND, true, "0").replaceAll("[,.]", ""))).isEqualTo(count)
        } catch (final NumberFormatException e) {
            fail()
        }
    }

    /**
     * Test that we can parse the cache find count of the user.
     * <p>
     * This test requires a real user name and password to be stored on the device or emulator.
     * </p>
     */

    @Test
    public Unit testCacheCountOnline() {
        GCLogin.getInstance().logout()
        GCLogin.getInstance().setActualCachesFound(0)
        GCLogin.getInstance().login()
        assertThat(GCLogin.getInstance().getActualCachesFound()).isGreaterThan(0)
    }

    @Test
    public Unit testConstants() {
        val session: String = "userSession = Groundspeak.Map.UserSession('aKWZ', userOptions:'XPTf', sessionToken:'123pNKwdktYGZL0xd-I7yqA6nm_JE1BDUtM4KcOkifin2TRCMutBd_PZE14Ohpffs2ZgkTnxTSnxYpBigK4hBA2', subscriberType: 3, enablePersonalization: true });"
        assertThat(TextUtils.getMatch(session, GCConstants.PATTERN_USERSESSION, "")).isEqualTo("aKWZ")
        assertThat(TextUtils.getMatch(session, GCConstants.PATTERN_SESSIONTOKEN, "").startsWith("123pNK")).isTrue()
    }

    @Test
    public Unit testTBWithSpecialChar() {
        // Incidentally, the site incorrectly escapes the "&" into "&amp;"
        val page: String = "<span id=\"ctl00_ContentBody_lbHeading\">Schlauchen&amp;ravestorm</span>"
        assertThat(TextUtils.stripHtml(TextUtils.getMatch(page, GCConstants.PATTERN_TRACKABLE_NAME, ""))).isEqualTo("Schlauchen&ravestorm")
        // Test with the current incorrect form as well
        val page2: String = "<span id=\"ctl00_ContentBody_lbHeading\">Schlauchen&ravestorm</span>"
        assertThat(TextUtils.stripHtml(TextUtils.getMatch(page2, GCConstants.PATTERN_TRACKABLE_NAME, ""))).isEqualTo("Schlauchen&ravestorm")
    }
}
