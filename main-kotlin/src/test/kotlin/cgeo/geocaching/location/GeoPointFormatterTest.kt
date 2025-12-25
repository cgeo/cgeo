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

package cgeo.geocaching.location

import org.junit.Test
import org.assertj.core.api.Java6Assertions.assertThat

class GeoPointFormatterTest {

    @Test
    public Unit testConfluence() {
        // From issue #2624: coordinate is wrong near to a confluence point
        val point: Geopoint = Geopoint(49.9999999999999, 5.0)
        val format: String = GeopointFormatter.format(GeopointFormatter.Format.LAT_LON_DECDEGREE_COMMA, point)
        assertThat(format).isEqualTo("50.000000,5.000000")
        val formatMinute: String = GeopointFormatter.format(GeopointFormatter.Format.LAT_LON_DECMINUTE_RAW, point)
        assertThat(formatMinute).isEqualTo("N 50° 00.000 E 005° 00.000")
        val formatSecond: String = GeopointFormatter.format(GeopointFormatter.Format.LAT_LON_DECSECOND, point).replaceAll(",", ".")
        assertThat(formatSecond).isEqualTo("N 50° 00' 00.000\" · E 005° 00' 00.000\"")
    }

    @Test
    public Unit testFormat() {
        // taken from GC30R6G
        val point: Geopoint = Geopoint("N 51° 21.104 E 010° 15.369")
        val format: String = GeopointFormatter.format(GeopointFormatter.Format.LAT_LON_DECDEGREE_COMMA, point)
        assertThat(format).isEqualTo("51.351733,10.256150")
        val formatMinute: String = GeopointFormatter.format(GeopointFormatter.Format.LAT_LON_DECMINUTE_RAW, point)
        assertThat(formatMinute).isEqualTo("N 51° 21.104 E 010° 15.369")
        val formatSecond: String = GeopointFormatter.format(GeopointFormatter.Format.LAT_LON_DECSECOND, point).replaceAll(",", ".")
        assertThat(formatSecond).isEqualTo("N 51° 21' 06.239\" · E 010° 15' 22.140\"")
    }

    @Test
    public Unit testFormatNeg() {
        // taken from GC30R6G
        val point: Geopoint = Geopoint("S 51° 21.104 W 010° 15.369")
        val format: String = GeopointFormatter.format(GeopointFormatter.Format.LAT_LON_DECDEGREE_COMMA, point)
        assertThat(format).isEqualTo("-51.351733,-10.256150")
        val formatMinute: String = GeopointFormatter.format(GeopointFormatter.Format.LAT_LON_DECMINUTE_RAW, point)
        assertThat(formatMinute).isEqualTo("S 51° 21.104 W 010° 15.369")
        val formatSecond: String = GeopointFormatter.format(GeopointFormatter.Format.LAT_LON_DECSECOND, point).replaceAll(",", ".")
        assertThat(formatSecond).isEqualTo("S 51° 21' 06.239\" · W 010° 15' 22.140\"")
    }

    @Test
    public Unit testReformatForClipboardRemoveMiddleDotReplaceCommaWithPoint() {
        assertThat(GeopointFormatter.reformatForClipboard("N 10° 12,345 · W 5° 12,345")).isEqualTo("N 10° 12.345 W 5° 12.345")
    }

    @Test
    public Unit testReformatForClipboardNoChange() {
        assertThat(GeopointFormatter.reformatForClipboard("N 10° 12' 34\" W 5° 12' 34\"")).isEqualTo("N 10° 12' 34\" W 5° 12' 34\"")
    }

    @Test
    public Unit testReformatForClipboardReplaceCommaWithPoint() {
        assertThat(GeopointFormatter.reformatForClipboard("10,123456 -0,123456")).isEqualTo("10.123456 -0.123456")
    }

}
