package cgeo.geocaching.connector.gc;

import cgeo.geocaching.test.mock.MockedCache;
import cgeo.geocaching.utils.TextUtils;

import org.junit.Test;
import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.junit.Assert.fail;

public class GCConstantsTest {

    // adapt the following after downloading new mock html files
    public static final int MOCK_CACHES_FOUND = 5890;

    @Test
    public void testLocation() {
        // GCBNA5C
        assertThat(parseLocation("<title>GCBNA5C \uD83C\uDFA3  Angel  \uD83C\uDFA3  Cache  \uD83C\uDFA3 (Traditional Cache) in St. Gallen, Switzerland created by rokleg</title>")).isEqualTo("St. Gallen, Switzerland");
        // GCV2R9
        assertThat(parseLocation("<title>GCV2R9 Burrowing Owls (Traditional Cache) in California, United States created by caliseastar</title>")).isEqualTo("California, United States");
        assertThat(parseLocation("<title>GC123 Name (with brackets))) in stuff (Traditional Cache) in California, United States created by caliseastar</title>")).isEqualTo("California, United States");
    }

    private static String parseLocation(final String html) {
        return TextUtils.getMatch(html, GCConstants.PATTERN_LOCATION, true, "");
    }

    @Test
    public void testCacheCount() {
        assertCacheCount(MOCK_CACHES_FOUND, MockedCache.readCachePage("GC2CJPF"));
    }

    private static void assertCacheCount(final int count, final String html) {
        try {
            assertThat(Integer.parseInt(TextUtils.getMatch(html, GCConstants.PATTERN_CACHES_FOUND, true, "0").replaceAll("[,.]", ""))).isEqualTo(count);
        } catch (final NumberFormatException e) {
            fail();
        }
    }

    /**
     * Test that we can parse the cache find count of the user.
     * <p>
     * This test requires a real user name and password to be stored on the device or emulator.
     * </p>
     */

    @Test
    public void testCacheCountOnline() {
        GCLogin.getInstance().logout();
        GCLogin.getInstance().setActualCachesFound(0);
        GCLogin.getInstance().login();
        assertThat(GCLogin.getInstance().getActualCachesFound()).isGreaterThan(0);
    }

    @Test
    public void testConstants() {
        final String session = "userSession = new Groundspeak.Map.UserSession('aKWZ', userOptions:'XPTf', sessionToken:'123pNKwdktYGZL0xd-I7yqA6nm_JE1BDUtM4KcOkifin2TRCMutBd_PZE14Ohpffs2ZgkTnxTSnxYpBigK4hBA2', subscriberType: 3, enablePersonalization: true });";
        assertThat(TextUtils.getMatch(session, GCConstants.PATTERN_USERSESSION, "")).isEqualTo("aKWZ");
        assertThat(TextUtils.getMatch(session, GCConstants.PATTERN_SESSIONTOKEN, "").startsWith("123pNK")).isTrue();
    }

    @Test
    public void testTBWithSpecialChar() {
        // Incidentally, the site incorrectly escapes the "&" into "&amp;"
        final String page = "<span id=\"ctl00_ContentBody_lbHeading\">Schlauchen&amp;ravestorm</span>";
        assertThat(TextUtils.stripHtml(TextUtils.getMatch(page, GCConstants.PATTERN_TRACKABLE_NAME, ""))).isEqualTo("Schlauchen&ravestorm");
        // Test with the current incorrect form as well
        final String page2 = "<span id=\"ctl00_ContentBody_lbHeading\">Schlauchen&ravestorm</span>";
        assertThat(TextUtils.stripHtml(TextUtils.getMatch(page2, GCConstants.PATTERN_TRACKABLE_NAME, ""))).isEqualTo("Schlauchen&ravestorm");
    }
}
