package cgeo.geocaching.utils;

import static org.assertj.core.api.Assertions.assertThat;

import cgeo.geocaching.connector.gc.GCConstants;
import cgeo.geocaching.connector.gc.GCConstantsTest;
import cgeo.geocaching.test.mock.MockedCache;

import android.test.AndroidTestCase;

import java.util.regex.Pattern;

public class TextUtilsTest extends AndroidTestCase {
    public static void testRegEx() {
        final String page = MockedCache.readCachePage("GC2CJPF");
        assertThat(TextUtils.getMatch(page, GCConstants.PATTERN_LOGIN_NAME, true, "???")).isEqualTo(GCConstantsTest.MOCK_LOGIN_NAME);
        assertThat(page.contains("id=\"ctl00_hlRenew\"") || GCConstants.MEMBER_STATUS_PREMIUM.equals(TextUtils.getMatch(page, GCConstants.PATTERN_MEMBER_STATUS, true, "???"))).isTrue();
    }

    public static void testReplaceWhitespaces() {
        assertThat(TextUtils.replaceWhitespace("  foo\n\tbar   \r   baz  ")).isEqualTo("foo bar baz ");
    }

    public static void testControlCharactersCleanup() {
        final Pattern patternAll = Pattern.compile("(.*)", Pattern.DOTALL);
        assertThat(TextUtils.getMatch("some" + "\u001C" + "control" + (char) 0x1D + "characters removed", patternAll, "")).isEqualTo("some control characters removed");
        assertThat(TextUtils.getMatch("newline\nalso\nremoved", patternAll, "")).isEqualTo("newline also removed");
    }

    public static void testGetMatch() {
        final Pattern patternAll = Pattern.compile("foo(...)");
        final String text = "abc-foobar-def-fooxyz-ghi-foobaz-jkl";
        assertThat(TextUtils.getMatch(text, patternAll, false, 1, null, false)).isEqualTo("bar");
        assertThat(TextUtils.getMatch(text, patternAll, false, 1, null, true)).isEqualTo("baz");
    }
}
