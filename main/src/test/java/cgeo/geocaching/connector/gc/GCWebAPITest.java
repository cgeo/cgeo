package cgeo.geocaching.connector.gc;

import org.junit.Test;
import static org.assertj.core.api.Assertions.assertThat;

public class GCWebAPITest {

    @Test
    public void formatCoordinates() {
        assertThat(GCWebAPI.formatCoordinates(51.5, -0.1)).isEqualTo("51.5,-0.1");
        assertThat(GCWebAPI.formatCoordinates(48.123456, 11.654321)).isEqualTo("48.123456,11.654321");
        assertThat(GCWebAPI.formatCoordinates(0.0, 0.0)).isEqualTo("0.0,0.0");
    }

    @Test
    public void formatCoordinatesCloseToZeroWithoutScientificNotation() {
        // W 0° 00.001' and S 0° 00.001'
        final double minute = 0.001 / 60;
        assertThat(String.valueOf(-minute)).contains("E"); // what was sent before
        assertThat(GCWebAPI.formatCoordinates(-minute, -minute)).isEqualTo("-0.000016666666666666667,-0.000016666666666666667");
        assertThat(GCWebAPI.formatCoordinates(51.4, 0.0005)).isEqualTo("51.4,0.00050");
    }
}
