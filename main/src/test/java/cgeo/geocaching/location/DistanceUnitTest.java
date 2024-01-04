package cgeo.geocaching.location;

import org.junit.Test;
import static org.assertj.core.api.Assertions.offset;
import static org.assertj.core.api.Java6Assertions.assertThat;

public class DistanceUnitTest {

    private static final double MM = 1e-6; // 1mm, in kilometers

    @Test
    public void testCanConvertFromKm() {
        assertThat((double) DistanceUnit.KILOMETER.toKilometers(1.2f)).isEqualTo(1.2, offset(MM));
    }

    @Test
    public void testCanConvertFromM() {
        assertThat((double) DistanceUnit.METER.toKilometers(1200)).isEqualTo(1.2, offset(MM));
    }

    @Test
    public void testCanConvertFromFt() {
        assertThat((double) DistanceUnit.FEET.toKilometers(1200)).isEqualTo(0.36576, offset(MM));
    }

    @Test
    public void testCanConvertFromMi() {
        assertThat((double) DistanceUnit.MILE.toKilometers(1.2f)).isEqualTo(1.9312128, offset(MM));
    }

    @Test
    public void testCanParseWithDot() {
        assertThat((double) DistanceUnit.MILE.parseToKilometers("1.2")).isEqualTo(1.9312128, offset(MM));
    }

    @Test
    public void testCanParseWithComma() {
        assertThat((double) DistanceUnit.MILE.parseToKilometers("1,2")).isEqualTo(1.9312128, offset(MM));
    }

    @Test
    public void testCanParseUnitKm() {
        assertThat(DistanceUnit.findById("km")).isEqualTo(DistanceUnit.KILOMETER);
    }

    @Test
    public void testCanParseUnitMi() {
        assertThat(DistanceUnit.findById("mi")).isEqualTo(DistanceUnit.MILE);
    }

    @Test
    public void testCanParseUnitYd() {
        assertThat(DistanceUnit.findById("yd")).isEqualTo(DistanceUnit.YARD);
    }

    @Test
    public void testCanParseUnitM() {
        assertThat(DistanceUnit.findById("m")).isEqualTo(DistanceUnit.METER);
    }

    @Test
    public void testCanParseUnitFt() {
        assertThat(DistanceUnit.findById("ft")).isEqualTo(DistanceUnit.FEET);
    }

    @Test
    public void testCanParseCaseSensetiveUnit() {
        assertThat(DistanceUnit.findById("yD")).isEqualTo(DistanceUnit.YARD);
    }

}
