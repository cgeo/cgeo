package cgeo.geocaching.utils;

import cgeo.geocaching.connector.gc.GCConstants;
import cgeo.geocaching.connector.gc.GCConstantsTest;
import cgeo.geocaching.test.mock.MockedCache;

import android.test.AndroidTestCase;

import java.util.regex.Pattern;

public class BaseUtilsTest extends AndroidTestCase {
    public static void testRegEx() {
        final String page = MockedCache.readCachePage("GC2CJPF");
        assertEquals(GCConstantsTest.MOCK_LOGIN_NAME, BaseUtils.getMatch(page, GCConstants.PATTERN_LOGIN_NAME, true, "???"));
        assertTrue(page.contains("id=\"ctl00_hlRenew\"") || GCConstants.MEMBER_STATUS_PM.equals(BaseUtils.getMatch(page, GCConstants.PATTERN_MEMBER_STATUS, true, "???")));
    }

    public static void testReplaceWhitespaces() {
        assertEquals("foo bar baz ", BaseUtils.replaceWhitespace("  foo\n\tbar   \r   baz  "));
    }

    public static void testControlCharactersCleanup() {
        final Pattern patternAll = Pattern.compile("(.*)", Pattern.DOTALL);
        assertEquals("some control characters removed", BaseUtils.getMatch("some" + "\u001C" + "control" + (char) 0x1D + "characters removed", patternAll, ""));
        assertEquals("newline also removed", BaseUtils.getMatch("newline\nalso\nremoved", patternAll, ""));
    }
}
