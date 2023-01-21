package cgeo.geocaching.utils;

import android.graphics.Color;

import org.junit.Test;
import static org.assertj.core.api.Java6Assertions.assertThat;

public class ColorUtilsTest {

    @Test
    public void testMaximalContrast() {
        assertThat(ColorUtils.getContrastRatio(Color.BLACK, Color.WHITE)).isEqualTo(21.0);
    }

    @Test
    public void testMinimalContrast() {
        assertThat(ColorUtils.getContrastRatio(Color.BLACK, Color.BLACK)).isEqualTo(1.0);
    }

    @Test
    public void testContrastSymmetric() {
        assertThat(ColorUtils.getContrastRatio(Color.DKGRAY, Color.WHITE)).isEqualTo(ColorUtils.getContrastRatio(Color.WHITE, Color.DKGRAY));
    }

    @Test
    public void testContrastAverage() {
        assertThat(ColorUtils.getContrastRatio(Color.DKGRAY, Color.WHITE)).isBetween(9.0, 10.0);
    }
}
