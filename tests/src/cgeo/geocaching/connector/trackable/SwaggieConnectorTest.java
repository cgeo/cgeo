package cgeo.geocaching.connector.trackable;

import static org.assertj.core.api.Assertions.assertThat;

import cgeo.geocaching.Trackable;

import junit.framework.TestCase;

public class SwaggieConnectorTest extends TestCase {

    public static void testCanHandleTrackable() {
        assertThat(new SwaggieConnector().canHandleTrackable("SW0001")).isTrue();
        assertThat(new SwaggieConnector().canHandleTrackable("SWABCD")).isFalse();
        assertThat(new SwaggieConnector().canHandleTrackable("GK82A2")).isFalse();
        assertThat(new SwaggieConnector().canHandleTrackable("TB1234")).isFalse();
        assertThat(new SwaggieConnector().canHandleTrackable("UNKNOWN")).isFalse();
    }

    public static void testGetTrackableCodeFromUrl() throws Exception {
        assertThat(new SwaggieConnector().getTrackableCodeFromUrl("http://geocaching.com.au/swaggie/sw0017")).isEqualTo("SW0017");
    }

    public static void testGetUrl() throws Exception {
        final Trackable trackable = new Trackable();
        trackable.setGeocode("SW0017");
        assertThat(new SwaggieConnector().getUrl(trackable)).isEqualTo("http://geocaching.com.au/swaggie/SW0017");
    }

    private static SwaggieConnector getConnector() {
        return new SwaggieConnector();
    }

    public static void testRecommendGeocode() throws Exception {
        assertThat(getConnector().recommendLogWithGeocode()).isFalse();
    }

}
