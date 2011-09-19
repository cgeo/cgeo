/**
 *
 */
package cgeo.geocaching.utils;

/**
 * Misc. utils
 */
public final class BaseUtils {

    /**
     * Replace the characters \n, \r and \t with a space
     * 
     * @param buffer
     *            The data
     */
    public static void replaceWhitespace(final StringBuffer buffer) {
        final int length = buffer.length();
        final char[] chars = new char[length];
        buffer.getChars(0, length, chars, 0);
        int resultSize = 0;
        boolean lastWasWhitespace = false;
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
        buffer.setLength(0);
        buffer.append(chars);
    }

    public static String getMatch(String match) {
        // creating a new String via String constructor is necessary here!!
        return new String(match.trim());
        // Java copies the whole page String, when matching with regular expressions
        // later this would block the garbage collector, as we only need tiny parts of the page
        // see http://developer.android.com/reference/java/lang/String.html#backing_array
    
        // And BTW: You cannot even see that effect in the debugger, but must use a separate memory profiler!
    }

}
