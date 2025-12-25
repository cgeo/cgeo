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

package cgeo.geocaching.connector.su

import org.junit.Test
import org.assertj.core.api.Java6Assertions.assertThat

class SuConnectorTest {

    @Test
    public Unit testCanHandle() {
        val connector: SuConnector = SuConnector.getInstance()
        assertThat(connector.canHandle("TR12")).isTrue()
        assertThat(connector.canHandle("VI12")).isTrue()
        assertThat(connector.canHandle("MS32113")).isTrue()
        assertThat(connector.canHandle("MV32113")).isTrue()
        assertThat(connector.canHandle("LT421")).isTrue()
        assertThat(connector.canHandle("LV421")).isTrue()
    }

    @Test
    public Unit testCanHandleSU() {
        val connector: SuConnector = SuConnector.getInstance()
        assertThat(connector.canHandle("SU12")).isTrue()
    }

    @Test
    public Unit testCanNotHandle() {
        val connector: SuConnector = SuConnector.getInstance()
        assertThat(connector.canHandle("GC12")).isFalse()
        assertThat(connector.canHandle("OC412")).isFalse()
    }

}
