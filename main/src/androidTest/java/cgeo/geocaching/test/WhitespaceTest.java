package cgeo.geocaching.test;

import cgeo.geocaching.utils.Log;
import cgeo.geocaching.utils.TextUtils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.junit.Before;
import org.junit.Test;
import static org.assertj.core.api.Java6Assertions.assertThat;

/**
 * This test is meant for performance measurements of different whitespace replacement implementations.
 * It does not test semantical correctness.
 */
public class WhitespaceTest {

    private static final int EXPECTED_SIZE = 122476;
    private String data;

    @Before
    public void setUp() throws Exception {
        data = CgeoTestUtils.getFileContent(R.raw.gc2cjpf_html);
    }

    /**
     * The place for the implementation to prove that the new version of replaceWhitespace is faster than
     * BaseUtils.replaceWhitespace()
     */
    public static String replaceWhitespaceManually(final String data) {
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

    public static String replaceWhitespaceStringUtils(final String data) {
        return StringUtils.join(StringUtils.split(data, " \n\r\t"), " ");
    }

    @Test
    public void testRegex() {
        final Pattern pattern = Pattern.compile("\\s+");
        final long start = System.currentTimeMillis();
        final Matcher matcher = pattern.matcher(data);
        final String result = matcher.replaceAll(" ").trim();
        final long end = System.currentTimeMillis();
        assertThat(result.length()).isEqualTo(EXPECTED_SIZE - 1);
        Log.d((end - start) + " ms regex");
    }

    @Test
    public void testReplaceAll() {
        final long start = System.currentTimeMillis();
        final String result = data.replaceAll("\\s+", " ");
        final long end = System.currentTimeMillis();
        assertThat(result.length()).isEqualTo(EXPECTED_SIZE + 1);
        Log.d((end - start) + " ms replaceAll");
    }

    @Test
    public void testActualImplementation() {
        final String result;
        final long start = System.currentTimeMillis();
        result = TextUtils.replaceWhitespace(data);
        final long end = System.currentTimeMillis();
        assertThat(result.length()).isEqualTo(EXPECTED_SIZE);
        Log.d((end - start) + " ms actual implementation");
    }

    @Test
    public void testManually() {
        final String result;
        final long start = System.currentTimeMillis();
        result = replaceWhitespaceManually(data);
        final long end = System.currentTimeMillis();
        assertThat(result.length()).isEqualTo(EXPECTED_SIZE);
        Log.d((end - start) + " ms manually");
    }

    @Test
    public void testStringUtils() {
        final String result;
        final long start = System.currentTimeMillis();
        result = replaceWhitespaceStringUtils(data);
        final long end = System.currentTimeMillis();
        assertThat(result.length()).isEqualTo(EXPECTED_SIZE - 1);
        Log.d((end - start) + " ms StringUtils");
    }
}
