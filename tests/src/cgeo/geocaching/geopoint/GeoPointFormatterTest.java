package cgeo.geocaching.geopoint;

import cgeo.geocaching.ui.Formatter;

import android.test.AndroidTestCase;

public class GeoPointFormatterTest extends AndroidTestCase {

    public static void testFormat() {
        // taken from GC30R6G
        Geopoint point = new Geopoint("N 51° 21.104 E 010° 15.369");
        final String format = GeopointFormatter.format(GeopointFormatter.Format.LAT_LON_DECDEGREE_COMMA, point);
        assertEquals(format, "51.351733,10.256150", format);
        final String formatMinute = GeopointFormatter.format(GeopointFormatter.Format.LAT_LON_DECMINUTE_RAW, point);
        assertEquals(formatMinute, "N 51° 21.104 E 010° 15.369", formatMinute);
        final String formatSecond = GeopointFormatter.format(GeopointFormatter.Format.LAT_LON_DECSECOND, point).replaceAll(",", ".");
        assertEquals(formatSecond, "N 51° 21' 06.240\"" + Formatter.SEPARATOR + "E 010° 15' 22.140\"", formatSecond);
    }

}
