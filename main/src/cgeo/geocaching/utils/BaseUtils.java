/**
 *
 */
package cgeo.geocaching.utils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Misc. utils
 */
public final class BaseUtils {

    /**
     * Searches for the pattern p in the data for the n-th group. If the pattern
     * is not found defaultValue is returned
     *
     * @param data
     * @param p
     * @param group
     * @param defaultValue
     * @return
     */
    public static String getMatch(final String data, final Pattern p, final int group, final String defaultValue) {
        final Matcher matcher = p.matcher(data);
        if (matcher.find() && matcher.groupCount() >= group) {
            // creating a new String via String constructor is necessary here!!
            return new String(matcher.group(group).trim());
            // Java copies the whole page String, when matching with regular expressions
            // later this would block the garbage collector, as we only need tiny parts of the page
            // see http://developer.android.com/reference/java/lang/String.html#backing_array

            // And BTW: You cannot even see that effect in the debugger, but must use a separate memory profiler!
        }
        return defaultValue;
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

}
