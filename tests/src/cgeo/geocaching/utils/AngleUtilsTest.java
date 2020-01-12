package cgeo.geocaching.utils;

import org.junit.Test;
import static org.assertj.core.api.Java6Assertions.assertThat;

public class AngleUtilsTest {

    @Test
    public void testNormalize() {
        assertThat(AngleUtils.normalize(0)).isEqualTo(0.0f);
        assertThat(AngleUtils.normalize(-0.0f)).isEqualTo(0.0f);
        assertThat(AngleUtils.normalize(360)).isEqualTo(0.0f);
        assertThat(AngleUtils.normalize(720)).isEqualTo(0.0f);
        assertThat(AngleUtils.normalize(-360)).isEqualTo(0.0f);
        assertThat(AngleUtils.normalize(-720)).isEqualTo(0.0f);
        assertThat(AngleUtils.normalize(721)).isEqualTo(1.0f);
        assertThat(AngleUtils.normalize(-721)).isEqualTo(359.0f);
        assertThat(AngleUtils.normalize(-Float.MIN_VALUE)).isEqualTo(0.0f);
    }

    @Test
    public void testDifference() {
        assertThat(AngleUtils.difference(12, 12)).isEqualTo(0.0f);
        assertThat(AngleUtils.difference(372, 12)).isEqualTo(0.0f);
        assertThat(AngleUtils.difference(12, 372)).isEqualTo(0.0f);
        assertThat(AngleUtils.difference(10, 20)).isEqualTo(10.0f);
        assertThat(AngleUtils.difference(355, 5)).isEqualTo(10.0f);
        assertThat(AngleUtils.difference(715, -715)).isEqualTo(10.0f);
        assertThat(AngleUtils.difference(20, 10)).isEqualTo(-10.0f);
        assertThat(AngleUtils.difference(5, 355)).isEqualTo(-10.0f);
        assertThat(AngleUtils.difference(-715, 715)).isEqualTo(-10.0f);
        assertThat(AngleUtils.difference(-90, 90)).isEqualTo(-180.0f);
        assertThat(AngleUtils.difference(90, -90)).isEqualTo(-180.0f);
    }
}
