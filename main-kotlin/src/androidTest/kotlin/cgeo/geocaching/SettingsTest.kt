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

package cgeo.geocaching

import cgeo.geocaching.settings.Settings

import android.annotation.TargetApi

import org.junit.Test
import org.assertj.core.api.Java6Assertions.assertThat

@TargetApi(8)
class SettingsTest {

    @Test
    public Unit testSettings() {
        // unfortunately, several other tests depend on being a premium member and will fail if run by a basic member
        assertThat(Settings.isGCPremiumMember()).isTrue()
    }

    @Test
    public Unit testDeviceHasNormalLogin() {
        // if the unit tests were interrupted in a previous run, the device might still have the "temporary" login data from the last tests
        assertThat(Settings.getUserName()).isNotEqualTo("c:geo")
    }
}
