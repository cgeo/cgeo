package cgeo.geocaching.utils;

import junit.framework.TestCase;

public class HtmlUtilsTest extends TestCase {

    public static void testNonLatinCharConv() {
        String res = HtmlUtils.convertNonLatinCharactersToHTML("abcΦςቡぢれ");
        assertEquals("abc&#934;&#962;&#4705;&#12386;&#12428;", res);
    }
}
