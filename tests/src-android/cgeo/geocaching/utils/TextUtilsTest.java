package cgeo.geocaching.utils;

import cgeo.geocaching.connector.gc.GCConstants;

import android.text.SpannableString;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.regex.Pattern;

import junit.framework.TestCase;
import org.apache.commons.io.IOUtils;
import org.assertj.core.api.Assertions;
import static org.assertj.core.api.Java6Assertions.assertThat;


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
            Assertions.fail("cannot read cache page", e);
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

    public static void testGetTextBeforeIndexUntil() {
        final String testStr = "this is a test";
        final int aIdx = testStr.indexOf("a");
        assertThat(TextUtils.getTextBeforeIndexUntil(testStr, aIdx, "h")).isEqualTo("is is ");
        assertThat(TextUtils.getTextBeforeIndexUntil(testStr, aIdx, " ")).isEqualTo("");
        assertThat(TextUtils.getTextBeforeIndexUntil(testStr, aIdx, "a")).isEqualTo("this is ");
        assertThat(TextUtils.getTextBeforeIndexUntil(testStr, aIdx, "his")).isEqualTo(" is ");
        assertThat(TextUtils.getTextBeforeIndexUntil("a", 0, "a")).isEqualTo("");
        assertThat(TextUtils.getTextBeforeIndexUntil(testStr, testStr.length(), "a")).isEqualTo(" test");
        assertThat(TextUtils.getTextBeforeIndexUntil(testStr, -1, "a")).isEqualTo("");
        assertThat(TextUtils.getTextBeforeIndexUntil(testStr, testStr.length(), "nonexisting")).isEqualTo(testStr);

        assertThat(TextUtils.getTextBeforeIndexUntil(testStr, aIdx, "t")).isEqualTo("his is ");
        assertThat(TextUtils.getTextBeforeIndexUntil(testStr, aIdx, "t", 6)).isEqualTo("is is ");

    }

    public static void testGetTextAfterIndexUntilDelimiter() {
        final String testStr = "this is a test";
        final int aIdx = testStr.indexOf("a");
        assertThat(TextUtils.getTextAfterIndexUntil(testStr, aIdx, "s")).isEqualTo(" te");
        assertThat(TextUtils.getTextAfterIndexUntil(testStr, aIdx, " ")).isEqualTo("");
        assertThat(TextUtils.getTextAfterIndexUntil(testStr, aIdx, "a")).isEqualTo(" test");
        assertThat(TextUtils.getTextAfterIndexUntil(testStr, aIdx, "st")).isEqualTo(" te");
        assertThat(TextUtils.getTextAfterIndexUntil("a", 0, "a")).isEqualTo("");
        assertThat(TextUtils.getTextAfterIndexUntil(testStr, testStr.length(), "a")).isEqualTo("");
        assertThat(TextUtils.getTextAfterIndexUntil(testStr, -1, "a")).isEqualTo("this is ");
        assertThat(TextUtils.getTextAfterIndexUntil(testStr, -1, "nonexisting")).isEqualTo(testStr);

        assertThat(TextUtils.getTextAfterIndexUntil(testStr, aIdx, "st")).isEqualTo(" te");
        assertThat(TextUtils.getTextAfterIndexUntil(testStr, aIdx, "st", 2)).isEqualTo(" t");

    }

    public static void testGetNextDelimValue() {
        assertThat(TextUtils.parseNextDelimitedValue("before \"soon it is \\\"christmas\\\\holiday\\\" again\" after ", '"', '\\'))
                .isEqualTo("soon it is \"christmas\\holiday\" again");
        //test symbols with special meaning in regerx
        assertThat(TextUtils.parseNextDelimitedValue("before *soon it is $*christmas$$holiday$* again* after ", '*', '$'))
                .isEqualTo("soon it is *christmas$holiday* again");
        //symbol and escape char are the same
        assertThat(TextUtils.parseNextDelimitedValue("before 'soon it is ''christmas'' again' after ", '\'', '\''))
                .isEqualTo("soon it is 'christmas' again");
        //other chars are escaped
        assertThat(TextUtils.parseNextDelimitedValue("before 'soon it \\is \\'christmas\\' \\again' after ", '\'', '\\'))
                .isEqualTo("soon it is 'christmas' again");
        //newline is used
        assertThat(TextUtils.parseNextDelimitedValue("before\n \"soon it\n is \\\"christ\nmas\\\\holiday\\\" again\" \nafter ", '"', '\\'))
                .isEqualTo("soon it\n is \"christ\nmas\\holiday\" again");

        //stable for unclosed value
        assertThat(TextUtils.parseNextDelimitedValue("before *soon it is \\*christmas\\* again after", '*', '\\'))
                .isEqualTo(null);
        //stable with empty values
        assertThat(TextUtils.parseNextDelimitedValue("", ' ', ' '))
                .isEqualTo(null);

    }

    public static void testGetDelimitedValue() {
        final String test = "soon it is 'christmas' again";
        final String expectedDelim = "'soon it is \\'christmas\\' again'";
        assertThat(TextUtils.createDelimitedValue(test, '\'', '\\')).isEqualTo(expectedDelim);
        assertThat(TextUtils.parseNextDelimitedValue(TextUtils.createDelimitedValue(test, '\'', '\\'), '\'', '\\')).isEqualTo(test);
    }

    public static void testGetWords() {
        assertThat(TextUtils.getWords("this is a test").length).isEqualTo(4);
        assertThat(TextUtils.getWords("this is a test")[0]).isEqualTo("this");
        assertThat(TextUtils.getWords(" this is a test")[0]).isEqualTo("this");
        assertThat(TextUtils.getWords("\n\t  \n\t this\tis a test")[0]).isEqualTo("this");
        assertThat(TextUtils.getWords("this is a \ntest\t\r")[3]).isEqualTo("test");
        assertThat(TextUtils.getWords("").length).isEqualTo(0);
        assertThat(TextUtils.getWords(" \n\r\t").length).isEqualTo(0);
        assertThat(TextUtils.getWords(null).length).isEqualTo(0);
    }

    public static void testGetAll() {
        assertThatListIsEqual(TextUtils.getAll("this is a test", "t", "s"), "hi", "e");
        assertThatListIsEqual(TextUtils.getAll("this is a test t", "t", "t"), "his is a ", " ");
        assertThatListIsEqual(TextUtils.getAll("this is a test", "t", "t"), "his is a ");
        assertThatListIsEqual(TextUtils.getAll("this is a test", "this", "test"), " is a ");
        assertThatListIsEqual(TextUtils.getAll("this* is $a test", "*", "$"), " is ");
        assertThatListIsEqual(TextUtils.getAll("this is a test", "", ""), "this is a test");
        assertThatListIsEqual(TextUtils.getAll("this is a test", "", "a"), "this is ");
        assertThatListIsEqual(TextUtils.getAll("this is a test", "a", ""), " test");

        assertThatListIsEqual(TextUtils.getAll("th\nis is a test", "t", "s"), "h\ni", "e");

        assertThatListIsEqual(TextUtils.getAll("", "a", "b"));
        assertThatListIsEqual(TextUtils.getAll(null, "a", "b"));

    }

    private static <T> void assertThatListIsEqual(final List<T> list, final T... expected) {
        assertThat(list.size()).isEqualTo(expected.length);
        int cnt = 0;
        for (final T e : list) {
            assertThat(e).isEqualTo(expected[cnt++]);
        }
    }


    public static void testReplaceAll() {
        assertThat(TextUtils.replaceAll("this is a test", "t", "s", "")).isEqualTo(" is a t");
        assertThat(TextUtils.replaceAll("this is a test", "this", "tes", "")).isEqualTo("t");
        assertThat(TextUtils.replaceAll("this is a test", "this", "test", "")).isEqualTo("");
        assertThat(TextUtils.replaceAll("this is a test", "t", "s", "$1")).isEqualTo("hi is a et");
        assertThat(TextUtils.replaceAll("this* is $a test", "*", "$", "")).isEqualTo("thisa test");
        assertThat(TextUtils.replaceAll("this is a test", "", "", "")).isEqualTo("");
        assertThat(TextUtils.replaceAll("this is a test", "", "a", "")).isEqualTo(" test");
        assertThat(TextUtils.replaceAll("this is a test", "a", "", "")).isEqualTo("this is ");
        assertThat(TextUtils.replaceAll("", "a", "b", "")).isEqualTo("");
        assertThat(TextUtils.replaceAll(null, "a", "b", "")).isEqualTo("");

        assertThat(TextUtils.replaceAll("before <@@@@@> middle </@@@@@> after", "<@@@@@>", "</@@@@@>", "")).isEqualTo("before  after");

    }
}
