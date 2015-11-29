package cgeo.geocaching.connector.trackable;

import static org.assertj.core.api.Assertions.assertThat;

import cgeo.geocaching.Trackable;

import junit.framework.TestCase;

public class UnknownTrackableConnectorTest extends TestCase {

    private static UnknownTrackableConnector getConnector() {
        return new UnknownTrackableConnector();
    }

    public static void testCanHandleTrackable() throws Exception {
        assertThat(getConnector().canHandleTrackable("TB1234")).isFalse();
    }

    public static void testGetUrl() throws Exception {
        try {
            getConnector().getUrl(new Trackable());
            fail("IllegalStateException expected");
        } catch (final IllegalStateException e) {
            // empty
        }
    }

    public static void testSearchTrackable() throws Exception {
        assertThat(getConnector().searchTrackable("TB1234", null, null)).isNull();
    }

    public static void testIsLoggable() throws Exception {
        assertThat(getConnector().isLoggable()).isFalse();
    }

    public static void testGetTrackableCodeFromUrl() throws Exception {
        assertThat(getConnector().getTrackableCodeFromUrl("http://www.sometrackable.com/1234")).isNull();
    }

    public static void testGetUserActions() throws Exception {
        assertThat(getConnector().getUserActions()).isEmpty();
    }

    public static void testRecommendGeocode() throws Exception {
        assertThat(getConnector().recommendLogWithGeocode()).isFalse();
    }

}
