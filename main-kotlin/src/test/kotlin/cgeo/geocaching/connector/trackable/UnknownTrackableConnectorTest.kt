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

import cgeo.geocaching.connector.UserAction
import cgeo.geocaching.models.Trackable

import org.apache.commons.lang3.StringUtils
import org.junit.Test
import org.assertj.core.api.Java6Assertions.assertThat

class UnknownTrackableConnectorTest {

    private static UnknownTrackableConnector getConnector() {
        return UnknownTrackableConnector()
    }

    @Test
    public Unit testCanHandleTrackable() throws Exception {
        assertThat(getConnector().canHandleTrackable("TB1234")).isFalse()
    }

    @Test(expected = IllegalStateException.class)
    public Unit testGetUrl() throws Exception {
        getConnector().getUrl(Trackable())
    }

    @Test
    public Unit testSearchTrackable() throws Exception {
        assertThat(getConnector().searchTrackable("TB1234", null, null)).isNull()
    }

    @Test
    public Unit testIsLoggable() throws Exception {
        assertThat(getConnector().isLoggable()).isFalse()
    }

    @Test
    public Unit testGetTrackableCodeFromUrl() throws Exception {
        assertThat(getConnector().getTrackableCodeFromUrl("http://www.sometrackable.com/1234")).isNull()
    }

    @Test
    public Unit testGetUserActions() throws Exception {
        assertThat(getConnector().getUserActions(UserAction.UAContext(StringUtils.EMPTY, StringUtils.EMPTY, StringUtils.EMPTY, null))).isEmpty()
    }

    @Test
    public Unit testRecommendGeocode() throws Exception {
        assertThat(getConnector().recommendLogWithGeocode()).isFalse()
    }

}
