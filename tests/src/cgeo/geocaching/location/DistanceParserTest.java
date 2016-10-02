package cgeo.geocaching.location;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.offset;

import junit.framework.TestCase;

public class DistanceParserTest extends TestCase {

    private static final double MM = 1e-6; // 1mm, in kilometers

    public static void testFormats() {
        assertThat((double) DistanceParser.parseDistance("1200 m", true)).isEqualTo(1.2, offset(MM));
        assertThat((double) DistanceParser.parseDistance("1.2 km", true)).isEqualTo(1.2, offset(MM));
        assertThat((double) DistanceParser.parseDistance("1200 ft", true)).isEqualTo(0.36576, offset(MM));
        assertThat((double) DistanceParser.parseDistance("1200 yd", true)).isEqualTo(1.09728, offset(MM));
        assertThat((double) DistanceParser.parseDistance("1.2 mi", true)).isEqualTo(1.9312128, offset(MM));
    }

    public static void testImplicit() {
        assertThat((double) DistanceParser.parseDistance("1200", true)).isEqualTo(1.2, offset(MM));
        assertThat((double) DistanceParser.parseDistance("1200", false)).isEqualTo(0.36576, offset(MM));
    }

    public static void testComma() {
        assertThat((double) DistanceParser.parseDistance("1,2km", true)).isEqualTo(1.2, offset(MM));
    }

    public static void testFeet() {
        assertThat((double) DistanceParser.parseDistance("1200 FT", false)).isEqualTo(0.36576, offset(MM));
    }

}
