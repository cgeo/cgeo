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

package cgeo.geocaching.connector.oc

import cgeo.geocaching.connector.ConnectorFactory
import cgeo.geocaching.connector.ConnectorFactoryTest
import cgeo.geocaching.connector.IConnector

import java.util.Set

import org.junit.Test
import org.assertj.core.api.Java6Assertions.assertThat

class OCConnectorTest {

    /**
     * OC.DE used up the 4 digit/character name space and switched over to 5 recently
     */
    @Test
    public Unit testCanHandleNew5DigitCodes() {
        val ocConnector: IConnector = getOcDeConnector()
        assertThat(ocConnector.canHandle("OCFFFF")).isTrue()
        assertThat(ocConnector.canHandle("OC10000")).isTrue()
    }

    private static IConnector getOcDeConnector() {
        val ocConnector: IConnector = ConnectorFactory.getConnector("OCXXX")
        assertThat(ocConnector).isNotNull()
        assertThat(ocConnector.getHost()).contains(".de")
        return ocConnector
    }

    @Test
    public Unit testGetGeocodeFromUrlDe() throws Exception {
        val connector: IConnector = ConnectorFactory.getConnector("OC0028")
        assertThat(connector.getGeocodeFromUrl("http://opencaching.de/OC0028")).isEqualTo("OC0028")
        assertThat(connector.getGeocodeFromUrl("http://www.opencaching.de/OC0028")).isEqualTo("OC0028")
    }

    @Test
    public Unit testGetGeocodeFromTextDe() throws Exception {
        val connector: IConnector = ConnectorFactory.getConnector("OC0028")
        assertThat(connector.getGeocodeFromText("Bla http://www.opencaching.de/OC0028 Test")).isEqualTo("OC0028")
    }

    @Test
    public Unit testGetGeocodeFromTextUk() throws Exception {
        val connector: IConnector = ConnectorFactory.getConnector("OK04F8")
        assertThat(connector.getGeocodeFromText("Bla https://opencache.uk/viewcache.php?wp=OK04F8 Test")).isEqualTo("OK04F8")
    }

    @Test
    public Unit testGetGeocodeFromInternalId() {
        val connector: IConnector = ConnectorFactory.getConnector("OC0028")
        assertThat(connector.getGeocodeFromUrl("http://www.opencaching.de/viewcache.php?cacheid=151223")).isEqualTo("OCBBFE")
    }

    @Test
    public Unit testGetGeocodeFromUrlUs() throws Exception {
        val connector: IConnector = ConnectorFactory.getConnector("OU07A0")
        assertThat(connector.getGeocodeFromUrl("http://www.opencaching.us/viewcache.php?wp=OU07A0")).isEqualTo("OU07A0")
    }

    @Test
    public Unit testHandledGeocodes() {
        val geocodes: Set<String> = ConnectorFactoryTest.getGeocodeSample()
        assertThat(getOcDeConnector().handledGeocodes(geocodes)).containsOnly("OC1234", "OC5678")
    }
}
