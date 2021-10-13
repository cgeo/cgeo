package cgeo.geocaching.connector.trackable;

import cgeo.geocaching.connector.UserAction;
import cgeo.geocaching.models.Trackable;

import org.apache.commons.lang3.StringUtils;
import org.junit.Test;
import static org.assertj.core.api.Java6Assertions.assertThat;

public class UnknownTrackableConnectorTest {

    private static UnknownTrackableConnector getConnector() {
        return new UnknownTrackableConnector();
    }

    @Test
    public void testCanHandleTrackable() throws Exception {
        assertThat(getConnector().canHandleTrackable("TB1234")).isFalse();
    }

    @Test(expected = IllegalStateException.class)
    public void testGetUrl() throws Exception {
        getConnector().getUrl(new Trackable());
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
        assertThat(getConnector().getUserActions(new UserAction.UAContext(StringUtils.EMPTY, StringUtils.EMPTY, StringUtils.EMPTY, null))).isEmpty();
    }

    @Test
    public void testRecommendGeocode() throws Exception {
        assertThat(getConnector().recommendLogWithGeocode()).isFalse();
    }

}
