package cgeo.geocaching;

import static org.assertj.core.api.Assertions.assertThat;

import android.test.AndroidTestCase;

public class TrackableTest extends AndroidTestCase {

    public static void testGetGeocode() {
        final Trackable trackable = createTrackable("tb1234");
        assertThat(trackable.getGeocode()).isEqualTo("TB1234");
    }

    public static void testSetLogsNull() {
        final Trackable trackable = new Trackable();
        trackable.setLogs(null);
        assertThat(trackable.getLogs()).as("Trackable logs").isNotNull();
    }

    public static void testTrackableUrl() {
        final Trackable trackable = createTrackable("TB1234");
        assertThat(trackable.getUrl()).isEqualTo("http://www.geocaching.com//track/details.aspx?tracker=TB1234");
    }

    public static void testGeokretUrl() {
        Trackable geokret = createTrackable("GK82A2");
        assertThat(geokret.getUrl()).isEqualTo("http://geokrety.org/konkret.php?id=33442");
    }

    public static void testLoggable() {
        assertThat(createTrackable("TB1234").isLoggable()).isTrue();
        assertThat(createTrackable("GK1234").isLoggable()).isTrue();
    }

    private static Trackable createTrackable(String geocode) {
        final Trackable trackable = new Trackable();
        trackable.setGeocode(geocode);
        return trackable;
    }

}
