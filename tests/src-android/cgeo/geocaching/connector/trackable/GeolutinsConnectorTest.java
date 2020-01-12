package cgeo.geocaching.connector.trackable;

import cgeo.geocaching.models.Trackable;

import junit.framework.TestCase;
import static org.assertj.core.api.Java6Assertions.assertThat;

public class GeolutinsConnectorTest extends TestCase {

    public static void testCanHandleTrackable() {
        assertThat(getConnector().canHandleTrackable("GL000001")).isFalse();
        assertThat(getConnector().canHandleTrackable("GL00001")).isTrue();
        assertThat(getConnector().canHandleTrackable("GL00ABC")).isTrue();
        assertThat(getConnector().canHandleTrackable("GL00GHI")).isFalse();
        assertThat(getConnector().canHandleTrackable("GL0001")).isFalse();
        assertThat(getConnector().canHandleTrackable("SWABCD")).isFalse();
        assertThat(getConnector().canHandleTrackable("GK82A2")).isFalse();
        assertThat(getConnector().canHandleTrackable("TB1234")).isFalse();
        assertThat(getConnector().canHandleTrackable("UNKNOWN")).isFalse();
        assertThat(getConnector().canHandleTrackable("12345678-12")).isFalse();
        assertThat(getConnector().canHandleTrackable("12345678-12")).isFalse();
        assertThat(getConnector().canHandleTrackable("12345678-123")).isTrue();
        assertThat(getConnector().canHandleTrackable("123456-1234")).isFalse();
        assertThat(getConnector().canHandleTrackable("1234567-1234")).isTrue();
        assertThat(getConnector().canHandleTrackable("12345671234")).isFalse();
        assertThat(getConnector().canHandleTrackable("1234567123")).isFalse();
        assertThat(getConnector().canHandleTrackable("12345678-1234")).isTrue();
        assertThat(getConnector().canHandleTrackable("123456789-1234")).isFalse();
        assertThat(getConnector().canHandleTrackable("ABCDEFGH-ABCD")).isFalse();
        assertThat(getConnector().canHandleTrackable("ABCDEFGHIJKL")).isFalse();

        assertThat(getConnector().canHandleTrackable("GL00001", TrackableBrand.UNKNOWN)).isTrue();
        assertThat(getConnector().canHandleTrackable("GL00001", TrackableBrand.TRAVELBUG)).isFalse();
        assertThat(getConnector().canHandleTrackable("GL00001", TrackableBrand.GEOKRETY)).isFalse();
        assertThat(getConnector().canHandleTrackable("GL00001", TrackableBrand.GEOLUTINS)).isTrue();
    }

    public static void testGetTrackableCodeFromUrl() throws Exception {
        assertThat(getConnector().getTrackableCodeFromUrl("http://www.geolutins.com/profil_geolutin.php?ID_Geolutin_Selectionne=1976")).isEqualTo("GL007B8");
    }

    public static void testGetUrl() throws Exception {
        final Trackable trackable = new Trackable();
        trackable.setGeocode("GL007B8");
        assertThat(getConnector().getUrl(trackable)).isEqualTo("http://www.geolutins.com/profil_geolutin.php?ID_Geolutin_Selectionne=1976");
    }

    private static GeolutinsConnector getConnector() {
        return new GeolutinsConnector();
    }

    public static void testRecommendGeocode() throws Exception {
        assertThat(getConnector().recommendLogWithGeocode()).isFalse();
    }

}
