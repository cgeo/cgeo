package cgeo.geocaching;

import cgeo.geocaching.test.mock.MockedCache;
import cgeo.geocaching.utils.BaseUtils;

import android.test.AndroidTestCase;

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
        assertCacheCount(149, "<strong><img src=\"/images/icons/icon_smile.png\" title=\"Caches Found\" /> 149</strong>");
        assertCacheCount(513, MockedCache.readCachePage("GC2CJPF"));
        assertCacheCount(1510, "<strong><img src=\"/images/icons/icon_smile.png\" title=\"Caches Found\" /> 1,510&nbsp;&middot;&nbsp;<img src=\"/images/challenges/types/sm/challenge.png\" title=\"Challenges Completed\" /> 2</strong>");
        assertCacheCount(67, "<strong><img title=\"Caches Found\" src=\"/images/icons/icon_smile.png\"/> 67</strong>");
        assertCacheCount(1067, "<strong><img title=\"Caches Found\" src=\"/images/icons/icon_smile.png\"/> 1,067</strong>");
    }

    private static void assertCacheCount(final int count, final String html) {
        assertEquals(count, Integer.parseInt(BaseUtils.getMatch(html, GCConstants.PATTERN_CACHES_FOUND, true, "0").replaceAll(",", "")));
    }
}
