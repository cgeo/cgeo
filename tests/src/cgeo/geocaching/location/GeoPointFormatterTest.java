package cgeo.geocaching.location;

import static org.assertj.core.api.Assertions.assertThat;

import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.location.GeopointFormatter;
import cgeo.geocaching.utils.Formatter;

import android.test.AndroidTestCase;

public class GeoPointFormatterTest extends AndroidTestCase {

    public static void testConfluence() {
        // From issue #2624: coordinate is wrong near to a confluence point
        final Geopoint point = new Geopoint(49.9999999999999, 5.0);
        final String format = GeopointFormatter.format(GeopointFormatter.Format.LAT_LON_DECDEGREE_COMMA, point);
        assertThat(format).isEqualTo("50.000000,5.000000");
        final String formatMinute = GeopointFormatter.format(GeopointFormatter.Format.LAT_LON_DECMINUTE_RAW, point);
        assertThat(formatMinute).isEqualTo("N 50° 00.000 E 005° 00.000");
        final String formatSecond = GeopointFormatter.format(GeopointFormatter.Format.LAT_LON_DECSECOND, point).replaceAll(",", ".");
        assertEquals(formatSecond, "N 50° 00' 00.000\"" + Formatter.SEPARATOR + "E 005° 00' 00.000\"", formatSecond);
    }

    public static void testFormat() {
        // taken from GC30R6G
        final Geopoint point = new Geopoint("N 51° 21.104 E 010° 15.369");
        final String format = GeopointFormatter.format(GeopointFormatter.Format.LAT_LON_DECDEGREE_COMMA, point);
        assertEquals(format, "51.351733,10.256150", format);
        final String formatMinute = GeopointFormatter.format(GeopointFormatter.Format.LAT_LON_DECMINUTE_RAW, point);
        assertEquals(formatMinute, "N 51° 21.104 E 010° 15.369", formatMinute);
        final String formatSecond = GeopointFormatter.format(GeopointFormatter.Format.LAT_LON_DECSECOND, point).replaceAll(",", ".");
        assertEquals(formatSecond, "N 51° 21' 06.240\"" + Formatter.SEPARATOR + "E 010° 15' 22.140\"", formatSecond);
    }

}
