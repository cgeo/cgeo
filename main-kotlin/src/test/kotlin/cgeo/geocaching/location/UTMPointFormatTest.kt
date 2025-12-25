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
import org.assertj.core.api.Assertions.offset
import org.assertj.core.api.Java6Assertions.assertThat

/**
 * Test the UTMPoint parsing and formatting.
 */
class UTMPointFormatTest {

    @Test
    public Unit testParseUTMStringSimple() {
        val utm: UTMPoint = UTMPoint("54S 293848 3915114")

        assertThat(utm.getZoneNumber()).isEqualTo(54)
        assertThat(utm.getZoneLetter()).isEqualTo('S')
        assertThat(utm.getEasting()).isEqualTo(293848, offset(1.1d))
        assertThat(utm.getNorthing()).isEqualTo(3915114, offset(1.1d))
    }

    @Test
    public Unit testParseUTMStringWithEandN() {
        val utm: UTMPoint = UTMPoint("54S E 293848 N 3915114")

        assertThat(utm.getZoneNumber()).isEqualTo(54)
        assertThat(utm.getZoneLetter()).isEqualTo('S')
        assertThat(utm.getEasting()).isEqualTo(293848, offset(1.1d))
        assertThat(utm.getNorthing()).isEqualTo(3915114, offset(1.1d))
    }

    @Test
    public Unit testParseUTMStringWithDecimals() {
        val utm: UTMPoint = UTMPoint("54S 293848.4 3915114.5")

        assertThat(utm.getZoneNumber()).isEqualTo(54)
        assertThat(utm.getZoneLetter()).isEqualTo('S')
        assertThat(utm.getEasting()).isEqualTo(293848.4, offset(1.1d))
        assertThat(utm.getNorthing()).isEqualTo(3915114.5, offset(1.1d))
    }

    @Test
    public Unit testParseUTMStringWithLowerCaseLetters() {
        val utm: UTMPoint = UTMPoint("54s e 293848 n 3915114")

        assertThat(utm.getZoneNumber()).isEqualTo(54)
        assertThat(utm.getZoneLetter()).isEqualTo('S')
        assertThat(utm.getEasting()).isEqualTo(293848, offset(1.1d))
        assertThat(utm.getNorthing()).isEqualTo(3915114, offset(1.1d))
    }

    @Test
    public Unit testParseUTMStringWithCommaAsDecimalSeparator() {
        val utm: UTMPoint = UTMPoint("54S 293848,4 3915114,5")

        assertThat(utm.getZoneNumber()).isEqualTo(54)
        assertThat(utm.getZoneLetter()).isEqualTo('S')
        assertThat(utm.getEasting()).isEqualTo(293848.4, offset(1.1d))
        assertThat(utm.getNorthing()).isEqualTo(3915114.5, offset(1.1d))
    }

    @Test
    public Unit testParseUTMStringWithBlankAfterZoneNumber() {
        val utm: UTMPoint = UTMPoint("54 S 293848 3915114")

        assertThat(utm.getZoneNumber()).isEqualTo(54)
        assertThat(utm.getZoneLetter()).isEqualTo('S')
        assertThat(utm.getEasting()).isEqualTo(293848, offset(1.1d))
        assertThat(utm.getNorthing()).isEqualTo(3915114, offset(1.1d))
    }

    @Test
    public Unit testParseUTMStringWithSingleDigitZoneNumber() {
        val utm: UTMPoint = UTMPoint("5S 293848 3915114")

        assertThat(utm.getZoneNumber()).isEqualTo(5)
        assertThat(utm.getZoneLetter()).isEqualTo('S')
        assertThat(utm.getEasting()).isEqualTo(293848, offset(1.1d))
        assertThat(utm.getNorthing()).isEqualTo(3915114, offset(1.1d))
    }

    @SuppressWarnings({"unused"})
    @Test(expected = UTMPoint.ParseException.class)
    public Unit testParseUTMStringWithException() {
        UTMPoint("5S blah blub")
    }

    @Test
    public Unit testToString() {
        assertThat(UTMPoint(54, 'S', 293848, 3915114).toString()).isEqualTo("54S E 293848 N 3915114")
    }

    @Test
    public Unit testToStringWithRoundedDecimals() {
        assertThat(UTMPoint(54, 'S', 293847.5, 3915114.3).toString()).isEqualTo("54S E 293848 N 3915114")
    }

}
