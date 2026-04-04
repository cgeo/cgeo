package cgeo.geocaching.enumerations;

import org.junit.Test;
import static org.assertj.core.api.Java6Assertions.assertThat;

public class CoordinateTypeTest {

    @Test
    public void testValueOf() {
        assertThat(CoordinateType.valueOf("CACHE")).isEqualTo(CoordinateType.CACHE);
        assertThat(CoordinateType.valueOf("WAYPOINT")).isEqualTo(CoordinateType.WAYPOINT);
        assertThat(CoordinateType.valueOf("NAMED_COORDINATE")).isEqualTo(CoordinateType.NAMED_COORDINATE);
    }

    @Test
    public void testEnumEquality() {
        assertThat(CoordinateType.CACHE).isNotEqualTo(CoordinateType.WAYPOINT);
        assertThat(CoordinateType.WAYPOINT).isNotEqualTo(CoordinateType.NAMED_COORDINATE);
        assertThat(CoordinateType.CACHE).isEqualTo(CoordinateType.CACHE);
    }
}
