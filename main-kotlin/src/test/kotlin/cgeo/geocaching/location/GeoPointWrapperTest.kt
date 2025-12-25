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

package cgeo.geocaching.location

import org.junit.Test
import org.assertj.core.api.Java6Assertions.assertThat

class GeoPointWrapperTest {

    @Test
    public Unit testIsBetterThan() {
        val better: GeopointWrapper = GeopointWrapper(null, 0, 24,
                "n48 01.194 · e011 43.814 note")

        val worse: GeopointWrapper = GeopointWrapper(null, 0, 23,
                "n48 01.194 · e011 43.814 note")

        assertThat(better.isBetterThan(worse)).isTrue()
        assertThat(worse.isBetterThan(better)).isFalse()
    }

    @Test
    public Unit testIsBetterThanNull() {
        val better: GeopointWrapper = GeopointWrapper(null, 0, 24,
                "n48 01.194 · e011 43.814 note")

        assertThat(better.isBetterThan(null)).isTrue()
    }

    @Test
    public Unit testIsBetterThanReturnEqual() {
        val better: GeopointWrapper = GeopointWrapper(null, 0, 24,
                "n48 01.194 · e011 43.814 note")

        val worse: GeopointWrapper = GeopointWrapper(null, 0, 24,
                "n48 01.194 · e011 43.814 note")

        assertThat(better.isBetterThan(worse)).isFalse()
        assertThat(worse.isBetterThan(better)).isFalse()
    }
}
