package cgeo.geocaching;

import static org.assertj.core.api.Assertions.assertThat;

import cgeo.geocaching.location.Geopoint;

import android.test.AndroidTestCase;

public class DestinationTest extends AndroidTestCase {

    private Destination dest = null;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        dest = new Destination(1, 10000, new Geopoint(52.5, 9.33));
    }

    public void testSomething() {
        assertThat(dest.getId()).isEqualTo(1);
        assertThat(dest.getDate()).isEqualTo(10000);
        assertThat(dest.getCoords().getLatitude()).isEqualTo(52.5);
        assertThat(dest.getCoords().getLongitude()).isEqualTo(9.33);
    }
}