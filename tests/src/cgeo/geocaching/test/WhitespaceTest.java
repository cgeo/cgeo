package cgeo.geocaching.test;

import cgeo.geocaching.cgSettings;

import org.apache.commons.lang3.StringUtils;

import android.test.AndroidTestCase;
import android.util.Log;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * This test is meant for performance measurements of different whitespace replacement implementations.
 * It does not test semantical correctness.
 *
 */
public class WhitespaceTest extends AndroidTestCase {

    private String data;

    @Override
    protected void setUp() throws Exception {
        final StringBuffer buffer = new StringBuffer();
        final InputStream is = this.getClass().getResourceAsStream("/cgeo/geocaching/test/mock/GC2CJPF.html");
        final BufferedReader br = new BufferedReader(new InputStreamReader(is));

        String line = null;

        while ((line = br.readLine()) != null) {
            buffer.append(line).append('\n');
        }
        data = buffer.toString();

        br.close();
    }

    public static String replaceWhitespaceManually(final String data) {
        final int length = data.length();
        final char[] chars = new char[length];
        data.getChars(0, length, chars, 0);
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
        return String.valueOf(chars, 0, resultSize);
    }

    public static String replaceWhitespaceStringUtils(final String data) {
        return StringUtils.join(StringUtils.split(data, " \n\r\t"), " ");
    }

    public void testManually() {
        String result;
        final long start = System.currentTimeMillis();
        result = replaceWhitespaceManually(data);
        final long end = System.currentTimeMillis();
        assertEquals(84028, result.length());
        Log.w(cgSettings.tag, (end - start) + " ms manually");
    }

    public void testStringUtils() {
        String result;
        final long start = System.currentTimeMillis();
        result = replaceWhitespaceStringUtils(data);
        final long end = System.currentTimeMillis();
        assertEquals(84026, result.length());
        Log.w(cgSettings.tag, (end - start) + " ms StringUtils");
    }
}
