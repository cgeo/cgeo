package cgeo.geocaching.connector.trackable;

import static org.assertj.core.api.Assertions.assertThat;

import cgeo.geocaching.Trackable;

import junit.framework.TestCase;

public class TravelBugConnectorTest extends TestCase {

    public static void testCanHandleTrackable() {
        assertThat(getConnector().canHandleTrackable("TB1234")).isTrue();
        assertThat(getConnector().canHandleTrackable("TB1")).isTrue();
        assertThat(getConnector().canHandleTrackable("TB123F")).isTrue();
        assertThat(getConnector().canHandleTrackable("TB123Z")).isTrue();
        assertThat(getConnector().canHandleTrackable("TB4JD36")).isTrue(); // existing TB, 5 specific characters
        assertThat(getConnector().canHandleTrackable("GK1234")).isTrue(); // valid secret code, even though this might be a geokrety
        assertThat(getConnector().canHandleTrackable("GST9HV")).isTrue(); // existing secret code
        assertThat(getConnector().canHandleTrackable("UNKNOWN")).isFalse();
    }

    public static void testGetUrl() {
        final Trackable trackable = new Trackable();
        trackable.setGeocode("TB2345");
        assertThat(getConnector().getUrl(trackable)).isEqualTo("http://www.geocaching.com//track/details.aspx?tracker=TB2345");
    }

    public static void testOnlineSearchBySecretCode() {
        final Trackable trackable = getConnector().searchTrackable("GST9HV", null, null);
        assertThat(trackable).isNotNull();
        assert trackable != null;
        assertThat(trackable.getName()).isEqualTo("Deutschland");
    }

    public static void testOnlineSearchByPublicCode() {
        final Trackable trackable = getConnector().searchTrackable("TB4JD36", null, null);
        assertThat(trackable).isNotNull();
        assert trackable != null;
        assertThat(trackable.getName()).isEqualTo("Mein Kilometerzähler");
    }

    private static TravelBugConnector getConnector() {
        return TravelBugConnector.getInstance();
    }

    public static void testGetTrackableCodeFromUrl() throws Exception {
        assertThat(TravelBugConnector.getInstance().getTrackableCodeFromUrl("http://coord.info/TB1234")).isEqualTo("TB1234");
        assertThat(TravelBugConnector.getInstance().getTrackableCodeFromUrl("http://www.coord.info/TB1234")).isEqualTo("TB1234");
        assertThat(TravelBugConnector.getInstance().getTrackableCodeFromUrl("http://geocaching.com/track/details.aspx?tracker=TB1234")).isEqualTo("TB1234");
        assertThat(TravelBugConnector.getInstance().getTrackableCodeFromUrl("http://www.geocaching.com/track/details.aspx?tracker=TB1234")).isEqualTo("TB1234");

        // do not match coord.info URLs of caches
        assertThat(TravelBugConnector.getInstance().getTrackableCodeFromUrl("http://coord.info/GC1234")).isNull();
        assertThat(TravelBugConnector.getInstance().getTrackableCodeFromUrl("http://www.coord.info/GC1234")).isNull();
    }

    public static void testRecommendGeocode() throws Exception {
        assertThat(getConnector().recommendLogWithGeocode()).isFalse();
    }
}
