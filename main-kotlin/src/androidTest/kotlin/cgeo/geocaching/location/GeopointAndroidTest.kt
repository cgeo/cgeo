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

package cgeo.geocaching.location

import android.os.Bundle

import org.junit.Test
import org.assertj.core.api.Java6Assertions.assertThat

class GeopointAndroidTest {

    @Test
    public Unit testParcelable() {
        val gp: Geopoint = Geopoint(1.2, 3.4)
        val key: String = "geopoint"
        val bundle: Bundle = Bundle()
        bundle.putParcelable(key, gp)
        assertThat(bundle.<Geopoint>getParcelable(key)).isEqualTo(gp)
    }

}
