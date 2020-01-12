package cgeo.geocaching.location;

import org.junit.Test;
import static org.assertj.core.api.Assertions.fail;
import static org.assertj.core.api.Assertions.offset;
import static org.assertj.core.api.Java6Assertions.assertThat;

public class DistanceParserTest {

    private static final double MM = 1e-6; // 1mm, in kilometers

    @Test
    public void testCanConvertFromKm() {
        assertThat((double) DistanceParser.convertDistance(1.2f, DistanceParser.DistanceUnit.KM)).isEqualTo(1.2, offset(MM));
    }

    @Test
    public void testCanConvertFromM() {
        assertThat((double) DistanceParser.convertDistance(1200, DistanceParser.DistanceUnit.M)).isEqualTo(1.2, offset(MM));
    }

    @Test
    public void testCanConvertFromFt() {
        assertThat((double) DistanceParser.convertDistance(1200, DistanceParser.DistanceUnit.FT)).isEqualTo(0.36576, offset(MM));
    }

    @Test
    public void testCanConvertFromMi() {
        assertThat((double) DistanceParser.convertDistance(1.2f, DistanceParser.DistanceUnit.MI)).isEqualTo(1.9312128, offset(MM));
    }

    @Test
    public void testCanParseWithDot() {
        assertThat((double) DistanceParser.parseDistance("1.2", DistanceParser.DistanceUnit.MI)).isEqualTo(1.9312128, offset(MM));
    }

    @Test
    public void testCanParseWithComma() {
        assertThat((double) DistanceParser.parseDistance("1,2", DistanceParser.DistanceUnit.MI)).isEqualTo(1.9312128, offset(MM));
    }

    @Test
    public void testCanParseUnitKm() {
        assertThat(DistanceParser.DistanceUnit.parseUnit("km")).isEqualTo(DistanceParser.DistanceUnit.KM);
    }

    @Test
    public void testCanParseUnitMi() {
        assertThat(DistanceParser.DistanceUnit.parseUnit("mi")).isEqualTo(DistanceParser.DistanceUnit.MI);
    }

    @Test
    public void testCanParseUnitYd() {
        assertThat(DistanceParser.DistanceUnit.parseUnit("yd")).isEqualTo(DistanceParser.DistanceUnit.YD);
    }

    @Test
    public void testCanParseUnitM() {
        assertThat(DistanceParser.DistanceUnit.parseUnit("m")).isEqualTo(DistanceParser.DistanceUnit.M);
    }

    @Test
    public void testCanParseUnitFt() {
        assertThat(DistanceParser.DistanceUnit.parseUnit("ft")).isEqualTo(DistanceParser.DistanceUnit.FT);
    }

    @Test
    public void testCanParseCaseSensetiveUnit() {
        assertThat(DistanceParser.DistanceUnit.parseUnit("yD")).isEqualTo(DistanceParser.DistanceUnit.YD);
    }

    @Test
    public void testCannotParseEmptyUnit() {
        try {
            DistanceParser.DistanceUnit.parseUnit("");
            fail("Expected NumberFormatException");
        } catch (final NumberFormatException e) {
            //Expected
        }
    }

    @Test
    public void testCannotParseBadUnit() {
        try {
            DistanceParser.DistanceUnit.parseUnit("custom");
            fail("Expected NumberFormatException");
        } catch (final NumberFormatException e) {
            //Expected
        }
    }

}
