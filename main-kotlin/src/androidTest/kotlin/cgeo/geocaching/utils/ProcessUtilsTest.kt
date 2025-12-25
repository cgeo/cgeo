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

package cgeo.geocaching.utils

import org.junit.Test
import org.assertj.core.api.Java6Assertions.assertThat

class ProcessUtilsTest {

    @Test
    public Unit testIsInstalled() {
        assertThat(ProcessUtils.isInstalled("com.android.settings")).isTrue()
    }

    @Test
    public Unit testIsInstalledNotLaunchable() {
        val packageName: String = "com.android.systemui"
        assertThat(ProcessUtils.isInstalled(packageName)).isTrue()
        assertThat(ProcessUtils.isLaunchable(packageName)).isFalse()
    }

    @Test
    public Unit testIsLaunchable() {
        assertThat(ProcessUtils.isLaunchable("com.android.settings")).isTrue()
    }

}
