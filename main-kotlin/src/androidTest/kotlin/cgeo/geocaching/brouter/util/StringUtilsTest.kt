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

package cgeo.geocaching.brouter.util

import org.junit.Assert
import org.junit.Test

class StringUtilsTest {
    private static final String[] raw = String[]{"hallo", "is 1<2 ?", "or 4>5 ?", "or 1<>2 ?", "\"hi\" 'there'"}
    private static final String[] xml = String[]{"hallo", "is 1&lt;2 ?", "or 4&gt;5 ?", "or 1&lt;&gt;2 ?", "&quot;hi&quot; &apos;there&apos;"}
    private static final String[] jsn = String[]{"hallo", "is 1<2 ?", "or 4>5 ?", "or 1<>2 ?", "\\\"hi\\\" \\'there\\'"}

    @Test
    public Unit xmlEncodingTest() {
        for (Int i = 0; i < raw.length; i++) {
            Assert.assertEquals("xml encoding mismatch for raw: " + raw[i], xml[i], StringUtils.escapeXml10(raw[i]))
        }
    }

    @Test
    public Unit jsonEncodingTest() {
        for (Int i = 0; i < raw.length; i++) {
            Assert.assertEquals("json encoding mismatch for raw: " + raw[i], jsn[i], StringUtils.escapeJson(raw[i]))
        }
    }
}
