package cgeo.geocaching.connector.gc;

import cgeo.geocaching.test.mock.MockedCache;
import cgeo.geocaching.utils.BaseUtils;

import android.test.AndroidTestCase;
import android.text.Html;

public class GCConstantsTest extends AndroidTestCase {

    // adapt the following after downloading new mock html files
    public static final String MOCK_LOGIN_NAME = "JoSaMaJa";
    public static final int MOCK_CACHES_FOUND = 426;

    public static void testLocation() {
        // GC37GFJ
        assertEquals("Bretagne, France", parseLocation("    <span id=\"ctl00_ContentBody_Location\">In Bretagne, France</span><br />"));
        // GCV2R9
        assertEquals("California, United States", parseLocation("<span id=\"ctl00_ContentBody_Location\">In <a href=\"/map/beta/default.aspx?lat=37.4354&lng=-122.07745&z=16\" title=\"View Map\">California, United States</a></span><br />"));
    }

    private static String parseLocation(final String html) {
        return BaseUtils.getMatch(html, GCConstants.PATTERN_LOCATION, true, "");
    }

    public static void testCacheCount() {
        assertCacheCount(1510, "<strong style=\"display:block\"> 1.510 Caches Found</strong>");
        assertCacheCount(1510, "<strong style=\"display:block\"> 1,510 Caches Found</strong>");
        assertCacheCount(MOCK_CACHES_FOUND, MockedCache.readCachePage("GC2CJPF"));
    }

    private static void assertCacheCount(final int count, final String html) {
        try {
            assertEquals(count, Integer.parseInt(BaseUtils.getMatch(html, GCConstants.PATTERN_CACHES_FOUND, true, "0").replaceAll("[,.]", "")));
        } catch (NumberFormatException e) {
            fail();
        }
    }

    /**
     * Test that we can parse the cache find count of the user.
     * <p>
     * This test requires a real user name and password to be stored on the device or emulator.
     * </p>
     */

    public static void testCacheCountOnline() {
        Login.logout();
        Login.setActualCachesFound(0);
        Login.login();
        assertTrue(Login.getActualCachesFound() > 0);
    }

    public static void testConstants() {
        final String session = "userSession = new Groundspeak.Map.UserSession('aKWZ', userOptions:'XPTf', sessionToken:'123pNKwdktYGZL0xd-I7yqA6nm_JE1BDUtM4KcOkifin2TRCMutBd_PZE14Ohpffs2ZgkTnxTSnxYpBigK4hBA2', subscriberType: 3, enablePersonalization: true });";
        assertEquals("aKWZ", BaseUtils.getMatch(session, GCConstants.PATTERN_USERSESSION, ""));
        assertTrue(BaseUtils.getMatch(session, GCConstants.PATTERN_SESSIONTOKEN, "").startsWith("123pNK"));
    }

    public static void testTBWithSpecialChar() {
        final String page = "<meta name=\"og:site_name\" content=\"Geocaching.com\" property=\"og:site_name\" /><meta name=\"og:type\" content=\"article\" property=\"og:type\" /><meta name=\"fb:app_id\" content=\"100167303362705\" property=\"fb:app_id\" /><meta name=\"og:url\" content=\"http://coord.info/TB4VPZD\" property=\"og:url\" /><meta name=\"og:description\" property=\"og:description\" /><meta name=\"og:image\" content=\"http://www.geocaching.com/images/facebook/wpttypes/24.png\" property=\"og:image\" /><meta name=\"og:title\" content=\"Schlauchen&amp;ravestorm\" property=\"og:title\" /></head>\n";
        assertEquals("Schlauchen&ravestorm", Html.fromHtml(BaseUtils.getMatch(page, GCConstants.PATTERN_TRACKABLE_NAME, "")).toString());
    }
}
