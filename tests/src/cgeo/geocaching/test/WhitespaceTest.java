package cgeo.geocaching.test;


import cgeo.geocaching.utils.BaseUtils;
import cgeo.geocaching.utils.Log;

import org.apache.commons.lang3.StringUtils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This test is meant for performance measurements of different whitespace replacement implementations.
 * It does not test semantical correctness.
 *
 */
public class WhitespaceTest extends AbstractResourceInstrumentationTestCase {

    private final static int EXPECTED_SIZE = 122907;
    private String data;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        data = getFileContent(R.raw.gc2cjpf_html);
    }

    /**
     * The place for the implementation to prove that the new version of replaceWhitespace is faster than
     * BaseUtils.replaceWhitespace()
     *
     * @param data
     * @return
     */
    public static String replaceWhitespaceManually(final String data) {
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

    public static String replaceWhitespaceStringUtils(final String data) {
        return StringUtils.join(StringUtils.split(data, " \n\r\t"), " ");
    }

    public void testRegex() {
        Pattern pattern = Pattern.compile("\\s+");
        final long start = System.currentTimeMillis();
        Matcher matcher = pattern.matcher(data);
        String result = matcher.replaceAll(" ").trim();
        final long end = System.currentTimeMillis();
        assertEquals(EXPECTED_SIZE - 1, result.length());
        Log.d((end - start) + " ms regex");
    }

    public void testReplaceAll() {
        final long start = System.currentTimeMillis();
        String result = data.replaceAll("\\s+", " ");
        final long end = System.currentTimeMillis();
        assertEquals(EXPECTED_SIZE + 1, result.length());
        Log.d((end - start) + " ms replaceAll");
    }

    public void testActualImplementation() {
        String result;
        final long start = System.currentTimeMillis();
        result = BaseUtils.replaceWhitespace(data);
        final long end = System.currentTimeMillis();
        assertEquals(EXPECTED_SIZE, result.length());
        Log.d((end - start) + " ms actual implementation");
    }

    public void testManually() {
        String result;
        final long start = System.currentTimeMillis();
        result = replaceWhitespaceManually(data);
        final long end = System.currentTimeMillis();
        assertEquals(EXPECTED_SIZE, result.length());
        Log.d((end - start) + " ms manually");
    }

    public void testStringUtils() {
        String result;
        final long start = System.currentTimeMillis();
        result = replaceWhitespaceStringUtils(data);
        final long end = System.currentTimeMillis();
        assertEquals(EXPECTED_SIZE - 1, result.length());
        Log.d((end - start) + " ms StringUtils");
    }
}