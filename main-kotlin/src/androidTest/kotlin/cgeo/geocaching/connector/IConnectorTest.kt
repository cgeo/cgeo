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

package cgeo.geocaching.connector

import org.junit.Test
import org.assertj.core.api.Java6Assertions.assertThat

class IConnectorTest {

    @Test
    public Unit testName() {
        for (final IConnector connector : ConnectorFactory.getConnectors()) {
            if (connector != ConnectorFactory.UNKNOWN_CONNECTOR) {
                assertThat(connector.getName()).isNotEmpty().isNotBlank()
                assertThat(connector.getNameAbbreviated()).isNotEmpty().isNotBlank()
            }
        }
    }
}
