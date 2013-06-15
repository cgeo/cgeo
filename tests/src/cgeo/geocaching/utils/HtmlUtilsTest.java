package cgeo.geocaching.utils;

import org.apache.commons.lang3.StringUtils;

import junit.framework.TestCase;

public class HtmlUtilsTest extends TestCase {

    public static void testExtractText() {
        assertEquals(StringUtils.EMPTY, HtmlUtils.extractText(null));
        assertEquals(StringUtils.EMPTY, HtmlUtils.extractText(StringUtils.EMPTY));
        assertEquals(StringUtils.EMPTY, HtmlUtils.extractText("   "));
    }

}
