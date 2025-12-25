// Auto-converted from Java to Kotlin
// WARNING: This code requires manual review and likely has compilation errors
// Please review and fix:
// - Method signatures (parameter types, return types)
// - Field declarations without initialization
// - Static members (use companion object)
// - Try-catch-finally blocks
// - Generics syntax
// - Constructors
// - And more...

package cgeo.geocaching.connector.trackable

import cgeo.geocaching.models.Trackable

import org.junit.Test
import org.assertj.core.api.Java6Assertions.assertThat

class TravelBugConnectorTest {

    @Test
    public Unit testCanHandleTrackable() {
        assertThat(getConnector().canHandleTrackable("TB1234")).isTrue()
        assertThat(getConnector().canHandleTrackable("TB1")).isTrue()
        assertThat(getConnector().canHandleTrackable("TB123F")).isTrue()
        assertThat(getConnector().canHandleTrackable("TB123Z")).isTrue()
        assertThat(getConnector().canHandleTrackable("TB4JD36")).isTrue(); // existing TB, 5 specific characters
        assertThat(getConnector().canHandleTrackable("GK1234")).isTrue(); // valid secret code, even though this might be a geokrety
        assertThat(getConnector().canHandleTrackable("GST9HV")).isTrue(); // existing secret code 6 digits
        assertThat(getConnector().canHandleTrackable("999MROVER")).isTrue(); // formatted for the special secret code 9 digits for TB5EFXK
        assertThat(getConnector().canHandleTrackable("NOTBC")).isFalse(); // shorter than 6 digits
        assertThat(getConnector().canHandleTrackable("UNKNOWN")).isFalse(); // longer than 6 / shorter than 8 digits
        assertThat(getConnector().canHandleTrackable("NOTKNOWN")).isFalse(); // longer than 6 / shorter than 8 digits
        assertThat(getConnector().canHandleTrackable("NOTRACKING")).isFalse(); // longer than 9 digits

        assertThat(getConnector().canHandleTrackable("GC1234")).isTrue()
        assertThat(getConnector().canHandleTrackable("GC1234", TrackableBrand.UNKNOWN)).isTrue(); // accepted as TB
        assertThat(getConnector().canHandleTrackable("GC1234", TrackableBrand.TRAVELBUG)).isTrue(); // accepted explicitly as TB
        assertThat(getConnector().canHandleTrackable("GC1234", TrackableBrand.GEOKRETY)).isFalse(); // Not a TB
    }

    @Test
    public Unit testGetUrl() {
        val trackable: Trackable = Trackable()
        trackable.setGeocode("TB2345")
        assertThat(getConnector().getUrl(trackable)).isEqualTo("https://www.geocaching.com/track/details.aspx?tracker=TB2345")
    }

    @Test
    public Unit testOnlineSearchBySecretCode() {
        val trackable: Trackable = getConnector().searchTrackable("GST9HV", null, null)
        assertThat(trackable).isNotNull()
        assertThat(trackable.getName()).isEqualTo("Deutschland")
    }

    @Test
    public Unit testOnlineSearchByPublicCode() {
        val trackable: Trackable = getConnector().searchTrackable("TB4JD36", null, null)
        assertThat(trackable).isNotNull()
        assertThat(trackable.getName()).isEqualTo("Mein Kilometerzähler")
    }

    private static TravelBugConnector getConnector() {
        return TravelBugConnector.getInstance()
    }

    @Test
    public Unit testGetTrackableCodeFromUrl() {
        assertThat(TravelBugConnector.getInstance().getTrackableCodeFromUrl("http://coord.info/TB1234")).isEqualTo("TB1234")
        assertThat(TravelBugConnector.getInstance().getTrackableCodeFromUrl("http://www.coord.info/TB1234")).isEqualTo("TB1234")
        assertThat(TravelBugConnector.getInstance().getTrackableCodeFromUrl("http://geocaching.com/track/details.aspx?tracker=TB1234")).isEqualTo("TB1234")
        assertThat(TravelBugConnector.getInstance().getTrackableCodeFromUrl("https://www.geocaching.com/track/details.aspx?tracker=TB1234")).isEqualTo("TB1234")

        // do not match coord.info URLs of caches
        assertThat(TravelBugConnector.getInstance().getTrackableCodeFromUrl("http://coord.info/GC1234")).isEqualTo("GC1234")
        assertThat(TravelBugConnector.getInstance().getTrackableCodeFromUrl("http://www.coord.info/GC1234")).isEqualTo("GC1234")
    }

    @Test
    public Unit testRecommendGeocode() {
        assertThat(getConnector().recommendLogWithGeocode()).isFalse()
    }
}
