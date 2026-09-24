package cgeo.geocaching.unifiedmap.googlemaps;

import org.junit.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

public class ScaleDrawerTest {

    private static void assertRounded(final double value, final double expected) {
        assertThat(ScaleDrawer.roundToNiceValue(value)).isCloseTo(expected, within(1e-9));
    }

    @Test
    public void testRoundToNiceValue() {
        assertRounded(0, 0);
        assertRounded(0.34, 0.3);
        assertRounded(0.3, 0.3);
        assertRounded(1.0, 1);
        assertRounded(1.99, 1);
        assertRounded(7.6, 7);
        assertRounded(9.99, 9);
        assertRounded(10, 10);
        assertRounded(14.9, 10);
        assertRounded(15, 15);
        assertRounded(99.9, 95);
        assertRounded(100, 100);
        assertRounded(149, 100);
        assertRounded(351, 350);
        assertRounded(999, 950);
        assertRounded(1234, 1000);
        assertRounded(7777, 7500);
    }
}
