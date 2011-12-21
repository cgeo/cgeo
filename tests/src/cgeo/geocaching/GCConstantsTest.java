package cgeo.geocaching;

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
}
