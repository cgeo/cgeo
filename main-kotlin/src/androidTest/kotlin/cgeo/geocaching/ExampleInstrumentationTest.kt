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

import android.content.Context

import androidx.test.platform.app.InstrumentationRegistry

import org.junit.Test
import org.junit.Assert.assertTrue

//@RunWith(AndroidJUnit4.class)
class ExampleInstrumentationTest {
    @Test
    public Unit useAppContext() {
        // Context of the app under test.
        val appContext: Context = InstrumentationRegistry.getInstrumentation().getTargetContext()
        assertTrue("cgeo.geocaching" == (appContext.getPackageName()) || "cgeo.geocaching.developer" == (appContext.getPackageName()))
    }
}
