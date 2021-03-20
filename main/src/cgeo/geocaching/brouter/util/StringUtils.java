package cgeo.geocaching.brouter.util;

/**
 * Some methods for String handling
 */
public class StringUtils {

    private static final char[] xmlChr = new char[]{'&', '<', '>', '\'', '"', '\t', '\n', '\r'};
    private static final String[] xmlEsc = new String[]{"&amp;", "&lt;", "&gt;", "&apos;", "&quot;", "&#x9;", "&#xA;", "&#xD;"};

    private static final char[] jsnChr = new char[]{'\'', '"', '\\', '/'};
    private static final String[] jsnEsc = new String[]{"\\'", "\\\"", "\\\\", "\\/"};

    private StringUtils() {
        // utility class
    }

    /**
     * Escape a literal to put into a json document
     */
    public static String escapeJson(final String s) {
        return escape(s, jsnChr, jsnEsc);
    }

    /**
     * Escape a literal to put into a xml document
     */
    public static String escapeXml10(final String s) {
        return escape(s, xmlChr, xmlEsc);
    }

    private static String escape(final String s, final char[] chr, final String[] esc) {
        StringBuilder sb = null;
        for (int i = 0; i < s.length(); i++) {
            final char c = s.charAt(i);
            int j = 0;
            while (j < chr.length) {
                if (c == chr[j]) {
                    if (sb == null) {
                        sb = new StringBuilder(s.substring(0, i));
                    }
                    sb.append(esc[j]);
                    break;
                }
                j++;
            }
            if (sb != null && j == chr.length) {
                sb.append(c);
            }
        }
        return sb == null ? s : sb.toString();
    }
}
