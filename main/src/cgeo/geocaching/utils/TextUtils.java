/**
 *
 */
package cgeo.geocaching.utils;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import org.eclipse.jdt.annotation.Nullable;

import java.nio.charset.Charset;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.CRC32;

/**
 * Misc. utils. All methods don't use Android specific stuff to use these methods in plain JUnit tests.
 */
public final class TextUtils {

    public static final Charset CHARSET_UTF8 = Charset.forName("UTF-8");
    public static final Charset CHARSET_ASCII = Charset.forName("US-ASCII");

    private static final Pattern PATTERN_REMOVE_NONPRINTABLE = Pattern.compile("\\p{Cntrl}");

    private TextUtils() {
        // utility class
    }

    /**
     * Searches for the pattern p in the data. If the pattern is not found defaultValue is returned
     *
     * @param data
     *            Data to search in
     * @param p
     *            Pattern to search for
     * @param trim
     *            Set to true if the group found should be trim'ed
     * @param group
     *            Number of the group to return if found
     * @param defaultValue
     *            Value to return if the pattern is not found
     * @param last
     *            Find the last occurring value
     * @return defaultValue or the n-th group if the pattern matches (trimmed if wanted)
     */
    @SuppressFBWarnings("DM_STRING_CTOR")
    public static String getMatch(@Nullable final String data, final Pattern p, final boolean trim, final int group, final String defaultValue, final boolean last) {
        if (data != null) {
            final Matcher matcher = p.matcher(data);
            if (matcher.find()) {
                String result = matcher.group(group);
                while (last && matcher.find()) {
                    result = matcher.group(group);
                }

                if (result != null) {
                    final Matcher remover = PATTERN_REMOVE_NONPRINTABLE.matcher(result);
                    result = remover.replaceAll(" ");

                    // Some versions of Java copy the whole page String, when matching with regular expressions
                    // later this would block the garbage collector, as we only need tiny parts of the page
                    // see http://developer.android.com/reference/java/lang/String.html#backing_array
                    // Thus the creating of a new String via String constructor is voluntary here!!
                    // And BTW: You cannot even see that effect in the debugger, but must use a separate memory profiler!
                    return trim ? new String(result).trim() : new String(result);
                }
            }
        }

        return defaultValue;
    }

    /**
     * Searches for the pattern p in the data. If the pattern is not found defaultValue is returned
     *
     * @param data
     *            Data to search in
     * @param p
     *            Pattern to search for
     * @param trim
     *            Set to true if the group found should be trim'ed
     * @param defaultValue
     *            Value to return if the pattern is not found
     * @return defaultValue or the first group if the pattern matches (trimmed if wanted)
     */
    public static String getMatch(final String data, final Pattern p, final boolean trim, final String defaultValue) {
        return TextUtils.getMatch(data, p, trim, 1, defaultValue, false);
    }

    /**
     * Searches for the pattern p in the data. If the pattern is not found defaultValue is returned
     *
     * @param data
     *            Data to search in
     * @param p
     *            Pattern to search for
     * @param defaultValue
     *            Value to return if the pattern is not found
     * @return defaultValue or the first group if the pattern matches (trimmed)
     */
    public static String getMatch(@Nullable final String data, final Pattern p, final String defaultValue) {
        return TextUtils.getMatch(data, p, true, 1, defaultValue, false);
    }

    /**
     * Searches for the pattern p in the data.
     *
     * @param data
     * @param p
     * @return true if data contains the pattern p
     */
    public static boolean matches(final String data, final Pattern p) {
        if (data == null) {
            return false;
        }
        // matcher is faster than String.contains() and more flexible - it takes patterns instead of fixed texts
        return p.matcher(data).find();

    }

    /**
     * Replaces every \n, \r and \t with a single space. Afterwards multiple spaces
     * are merged into a single space. Finally leading spaces are deleted.
     *
     * This method must be fast, but may not lead to the shortest replacement String.
     *
     * You are only allowed to change this code if you can prove it became faster on a device.
     * see cgeo.geocaching.test.WhiteSpaceTest#replaceWhitespaceManually in the test project.
     *
     * @param data
     *            complete HTML page
     * @return the HTML page as a very long single "line"
     */
    public static String replaceWhitespace(final String data) {
        final int length = data.length();
        final char[] chars = new char[length];
        data.getChars(0, length, chars, 0);
        int resultSize = 0;
        boolean lastWasWhitespace = true;
        for (final char c : chars) {
            if (c == ' ' || c == '\n' || c == '\r' || c == '\t') {
                if (!lastWasWhitespace) {
                    chars[resultSize++] = ' ';
                }
                lastWasWhitespace = true;
            } else {
                chars[resultSize++] = c;
                lastWasWhitespace = false;
            }
        }
        return String.valueOf(chars, 0, resultSize);
    }

    /**
     * Quick and naive check for possible rich HTML content in a string.
     *
     * @param str A string containing HTML code.
     * @return <tt>true</tt> if <tt>str</tt> contains HTML code that needs to go through a HTML renderer before
     *         being displayed, <tt>false</tt> if it can be displayed as-is without any loss
     */
    public static boolean containsHtml(final String str) {
        return str.indexOf('<') != -1 || str.indexOf('&') != -1;
    }

    /**
     * Remove all control characters (which are not valid in XML or HTML), as those should not appear in cache texts
     * anyway
     *
     * @param input
     * @return
     */
    public static String removeControlCharacters(final String input) {
        final Matcher remover = PATTERN_REMOVE_NONPRINTABLE.matcher(input);
        return remover.replaceAll(" ").trim();
    }

    /**
     * Calculate a simple checksum for change-checking (not usable for security/cryptography!)
     * 
     * @param input
     *            String to check
     * @return resulting checksum
     */
    public static long checksum(final String input) {
        final CRC32 checksum = new CRC32();
        checksum.update(input.getBytes());
        return checksum.getValue();
    }
}
