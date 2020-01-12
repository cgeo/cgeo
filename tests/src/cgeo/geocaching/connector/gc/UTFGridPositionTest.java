package cgeo.geocaching.connector.gc;

import org.junit.Test;
import static org.assertj.core.api.Java6Assertions.assertThat;

public class UTFGridPositionTest {

    @Test
    public void testValidUTFGridPosition() {
        assertThat(new UTFGridPosition(0, 0)).isNotNull();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInvalidUTFGridPosition() {
        assertThat(new UTFGridPosition(-1, 0)).isNotNull();
    }

    @Test
    public void testFromString() throws Exception {
        assertXYFromString("(1, 2)", 1, 2);
        assertXYFromString("(12, 34)", 12, 34);
        assertXYFromString("(34,56)", 34, 56);
        assertXYFromString("(34,  56)", 34, 56);
    }

    private static void assertXYFromString(final String key, final int x, final int y) {
        final UTFGridPosition pos = UTFGridPosition.fromString(key);
        assertThat(pos.getX()).isEqualTo(x);
        assertThat(pos.getY()).isEqualTo(y);
    }

}
