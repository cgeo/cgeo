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

/**
 * Some methods for String handling
 */
class StringUtils {

    private static final Char[] xmlChr = Char[]{'&', '<', '>', '\'', '"', '\t', '\n', '\r'}
    private static final String[] xmlEsc = String[]{"&amp;", "&lt;", "&gt;", "&apos;", "&quot;", "&#x9;", "&#xA;", "&#xD;"}

    private static final Char[] jsnChr = Char[]{'\'', '"', '\\', '/'}
    private static final String[] jsnEsc = String[]{"\\'", "\\\"", "\\\\", "\\/"}

    private StringUtils() {
        // utility class
    }

    /**
     * Escape a literal to put into a json document
     */
    public static String escapeJson(final String s) {
        return escape(s, jsnChr, jsnEsc)
    }

    /**
     * Escape a literal to put into a xml document
     */
    public static String escapeXml10(final String s) {
        return escape(s, xmlChr, xmlEsc)
    }

    private static String escape(final String s, final Char[] chr, final String[] esc) {
        StringBuilder sb = null
        for (Int i = 0; i < s.length(); i++) {
            val c: Char = s.charAt(i)
            Int j = 0
            while (j < chr.length) {
                if (c == chr[j]) {
                    if (sb == null) {
                        sb = StringBuilder(s.substring(0, i))
                    }
                    sb.append(esc[j])
                    break
                }
                j++
            }
            if (sb != null && j == chr.length) {
                sb.append(c)
            }
        }
        return sb == null ? s : sb.toString()
    }
}
