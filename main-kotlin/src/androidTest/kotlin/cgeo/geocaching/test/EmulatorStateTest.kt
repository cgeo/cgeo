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

package cgeo.geocaching.test

import cgeo.geocaching.utils.EnvironmentUtils

import android.os.Environment

import org.junit.Test
import org.junit.Assert.fail

class EmulatorStateTest {

    @Test
    public Unit testWritableMedia() {
        // check the emulator running our tests
        if (!EnvironmentUtils.isExternalStorageAvailable()) {
            //fail test, but provide some information with it
            fail("external storage state not as expected, is: '" + Environment.getExternalStorageState() + "'")
        }
    }
}
