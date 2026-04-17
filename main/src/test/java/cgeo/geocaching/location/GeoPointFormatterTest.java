package cgeo.geocaching.location;

import org.junit.Test;
import static org.assertj.core.api.Java6Assertions.assertThat;

public class GeoPointFormatterTest {

    @Test
    public void testConfluence() {
        // From issue #2624: coordinate is wrong near to a confluence point
        final Geopoint point = new Geopoint(49.9999999999999, 5.0);
        final String format = GeopointFormatter.format(GeopointFormatter.Format.LAT_LON_DECDEGREE_COMMA, point);
        assertThat(format).isEqualTo("50.000000,5.000000");
        final String formatMinute = GeopointFormatter.format(GeopointFormatter.Format.LAT_LON_DECMINUTE_RAW, point);
        assertThat(formatMinute).isEqualTo("N 50° 00.000 E 005° 00.000");
        final String formatSecond = GeopointFormatter.format(GeopointFormatter.Format.LAT_LON_DECSECOND, point).replaceAll(",", ".");
        assertThat(formatSecond).isEqualTo("N 50° 00' 00.000\" · E 005° 00' 00.000\"");
    }

    @Test
    public void testFormat() {
        // taken from GC30R6G
        final Geopoint point = new Geopoint("N 51° 21.104 E 010° 15.369");
        final String format = GeopointFormatter.format(GeopointFormatter.Format.LAT_LON_DECDEGREE_COMMA, point);
        assertThat(format).isEqualTo("51.351733,10.256150");
        final String formatMinute = GeopointFormatter.format(GeopointFormatter.Format.LAT_LON_DECMINUTE_RAW, point);
        assertThat(formatMinute).isEqualTo("N 51° 21.104 E 010° 15.369");
        final String formatSecond = GeopointFormatter.format(GeopointFormatter.Format.LAT_LON_DECSECOND, point).replaceAll(",", ".");
        assertThat(formatSecond).isEqualTo("N 51° 21' 06.239\" · E 010° 15' 22.140\"");
    }

    @Test
    public void testFormatNeg() {
        // taken from GC30R6G
        final Geopoint point = new Geopoint("S 51° 21.104 W 010° 15.369");
        final String format = GeopointFormatter.format(GeopointFormatter.Format.LAT_LON_DECDEGREE_COMMA, point);
        assertThat(format).isEqualTo("-51.351733,-10.256150");
        final String formatMinute = GeopointFormatter.format(GeopointFormatter.Format.LAT_LON_DECMINUTE_RAW, point);
        assertThat(formatMinute).isEqualTo("S 51° 21.104 W 010° 15.369");
        final String formatSecond = GeopointFormatter.format(GeopointFormatter.Format.LAT_LON_DECSECOND, point).replaceAll(",", ".");
        assertThat(formatSecond).isEqualTo("S 51° 21' 06.239\" · W 010° 15' 22.140\"");
    }

    @Test
    public void testReformatForClipboardRemoveMiddleDotReplaceCommaWithPoint() {
        assertThat(GeopointFormatter.reformatForClipboard("N 10° 12,345 · W 5° 12,345")).isEqualTo("N 10° 12.345 W 5° 12.345");
    }

    @Test
    public void testReformatForClipboardNoChange() {
        assertThat(GeopointFormatter.reformatForClipboard("N 10° 12' 34\" W 5° 12' 34\"")).isEqualTo("N 10° 12' 34\" W 5° 12' 34\"");
    }

    @Test
    public void testReformatForClipboardReplaceCommaWithPoint() {
        assertThat(GeopointFormatter.reformatForClipboard("10,123456 -0,123456")).isEqualTo("10.123456 -0.123456");
    }

    @Test
    public void testExtendedFormatsRoundtrip() {
        final Geopoint point = new Geopoint("N 52° 22.366 E 004° 53.616");

        final String mgrs = GeopointFormatter.format(GeopointFormatter.Format.MGRS, point);
        final String olc = GeopointFormatter.format(GeopointFormatter.Format.OLC, point);
        final String swiss = GeopointFormatter.format(GeopointFormatter.Format.SWISS_GRID, point);
        final String rd = GeopointFormatter.format(GeopointFormatter.Format.RD, point);

        assertThat(mgrs).matches("\\d{1,2}[C-HJ-NP-X] [A-HJ-NP-Z][A-HJ-NP-V] \\d{5} \\d{5}");
        assertThat(olc).matches("[23456789CFGHJMPQRVWX]{8}\\+[23456789CFGHJMPQRVWX]{2}");
        assertThat(swiss).startsWith("LV95 E ");
        assertThat(rd).startsWith("RD X ");

        assertThat(point.distanceTo(GeopointParser.parse(mgrs))).isLessThan(2.0f);
        assertThat(point.distanceTo(GeopointParser.parse(olc))).isLessThan(20.0f);
        assertThat(point.distanceTo(GeopointParser.parse(swiss))).isLessThan(3.0f);
        assertThat(point.distanceTo(GeopointParser.parse(rd))).isLessThan(3.0f);
    }

}
