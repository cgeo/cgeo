package cgeo.geocaching.connector.gc;

import cgeo.geocaching.test.mock.MockedCache;
import cgeo.geocaching.utils.BaseUtils;

import android.test.AndroidTestCase;
import android.text.Html;

public class GCConstantsTest extends AndroidTestCase {

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
        assertCacheCount(725, MockedCache.readCachePage("GC2CJPF")); // # of caches found by blafoo at the point of time creating the mocked data
    }

    private static void assertCacheCount(final int count, final String html) {
        try {
            assertEquals(count, Integer.parseInt(BaseUtils.getMatch(html, GCConstants.PATTERN_CACHES_FOUND, true, "0").replaceAll("[,.]", "")));
        } catch (NumberFormatException e) {
            fail();
        }
    }

    public static void testConstants() {
        String session = "userSession = new Groundspeak.Map.UserSession('aKWZ', userOptions:'XPTf', sessionToken:'123pNKwdktYGZL0xd-I7yqA6nm_JE1BDUtM4KcOkifin2TRCMutBd_PZE14Ohpffs2ZgkTnxTSnxYpBigK4hBA2', subscriberType: 3, enablePersonalization: true });";
        assertEquals("aKWZ", BaseUtils.getMatch(session, GCConstants.PATTERN_USERSESSION, ""));
        assertTrue(BaseUtils.getMatch(session, GCConstants.PATTERN_SESSIONTOKEN, "").startsWith("123pNK"));
    }

    public static void testTBWithSpecialChar() {
        String page = "<meta name=\"og:site_name\" content=\"Geocaching.com\" property=\"og:site_name\" /><meta name=\"og:type\" content=\"article\" property=\"og:type\" /><meta name=\"fb:app_id\" content=\"100167303362705\" property=\"fb:app_id\" /><meta name=\"og:url\" content=\"http://coord.info/TB4VPZD\" property=\"og:url\" /><meta name=\"og:description\" property=\"og:description\" /><meta name=\"og:image\" content=\"http://www.geocaching.com/images/facebook/wpttypes/24.png\" property=\"og:image\" /><meta name=\"og:title\" content=\"Schlauchen&amp;ravestorm\" property=\"og:title\" /></head>\n";
        assertEquals("Schlauchen&ravestorm", Html.fromHtml(BaseUtils.getMatch(page, GCConstants.PATTERN_TRACKABLE_NAME, "")).toString());
    }
}
