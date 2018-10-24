package cgeo.geocaching.location;

import org.junit.Test;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.assertj.core.api.Assertions.offset;

public class DistanceParserTest {

    private static final double MM = 1e-6; // 1mm, in kilometers

    @Test
    public void testFormats() {
        assertThat((double) DistanceParser.parseDistance("1200 m", true)).isEqualTo(1.2, offset(MM));
        assertThat((double) DistanceParser.parseDistance("1.2 km", true)).isEqualTo(1.2, offset(MM));
        assertThat((double) DistanceParser.parseDistance("1200 ft", true)).isEqualTo(0.36576, offset(MM));
        assertThat((double) DistanceParser.parseDistance("1200 yd", true)).isEqualTo(1.09728, offset(MM));
        assertThat((double) DistanceParser.parseDistance("1.2 mi", true)).isEqualTo(1.9312128, offset(MM));
    }

    @Test
    public void testImplicit() {
        assertThat((double) DistanceParser.parseDistance("1200", true)).isEqualTo(1.2, offset(MM));
        assertThat((double) DistanceParser.parseDistance("1200", false)).isEqualTo(0.36576, offset(MM));
    }

    @Test
    public void testComma() {
        assertThat((double) DistanceParser.parseDistance("1,2km", true)).isEqualTo(1.2, offset(MM));
    }

    @Test
    public void testFeet() {
        assertThat((double) DistanceParser.parseDistance("1200 FT", false)).isEqualTo(0.36576, offset(MM));
    }

    @Test
    public void testWithUnitKm() {
        assertThat((double) DistanceParser.convertDistance(1.2f, DistanceParser.UNIT.KM)).isEqualTo(1.2, offset(MM));
    }

    @Test
    public void testWithUnitM() {
        assertThat((double) DistanceParser.convertDistance(1200, DistanceParser.UNIT.M)).isEqualTo(1.2, offset(MM));
    }

    @Test
    public void testWithUnitFt() {
        assertThat((double) DistanceParser.convertDistance(1200, DistanceParser.UNIT.FT)).isEqualTo(0.36576, offset(MM));
    }

    @Test
    public void testWithUnitMi() {
        assertThat((double) DistanceParser.convertDistance(1.2f, DistanceParser.UNIT.MI)).isEqualTo(1.9312128, offset(MM));
    }

}
