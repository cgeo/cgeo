package cgeo.geocaching.connector.trackable;

import cgeo.geocaching.models.Trackable;

import org.junit.Test;
import static org.assertj.core.api.Java6Assertions.assertThat;

public class TravelBugConnectorTest {

    @Test
    public void testCanHandleTrackable() {
        assertThat(getConnector().canHandleTrackable("TB1234")).isTrue();
        assertThat(getConnector().canHandleTrackable("TB1")).isTrue();
        assertThat(getConnector().canHandleTrackable("TB123F")).isTrue();
        assertThat(getConnector().canHandleTrackable("TB123Z")).isTrue();
        assertThat(getConnector().canHandleTrackable("TB4JD36")).isTrue(); // existing TB, 5 specific characters
        assertThat(getConnector().canHandleTrackable("GK1234")).isTrue(); // valid secret code, even though this might be a geokrety
        assertThat(getConnector().canHandleTrackable("GST9HV")).isTrue(); // existing secret code 6 digits
        assertThat(getConnector().canHandleTrackable("999MROVER")).isTrue(); // formatted for the special secret code 9 digits for TB5EFXK
        assertThat(getConnector().canHandleTrackable("NOTBC")).isFalse(); // shorter than 6 digits
        assertThat(getConnector().canHandleTrackable("UNKNOWN")).isFalse(); // longer than 6 / shorter than 8 digits
        assertThat(getConnector().canHandleTrackable("NOTKNOWN")).isFalse(); // longer than 6 / shorter than 8 digits
        assertThat(getConnector().canHandleTrackable("NOTRACKING")).isFalse(); // longer than 9 digits

        assertThat(getConnector().canHandleTrackable("GC1234")).isTrue();
        assertThat(getConnector().canHandleTrackable("GC1234", TrackableBrand.UNKNOWN)).isTrue(); // accepted as TB
        assertThat(getConnector().canHandleTrackable("GC1234", TrackableBrand.TRAVELBUG)).isTrue(); // accepted explicitly as TB
        assertThat(getConnector().canHandleTrackable("GC1234", TrackableBrand.GEOKRETY)).isFalse(); // Not a TB
    }

    @Test
    public void testGetUrl() {
        final Trackable trackable = new Trackable();
        trackable.setGeocode("TB2345");
        assertThat(getConnector().getUrl(trackable)).isEqualTo("https://www.geocaching.com//track/details.aspx?tracker=TB2345");
    }

    @Test
    public void testOnlineSearchBySecretCode() {
        final Trackable trackable = getConnector().searchTrackable("GST9HV", null, null);
        assertThat(trackable).isNotNull();
        assertThat(trackable.getName()).isEqualTo("Deutschland");
    }

    @Test
    public void testOnlineSearchByPublicCode() {
        final Trackable trackable = getConnector().searchTrackable("TB4JD36", null, null);
        assertThat(trackable).isNotNull();
        assertThat(trackable.getName()).isEqualTo("Mein Kilometerzähler");
    }

    private static TravelBugConnector getConnector() {
        return TravelBugConnector.getInstance();
    }

    @Test
    public void testGetTrackableCodeFromUrl() {
        assertThat(TravelBugConnector.getInstance().getTrackableCodeFromUrl("http://coord.info/TB1234")).isEqualTo("TB1234");
        assertThat(TravelBugConnector.getInstance().getTrackableCodeFromUrl("http://www.coord.info/TB1234")).isEqualTo("TB1234");
        assertThat(TravelBugConnector.getInstance().getTrackableCodeFromUrl("http://geocaching.com/track/details.aspx?tracker=TB1234")).isEqualTo("TB1234");
        assertThat(TravelBugConnector.getInstance().getTrackableCodeFromUrl("https://www.geocaching.com/track/details.aspx?tracker=TB1234")).isEqualTo("TB1234");

        // do not match coord.info URLs of caches
        assertThat(TravelBugConnector.getInstance().getTrackableCodeFromUrl("http://coord.info/GC1234")).isEqualTo("GC1234");
        assertThat(TravelBugConnector.getInstance().getTrackableCodeFromUrl("http://www.coord.info/GC1234")).isEqualTo("GC1234");
    }

    @Test
    public void testRecommendGeocode() {
        assertThat(getConnector().recommendLogWithGeocode()).isFalse();
    }
}
