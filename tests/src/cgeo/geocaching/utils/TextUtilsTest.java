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
        assertEquals(GCConstantsTest.MOCK_LOGIN_NAME, TextUtils.getMatch(page, GCConstants.PATTERN_LOGIN_NAME, true, "???"));
        assertThat(page.contains("id=\"ctl00_hlRenew\"") || GCConstants.MEMBER_STATUS_PM.equals(TextUtils.getMatch(page, GCConstants.PATTERN_MEMBER_STATUS, true, "???"))).isTrue();
    }

    public static void testReplaceWhitespaces() {
        assertThat(TextUtils.replaceWhitespace("  foo\n\tbar   \r   baz  ")).isEqualTo("foo bar baz ");
    }

    public static void testControlCharactersCleanup() {
        final Pattern patternAll = Pattern.compile("(.*)", Pattern.DOTALL);
        assertEquals("some control characters removed", TextUtils.getMatch("some" + "\u001C" + "control" + (char) 0x1D + "characters removed", patternAll, ""));
        assertEquals("newline also removed", TextUtils.getMatch("newline\nalso\nremoved", patternAll, ""));
    }
}
