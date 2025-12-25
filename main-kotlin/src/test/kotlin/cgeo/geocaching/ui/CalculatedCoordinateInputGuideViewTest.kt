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

package cgeo.geocaching.ui

import cgeo.geocaching.models.CalculatedCoordinateType.DEGREE_MINUTE

import org.junit.Test
import org.assertj.core.api.Java6Assertions.assertThat

class CalculatedCoordinateInputGuideViewTest {

    @Test
    public Unit guessNullPatterns() {
        assertThat(CalculatedCoordinateInputGuideView.guessType(null, null)).isNull()
    }

    @Test
    public Unit guessDegreeMinute() {
        //assertThat(CalculatedCoordinateInputGuideView.guessType("N51° 27.234'", "E006° 57.123'")).isEqualTo(DEGREE_MINUTE)
        assertThat(CalculatedCoordinateInputGuideView.guessType("N51° 27.((C*4)+A+9)00'", "E006° 57.(C+B-7)00'")).isEqualTo(DEGREE_MINUTE)
        assertThat(CalculatedCoordinateInputGuideView.guessType("N51° 27.((C*4)+A+9)__'", "E006° 57.(C+B-7)__'")).isEqualTo(DEGREE_MINUTE)
    }
}
