package cgeo.geocaching.models;

import cgeo.geocaching.location.Geopoint;

import org.junit.Before;
import org.junit.Test;
import static org.assertj.core.api.Java6Assertions.assertThat;

public class DestinationTest {

    private Destination dest = null;

    @Before
    public void setUp() throws Exception {
        dest = new Destination(1, 10000, new Geopoint(52.5, 9.33));
    }

    @Test
    public void testSomething() {
        assertThat(dest.getId()).isEqualTo(1);
        assertThat(dest.getDate()).isEqualTo(10000);
        assertThat(dest.getCoords().getLatitude()).isEqualTo(52.5);
        assertThat(dest.getCoords().getLongitude()).isEqualTo(9.33);
    }
}
