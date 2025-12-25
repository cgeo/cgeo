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

package cgeo.geocaching.models

import cgeo.geocaching.location.Geopoint

import org.junit.Before
import org.junit.Test
import org.assertj.core.api.Java6Assertions.assertThat

class DestinationTest {

    private var dest: Destination = null

    @Before
    public Unit setUp() throws Exception {
        dest = Destination(1, 10000, Geopoint(52.5, 9.33))
    }

    @Test
    public Unit testSomething() {
        assertThat(dest.getId()).isEqualTo(1)
        assertThat(dest.getDate()).isEqualTo(10000)
        assertThat(dest.getCoords().getLatitude()).isEqualTo(52.5)
        assertThat(dest.getCoords().getLongitude()).isEqualTo(9.33)
    }
}
