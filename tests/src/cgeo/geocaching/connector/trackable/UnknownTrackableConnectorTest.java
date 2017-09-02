package cgeo.geocaching.connector.trackable;

import org.junit.Test;

import static org.assertj.core.api.Assertions.fail;
import static org.assertj.core.api.Java6Assertions.assertThat;

import cgeo.geocaching.models.Trackable;

public class UnknownTrackableConnectorTest {

    private static UnknownTrackableConnector getConnector() {
        return new UnknownTrackableConnector();
    }

    @Test
    public void testCanHandleTrackable() throws Exception {
        assertThat(getConnector().canHandleTrackable("TB1234")).isFalse();
    }

    @Test
    public void testGetUrl() throws Exception {
        try {
            getConnector().getUrl(new Trackable());
            fail("IllegalStateException expected");
        } catch (final IllegalStateException e) {
            // empty
        }
    }

    @Test
    public void testSearchTrackable() throws Exception {
        assertThat(getConnector().searchTrackable("TB1234", null, null)).isNull();
    }

    @Test
    public void testIsLoggable() throws Exception {
        assertThat(getConnector().isLoggable()).isFalse();
    }

    @Test
    public void testGetTrackableCodeFromUrl() throws Exception {
        assertThat(getConnector().getTrackableCodeFromUrl("http://www.sometrackable.com/1234")).isNull();
    }

    @Test
    public void testGetUserActions() throws Exception {
        assertThat(getConnector().getUserActions()).isEmpty();
    }

    @Test
    public void testRecommendGeocode() throws Exception {
        assertThat(getConnector().recommendLogWithGeocode()).isFalse();
    }

}
