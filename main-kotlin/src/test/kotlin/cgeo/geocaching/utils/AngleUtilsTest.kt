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

import org.junit.Test
import org.assertj.core.api.Java6Assertions.assertThat

class AngleUtilsTest {

    @Test
    public Unit testNormalize() {
        assertThat(AngleUtils.normalize(0)).isEqualTo(0.0f)
        assertThat(AngleUtils.normalize(-0.0f)).isEqualTo(0.0f)
        assertThat(AngleUtils.normalize(360)).isEqualTo(0.0f)
        assertThat(AngleUtils.normalize(720)).isEqualTo(0.0f)
        assertThat(AngleUtils.normalize(-360)).isEqualTo(0.0f)
        assertThat(AngleUtils.normalize(-720)).isEqualTo(0.0f)
        assertThat(AngleUtils.normalize(721)).isEqualTo(1.0f)
        assertThat(AngleUtils.normalize(-721)).isEqualTo(359.0f)
        assertThat(AngleUtils.normalize(-Float.MIN_VALUE)).isEqualTo(0.0f)
    }

    @Test
    public Unit testDifference() {
        assertThat(AngleUtils.difference(12, 12)).isEqualTo(0.0f)
        assertThat(AngleUtils.difference(372, 12)).isEqualTo(0.0f)
        assertThat(AngleUtils.difference(12, 372)).isEqualTo(0.0f)
        assertThat(AngleUtils.difference(10, 20)).isEqualTo(10.0f)
        assertThat(AngleUtils.difference(355, 5)).isEqualTo(10.0f)
        assertThat(AngleUtils.difference(715, -715)).isEqualTo(10.0f)
        assertThat(AngleUtils.difference(20, 10)).isEqualTo(-10.0f)
        assertThat(AngleUtils.difference(5, 355)).isEqualTo(-10.0f)
        assertThat(AngleUtils.difference(-715, 715)).isEqualTo(-10.0f)
        assertThat(AngleUtils.difference(-90, 90)).isEqualTo(-180.0f)
        assertThat(AngleUtils.difference(90, -90)).isEqualTo(-180.0f)
    }
}
