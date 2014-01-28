package cgeo.geocaching.connector.gc;

import cgeo.geocaching.test.mock.MockedCache;
import cgeo.geocaching.utils.TextUtils;

import android.test.AndroidTestCase;
import android.text.Html;

public class GCConstantsTest extends AndroidTestCase {

    // adapt the following after downloading new mock html files
    public static final String MOCK_LOGIN_NAME = "JoSaMaJa";
    public static final int MOCK_CACHES_FOUND = 484;

    public static void testLocation() {
        // GC37GFJ
        assertEquals("Bretagne, France", parseLocation("    <span id=\"ctl00_ContentBody_Location\">In Bretagne, France</span><br />"));
        // GCV2R9
        assertEquals("California, United States", parseLocation("<span id=\"ctl00_ContentBody_Location\">In <a href=\"/map/beta/default.aspx?lat=37.4354&lng=-122.07745&z=16\" title=\"View Map\">California, United States</a></span><br />"));
    }

    private static String parseLocation(final String html) {
        return TextUtils.getMatch(html, GCConstants.PATTERN_LOCATION, true, "");
    }

    public static void testCacheCount() {
        assertCacheCount(1510, "<strong style=\"display:block\"> 1.510 Caches Found</strong>");
        assertCacheCount(1510, "<strong style=\"display:block\"> 1,510 Caches Found</strong>");
        assertCacheCount(MOCK_CACHES_FOUND, MockedCache.readCachePage("GC2CJPF"));
    }

    private static void assertCacheCount(final int count, final String html) {
        try {
            assertEquals(count, Integer.parseInt(TextUtils.getMatch(html, GCConstants.PATTERN_CACHES_FOUND, true, "0").replaceAll("[,.]", "")));
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
        GCLogin.getInstance().logout();
        GCLogin.getInstance().setActualCachesFound(0);
        GCLogin.getInstance().login();
        assertTrue(GCLogin.getInstance().getActualCachesFound() > 0);
    }

    public static void testConstants() {
        final String session = "userSession = new Groundspeak.Map.UserSession('aKWZ', userOptions:'XPTf', sessionToken:'123pNKwdktYGZL0xd-I7yqA6nm_JE1BDUtM4KcOkifin2TRCMutBd_PZE14Ohpffs2ZgkTnxTSnxYpBigK4hBA2', subscriberType: 3, enablePersonalization: true });";
        assertEquals("aKWZ", TextUtils.getMatch(session, GCConstants.PATTERN_USERSESSION, ""));
        assertTrue(TextUtils.getMatch(session, GCConstants.PATTERN_SESSIONTOKEN, "").startsWith("123pNK"));
    }

    public static void testTBWithSpecialChar() {
        // Incidentally, the site incorrectly escapes the "&" into "&amp;"
        final String page = "<span id=\"ctl00_ContentBody_lbHeading\">Schlauchen&amp;ravestorm</span>";
        assertEquals("Schlauchen&ravestorm", Html.fromHtml(TextUtils.getMatch(page, GCConstants.PATTERN_TRACKABLE_NAME, "")).toString());
        // Test with the current incorrect form as well
        final String page2 = "<span id=\"ctl00_ContentBody_lbHeading\">Schlauchen&ravestorm</span>";
        assertEquals("Schlauchen&ravestorm", Html.fromHtml(TextUtils.getMatch(page2, GCConstants.PATTERN_TRACKABLE_NAME, "")).toString());
    }
}
