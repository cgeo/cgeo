package cgeo.geocaching.connector.trackable;

import static org.assertj.core.api.Assertions.assertThat;
import junit.framework.TestCase;

public class GeokretyConnectorTest extends TestCase {

    public static void testCanHandleTrackable() {
        assertThat(new GeokretyConnector().canHandleTrackable("GK82A2")).isTrue();
        assertThat(new GeokretyConnector().canHandleTrackable("GKXYZ1")).isFalse(); // non hex
        assertThat(new GeokretyConnector().canHandleTrackable("TB1234")).isFalse();
        assertThat(new GeokretyConnector().canHandleTrackable("UNKNOWN")).isFalse();
    }

    public static void testGetTrackableCodeFromUrl() throws Exception {
        assertThat(new GeokretyConnector().getTrackableCodeFromUrl("http://www.geokrety.org/konkret.php?id=30970")).isEqualTo("GK78FA");
        assertThat(new GeokretyConnector().getTrackableCodeFromUrl("http://geokrety.org/konkret.php?id=30970")).isEqualTo("GK78FA");
    }

    public static void testGeocode() throws Exception {
        assertThat(GeokretyConnector.geocode(38849)).isEqualTo("GK97C1");
    }

    public static void testGetId() throws Exception {
        assertThat(GeokretyConnector.getId("GK97C1")).isEqualTo(38849);
    }

}
