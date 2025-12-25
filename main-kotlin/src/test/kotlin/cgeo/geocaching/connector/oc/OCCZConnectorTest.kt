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

import org.junit.Test
import org.assertj.core.api.Java6Assertions.assertThat

class OCCZConnectorTest {

    @Test
    public Unit testGetGeocodeFromUrl() throws Exception {
        val connector: OCCZConnector = OCCZConnector()
        assertThat(connector.getGeocodeFromUrl("http://opencaching.cz/viewcache.php?cacheid=610")).isEqualTo("OZ0262")
        assertThat(connector.getGeocodeFromUrl("http://www.opencaching.de/viewcache.php?cacheid=151223")).isNull()
    }

}
