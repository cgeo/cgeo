/**
 *
 */
package cgeo.geocaching.utils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Misc. utils. All methods don't use Android specific stuff to use these methods in plain JUnit tests.
 */
public final class BaseUtils {

    private static final Pattern PATTERN_REMOVE_NONPRINTABLE = Pattern.compile("\\p{Cntrl}");

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
    public static String getMatch(final String data, final Pattern p, final boolean trim, final int group, final String defaultValue, final boolean last) {
        if (data != null) {

            String result = null;
            final Matcher matcher = p.matcher(data);

            if (matcher.find()) {
                result = matcher.group(group);
            }
            if (null != result) {
                Matcher remover = PATTERN_REMOVE_NONPRINTABLE.matcher(result);
                result = remover.replaceAll(" ");

                return trim ? new String(result).trim() : new String(result);
                // Java copies the whole page String, when matching with regular expressions
                // later this would block the garbage collector, as we only need tiny parts of the page
                // see http://developer.android.com/reference/java/lang/String.html#backing_array
                // Thus the creating of a new String via String constructor is necessary here!!

                // And BTW: You cannot even see that effect in the debugger, but must use a separate memory profiler!
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
        return BaseUtils.getMatch(data, p, trim, 1, defaultValue, false);
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
    public static String getMatch(final String data, final Pattern p, final String defaultValue) {
        return BaseUtils.getMatch(data, p, true, 1, defaultValue, false);
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
     * Replaces every \n, \r and \t with a single space. Afterwards multiples spaces
     * are merged into a single space. Finally leading spaces are deleted.
     *
     * This method must be fast, but may not lead to the shortest replacement String.
     *
     * You are only allowed to change this code if you can prove it became faster on a device.
     * see cgeo.geocaching.test.WhiteSpaceTest#replaceWhitespaceManually in the test project
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
        for (char c : chars) {
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
     * Quick and naive check for possible html-content of a string.
     *
     * @param str
     * @return True, if <code>str</code> could contain html
     */
    public static boolean containsHtml(final String str) {
        return str.indexOf('<') != -1 || str.indexOf('&') != -1;
    }

}
