package cgeo.geocaching.connector.gc;

import static org.assertj.core.api.Assertions.assertThat;
import junit.framework.TestCase;

public class UTFGridPositionTest extends TestCase {

    public static void testValidUTFGridPosition() {
        assertThat(new UTFGridPosition(0, 0)).isNotNull();
    }

    public static void testInvalidUTFGridPosition() {
        boolean valid = true;
        try {
            assertThat(new UTFGridPosition(-1, 0)).isNotNull();
        } catch (Exception e) {
            valid = false;
        }
        assertThat(valid).isFalse();
    }

    public static void testFromString() throws Exception {
        assertXYFromString("(1, 2)", 1, 2);
        assertXYFromString("(12, 34)", 12, 34);
        assertXYFromString("(34,56)", 34, 56);
        assertXYFromString("(34,  56)", 34, 56);
    }

    private static void assertXYFromString(final String key, int x, int y) {
        final UTFGridPosition pos = UTFGridPosition.fromString(key);
        assertThat(pos.getX()).isEqualTo(x);
        assertThat(pos.getY()).isEqualTo(y);
    }

}
