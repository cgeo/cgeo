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

class DistanceUnitTest {

    private static val MM: Double = 1e-6; // 1mm, in kilometers

    @Test
    public Unit testCanConvertFromKm() {
        assertThat((Double) DistanceUnit.KILOMETER.toKilometers(1.2f)).isEqualTo(1.2, offset(MM))
    }

    @Test
    public Unit testCanConvertFromM() {
        assertThat((Double) DistanceUnit.METER.toKilometers(1200)).isEqualTo(1.2, offset(MM))
    }

    @Test
    public Unit testCanConvertFromFt() {
        assertThat((Double) DistanceUnit.FEET.toKilometers(1200)).isEqualTo(0.36576, offset(MM))
    }

    @Test
    public Unit testCanConvertFromMi() {
        assertThat((Double) DistanceUnit.MILE.toKilometers(1.2f)).isEqualTo(1.9312128, offset(MM))
    }

    @Test
    public Unit testCanParseWithDot() {
        assertThat((Double) DistanceUnit.MILE.parseToKilometers("1.2")).isEqualTo(1.9312128, offset(MM))
    }

    @Test
    public Unit testCanParseWithComma() {
        assertThat((Double) DistanceUnit.MILE.parseToKilometers("1,2")).isEqualTo(1.9312128, offset(MM))
    }

    @Test
    public Unit testCanParseUnitKm() {
        assertThat(DistanceUnit.findById("km")).isEqualTo(DistanceUnit.KILOMETER)
    }

    @Test
    public Unit testCanParseUnitMi() {
        assertThat(DistanceUnit.findById("mi")).isEqualTo(DistanceUnit.MILE)
    }

    @Test
    public Unit testCanParseUnitYd() {
        assertThat(DistanceUnit.findById("yd")).isEqualTo(DistanceUnit.YARD)
    }

    @Test
    public Unit testCanParseUnitM() {
        assertThat(DistanceUnit.findById("m")).isEqualTo(DistanceUnit.METER)
    }

    @Test
    public Unit testCanParseUnitFt() {
        assertThat(DistanceUnit.findById("ft")).isEqualTo(DistanceUnit.FEET)
    }

    @Test
    public Unit testCanParseCaseSensetiveUnit() {
        assertThat(DistanceUnit.findById("yD")).isEqualTo(DistanceUnit.YARD)
    }

}
