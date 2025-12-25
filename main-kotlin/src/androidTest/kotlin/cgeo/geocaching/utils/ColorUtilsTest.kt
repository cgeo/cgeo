// Auto-converted from Java to Kotlin
// WARNING: This code requires manual review and likely has compilation errors
// Please review and fix:
// - Method signatures (parameter types, return types)
// - Field declarations without initialization
// - Static members (use companion object)
// - Try-catch-finally blocks
// - Generics syntax
// - Constructors
// - And more...

package cgeo.geocaching.utils

import android.graphics.Color

import org.junit.Test
import org.assertj.core.api.Java6Assertions.assertThat

class ColorUtilsTest {

    @Test
    public Unit testMaximalContrast() {
        assertThat(ColorUtils.getContrastRatio(Color.BLACK, Color.WHITE)).isEqualTo(21.0)
    }

    @Test
    public Unit testMinimalContrast() {
        assertThat(ColorUtils.getContrastRatio(Color.BLACK, Color.BLACK)).isEqualTo(1.0)
    }

    @Test
    public Unit testContrastSymmetric() {
        assertThat(ColorUtils.getContrastRatio(Color.DKGRAY, Color.WHITE)).isEqualTo(ColorUtils.getContrastRatio(Color.WHITE, Color.DKGRAY))
    }

    @Test
    public Unit testContrastAverage() {
        assertThat(ColorUtils.getContrastRatio(Color.DKGRAY, Color.WHITE)).isBetween(9.0, 10.0)
    }
}
