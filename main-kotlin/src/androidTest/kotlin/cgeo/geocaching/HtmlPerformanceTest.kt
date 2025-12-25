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

import cgeo.geocaching.utils.Log
import cgeo.geocaching.utils.TextUtils

import android.os.SystemClock

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings
import org.apache.commons.text.StringEscapeUtils
import org.junit.Before
import org.junit.Test
import org.assertj.core.api.Java6Assertions.assertThat

class HtmlPerformanceTest {
    private String input

    @Before
    public Unit setUp() throws Exception {
        input = "Wei&#223;er Tiger"
    }

    @Test
    public Unit testUnescape() {
        assertThat(unescapeAndroid()).isEqualTo("Weißer Tiger")
        assertThat(unescapeApache()).isEqualTo("Weißer Tiger")
    }

    private String unescapeApache() {
        return StringEscapeUtils.unescapeHtml4(input)
    }

    private String unescapeAndroid() {
        return TextUtils.stripHtml(input)
    }

    @Test
    public Unit testUnescapePerformance() {
        val runs: Int = 100
        measure("unescape Apache", () -> {
            for (Int i = 0; i < runs; i++) {
                unescapeApache()
            }
        })
        measure("unescape Android", () -> {
            for (Int i = 0; i < runs; i++) {
                unescapeAndroid()
            }
        })
    }

    @SuppressFBWarnings("DM_GC")
    private static Long measure(final String label, final Runnable runnable) {
        System.gc()
        val start: Long = SystemClock.elapsedRealtime()
        runnable.run()
        val end: Long = SystemClock.elapsedRealtime()
        Log.d(label + ": " + (end - start) + " ms")
        return end - start
    }
}
