package cgeo.geocaching.utils;

import android.graphics.Color;

import junit.framework.TestCase;
import static org.assertj.core.api.Java6Assertions.assertThat;

public class ColorUtilsTest extends TestCase {

    public static void testMaximalContrast() {
        assertThat(ColorUtils.getContrastRatio(Color.BLACK, Color.WHITE)).isEqualTo(21.0);
    }

    public static void testMinimalContrast() {
        assertThat(ColorUtils.getContrastRatio(Color.BLACK, Color.BLACK)).isEqualTo(1.0);
    }

    public static void testContrastSymmetric() {
        assertThat(ColorUtils.getContrastRatio(Color.DKGRAY, Color.WHITE)).isEqualTo(ColorUtils.getContrastRatio(Color.WHITE, Color.DKGRAY));
    }

    public static void testContrastAverage() {
        assertThat(ColorUtils.getContrastRatio(Color.DKGRAY, Color.WHITE)).isBetween(9.0, 10.0);
    }
}
