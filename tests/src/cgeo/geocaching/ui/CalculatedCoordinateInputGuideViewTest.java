package cgeo.geocaching.ui;

import static cgeo.geocaching.models.CalculatedCoordinateType.DEGREE_MINUTE;

import org.junit.Test;
import static org.assertj.core.api.Java6Assertions.assertThat;

public class CalculatedCoordinateInputGuideViewTest {

    @Test
    public void guessNullPatterns() {
        assertThat(CalculatedCoordinateInputGuideView.guessType(null, null)).isNull();
    }

    @Test
    public void guessDegreeMinute() {
        //assertThat(CalculatedCoordinateInputGuideView.guessType("N51° 27.234'", "E006° 57.123'")).isEqualTo(DEGREE_MINUTE);
        assertThat(CalculatedCoordinateInputGuideView.guessType("N51° 27.((C*4)+A+9)00'", "E006° 57.(C+B-7)00'")).isEqualTo(DEGREE_MINUTE);
        assertThat(CalculatedCoordinateInputGuideView.guessType("N51° 27.((C*4)+A+9)__'", "E006° 57.(C+B-7)__'")).isEqualTo(DEGREE_MINUTE);
    }
}
