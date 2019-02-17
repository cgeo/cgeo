package cgeo.geocaching.utils;

import static org.assertj.core.api.Java6Assertions.assertThat;

import cgeo.geocaching.connector.gc.GCConstants;

import junit.framework.Assert;
import junit.framework.TestCase;
import org.apache.commons.io.IOUtils;

import android.text.SpannableString;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.regex.Pattern;

public class TextUtilsTest extends TestCase {

    private static String readCachePage(final String geocode) {
        InputStream is = null;
        BufferedReader br = null;
        try {
            is = TextUtilsTest.class.getResourceAsStream("/cgeo/geocaching/test/mock/" + geocode + ".html");
            br = new BufferedReader(new InputStreamReader(is), 150000);

            final StringBuilder buffer = new StringBuilder();
            String line;

            while ((line = br.readLine()) != null) {
                buffer.append(line).append('\n');
            }

            return TextUtils.replaceWhitespace(buffer.toString());
        } catch (final IOException e) {
            Assert.fail(e.getMessage());
        } finally {
            IOUtils.closeQuietly(is);
            IOUtils.closeQuietly(br);
        }
        return null;
    }


    public static void testRegEx() {
        final String page = readCachePage("GC2CJPF");
        assertThat(TextUtils.getMatch(page, GCConstants.PATTERN_LOGIN_NAME, true, "???")).isEqualTo("ra_sch");
    }

    public static void testReplaceWhitespaces() {
        assertThat(TextUtils.replaceWhitespace("  foo\n\tbar   \r   baz  ")).isEqualTo("foo bar baz ");
    }

    public static void testControlCharactersCleanup() {
        final Pattern patternAll = Pattern.compile("(.*)", Pattern.DOTALL);
        assertThat(TextUtils.getMatch("some" + '\u001C' + "control" + (char) 0x1D + "characters removed", patternAll, "")).isEqualTo("some control characters removed");
        assertThat(TextUtils.getMatch("newline\nalso\nremoved", patternAll, "")).isEqualTo("newline also removed");
    }

    public static void testGetMatch() {
        final Pattern patternAll = Pattern.compile("foo(...)");
        final String text = "abc-foobar-def-fooxyz-ghi-foobaz-jkl";
        assertThat(TextUtils.getMatch(text, patternAll, false, 1, null, false)).isEqualTo("bar");
        assertThat(TextUtils.getMatch(text, patternAll, false, 1, null, true)).isEqualTo("baz");
    }

    public static void testTrimSpanned() {
        assertTrimSpanned(" ", "");
        assertTrimSpanned("\n", "");
        assertTrimSpanned("a ", "a");
        assertTrimSpanned("a\n", "a");
    }

    private static void assertTrimSpanned(final String input, final String expected) {
        assertThat(TextUtils.trimSpanned(new SpannableString(input)).toString()).isEqualTo(new SpannableString(expected).toString());
    }

    public static void testStripHtml() {
        assertThat(TextUtils.stripHtml("foo bar")).isEqualTo("foo bar");
        assertThat(TextUtils.stripHtml("<div><span>foo</span> bar</div>")).isEqualTo("foo bar");
    }
}
