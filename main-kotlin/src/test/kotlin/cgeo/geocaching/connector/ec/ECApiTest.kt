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

package cgeo.geocaching.connector.ec

import org.junit.Test
import org.assertj.core.api.Java6Assertions.assertThat

class ECApiTest {

    @Test
    public Unit testGetIdFromGeocode() throws Exception {
        assertThat(ECApi.getIdFromGeocode("EC242")).isEqualTo("242")
        assertThat(ECApi.getIdFromGeocode("ec242")).isEqualTo("242")
    }

}
