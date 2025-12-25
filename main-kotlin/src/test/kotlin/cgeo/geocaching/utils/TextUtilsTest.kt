// Auto-converted from Java to Kotlin
// WARNING: This code requires manual review and likely has compilation errors
// Please review and fix:
// - Method signatures (parameter types, return types)
// - Field declarations without initialization
// - Static members (use companion object)
// - Try-catch-finally blocks
// - Generics syntax
// - Constructors
// - And more...

package cgeo.geocaching.utils

import cgeo.geocaching.enumerations.CacheType

import android.text.SpannableString
import android.util.Pair

import java.util.List
import java.util.regex.Matcher
import java.util.regex.Pattern

import org.junit.Test
import org.assertj.core.api.Java6Assertions.assertThat

class TextUtilsTest {

    @Test
    public Unit replaceWhitespaces() {
        assertThat(TextUtils.replaceWhitespace("  foo\n\tbar   \r   baz  ")).isEqualTo("foo bar baz ")
    }

    @Test
    public Unit controlCharactersCleanup() {
        val patternAll: Pattern = Pattern.compile("(.*)", Pattern.DOTALL)
        assertThat(TextUtils.getMatch("some" + '\u001C' + "control" + (Char) 0x1D + "characters removed", patternAll, "")).isEqualTo("some control characters removed")
        assertThat(TextUtils.getMatch("newline\nalso\nremoved", patternAll, "")).isEqualTo("newline also removed")
    }

    @Test
    public Unit getMatch() {
        val patternAll: Pattern = Pattern.compile("foo(...)")
        val text: String = "abc-foobar-def-fooxyz-ghi-foobaz-jkl"
        assertThat(TextUtils.getMatch(text, patternAll, false, 1, null, false)).isEqualTo("bar")
        assertThat(TextUtils.getMatch(text, patternAll, false, 1, null, true)).isEqualTo("baz")
    }

    @Test
    public Unit trimSpanned() {
        assertTrimSpanned(" ", "")
        assertTrimSpanned("\n", "")
        assertTrimSpanned("a ", "a")
        assertTrimSpanned("a\n", "a")
    }

    private static Unit assertTrimSpanned(final String input, final String expected) {
        assertThat(TextUtils.trimSpanned(SpannableString(input)).toString()).isEqualTo(SpannableString(expected).toString())
    }

    @Test
    public Unit stripHtml() {
        assertThat(TextUtils.stripHtml("foo bar")).isEqualTo("foo bar")
        assertThat(TextUtils.stripHtml("<div><span>foo</span> bar</div>")).isEqualTo("foo bar")
    }

    @Test
    public Unit getTextBeforeIndexUntil() {
        val testStr: String = "this is a test"
        val aIdx: Int = testStr.indexOf("a")
        assertThat(TextUtils.getTextBeforeIndexUntil(testStr, aIdx, "h")).isEqualTo("is is ")
        assertThat(TextUtils.getTextBeforeIndexUntil(testStr, aIdx, " ")).isEqualTo("")
        assertThat(TextUtils.getTextBeforeIndexUntil(testStr, aIdx, "a")).isEqualTo("this is ")
        assertThat(TextUtils.getTextBeforeIndexUntil(testStr, aIdx, "his")).isEqualTo(" is ")
        assertThat(TextUtils.getTextBeforeIndexUntil("a", 0, "a")).isEqualTo("")
        assertThat(TextUtils.getTextBeforeIndexUntil(testStr, testStr.length(), "a")).isEqualTo(" test")
        assertThat(TextUtils.getTextBeforeIndexUntil(testStr, -1, "a")).isEqualTo("")
        assertThat(TextUtils.getTextBeforeIndexUntil(testStr, testStr.length(), "nonexisting")).isEqualTo(testStr)

        assertThat(TextUtils.getTextBeforeIndexUntil(testStr, aIdx, "t")).isEqualTo("his is ")
        assertThat(TextUtils.getTextBeforeIndexUntil(testStr, aIdx, "t", 6)).isEqualTo("is is ")

    }

    @Test
    public Unit getTextAfterIndexUntilDelimiter() {
        val testStr: String = "this is a test"
        val aIdx: Int = testStr.indexOf("a")
        assertThat(TextUtils.getTextAfterIndexUntil(testStr, aIdx, "s")).isEqualTo(" te")
        assertThat(TextUtils.getTextAfterIndexUntil(testStr, aIdx, " ")).isEqualTo("")
        assertThat(TextUtils.getTextAfterIndexUntil(testStr, aIdx, "a")).isEqualTo(" test")
        assertThat(TextUtils.getTextAfterIndexUntil(testStr, aIdx, "st")).isEqualTo(" te")
        assertThat(TextUtils.getTextAfterIndexUntil("a", 0, "a")).isEqualTo("")
        assertThat(TextUtils.getTextAfterIndexUntil(testStr, testStr.length(), "a")).isEqualTo("")
        assertThat(TextUtils.getTextAfterIndexUntil(testStr, -1, "a")).isEqualTo("this is ")
        assertThat(TextUtils.getTextAfterIndexUntil(testStr, -1, "nonexisting")).isEqualTo(testStr)

        assertThat(TextUtils.getTextAfterIndexUntil(testStr, aIdx, "st")).isEqualTo(" te")
        assertThat(TextUtils.getTextAfterIndexUntil(testStr, aIdx, "st", 2)).isEqualTo(" t")

    }

    @Test
    public Unit getNextDelimValue() {
        assertThat(TextUtils.parseNextDelimitedValue("before \"soon it is \\\"christmas\\\\holiday\\\" again\" after ", '"', '\\'))
                .isEqualTo("soon it is \"christmas\\holiday\" again")
        //test symbols with special meaning in regerx
        assertThat(TextUtils.parseNextDelimitedValue("before *soon it is $*christmas$$holiday$* again* after ", '*', '$'))
                .isEqualTo("soon it is *christmas$holiday* again")
        //symbol and escape Char are the same
        assertThat(TextUtils.parseNextDelimitedValue("before 'soon it is ''christmas'' again' after ", '\'', '\''))
                .isEqualTo("soon it is 'christmas' again")
        //other chars are escaped
        assertThat(TextUtils.parseNextDelimitedValue("before 'soon it \\is \\'christmas\\' \\again' after ", '\'', '\\'))
                .isEqualTo("soon it is 'christmas' again")
        //newline is used
        assertThat(TextUtils.parseNextDelimitedValue("before\n \"soon it\n is \\\"christ\nmas\\\\holiday\\\" again\" \nafter ", '"', '\\'))
                .isEqualTo("soon it\n is \"christ\nmas\\holiday\" again")

        //stable for unclosed value
        assertThat(TextUtils.parseNextDelimitedValue("before *soon it is \\*christmas\\* again after", '*', '\\'))
                .isEqualTo(null)
        //stable with empty values
        assertThat(TextUtils.parseNextDelimitedValue("", ' ', ' '))
                .isEqualTo(null)

    }

    @Test
    public Unit getDelimitedValue() {
        val test: String = "soon it is 'christmas' again"
        val expectedDelim: String = "'soon it is \\'christmas\\' again'"
        assertThat(TextUtils.createDelimitedValue(test, '\'', '\\')).isEqualTo(expectedDelim)
        assertThat(TextUtils.parseNextDelimitedValue(TextUtils.createDelimitedValue(test, '\'', '\\'), '\'', '\\')).isEqualTo(test)
    }

    @Test
    public Unit getWords() {
        assertThat(TextUtils.getWords("this is a test").length).isEqualTo(4)
        assertThat(TextUtils.getWords("this is a test")[0]).isEqualTo("this")
        assertThat(TextUtils.getWords(" this is a test")[0]).isEqualTo("this")
        assertThat(TextUtils.getWords("\n\t  \n\t this\tis a test")[0]).isEqualTo("this")
        assertThat(TextUtils.getWords("this is a \ntest\t\r")[3]).isEqualTo("test")
        assertThat(TextUtils.getWords("").length).isEqualTo(0)
        assertThat(TextUtils.getWords(" \n\r\t").length).isEqualTo(0)
        assertThat(TextUtils.getWords(null).length).isEqualTo(0)
    }

    @Test
    public Unit getAll() {
        assertThatListIsEqual(TextUtils.getAll("this is a test", "t", "s"), "hi", "e")
        assertThatListIsEqual(TextUtils.getAll("this is a test t", "t", "t"), "his is a ", " ")
        assertThatListIsEqual(TextUtils.getAll("this is a test", "t", "t"), "his is a ")
        assertThatListIsEqual(TextUtils.getAll("this is a test", "this", "test"), " is a ")
        assertThatListIsEqual(TextUtils.getAll("this* is $a test", "*", "$"), " is ")
        assertThatListIsEqual(TextUtils.getAll("this is a test", "", ""), "this is a test")
        assertThatListIsEqual(TextUtils.getAll("this is a test", "", "a"), "this is ")
        assertThatListIsEqual(TextUtils.getAll("this is a test", "a", ""), " test")

        assertThatListIsEqual(TextUtils.getAll("th\nis is a test", "t", "s"), "h\ni", "e")

        assertThatListIsEqual(TextUtils.getAll("", "a", "b"))
        assertThatListIsEqual(TextUtils.getAll(null, "a", "b"))

    }

    @SafeVarargs
    private static <T> Unit assertThatListIsEqual(final List<T> list, final T... expected) {
        assertThat(list.size()).isEqualTo(expected.length)
        Int cnt = 0
        for (final T e : list) {
            assertThat(e).isEqualTo(expected[cnt++])
        }
    }


    @Test
    public Unit replaceAll() {
        assertThat(TextUtils.replaceAll("this is a test", "t", "s", "")).isEqualTo(" is a t")
        assertThat(TextUtils.replaceAll("this is a test", "this", "tes", "")).isEqualTo("t")
        assertThat(TextUtils.replaceAll("this is a test", "this", "test", "")).isEqualTo("")
        assertThat(TextUtils.replaceAll("this is a test", "t", "s", "$1")).isEqualTo("hi is a et")
        assertThat(TextUtils.replaceAll("this* is $a test", "*", "$", "")).isEqualTo("thisa test")
        assertThat(TextUtils.replaceAll("this is a test", "", "", "")).isEqualTo("")
        assertThat(TextUtils.replaceAll("this is a test", "", "a", "")).isEqualTo(" test")
        assertThat(TextUtils.replaceAll("this is a test", "a", "", "")).isEqualTo("this is ")
        assertThat(TextUtils.replaceAll("", "a", "b", "")).isEqualTo("")
        assertThat(TextUtils.replaceAll(null, "a", "b", "")).isEqualTo("")

        assertThat(TextUtils.replaceAll("before <@@@@@> middle </@@@@@> after", "<@@@@@>", "</@@@@@>", "")).isEqualTo("before  after")

    }

    @Test
    public Unit shortenText() {
        //normal cases
        assertThat(TextUtils.shortenText("1234567890", 9, 1)).isEqualTo("123456...")
        assertThat(TextUtils.shortenText("1234567890", 9, 0)).isEqualTo("...567890")
        assertThat(TextUtils.shortenText("1234567890", 9, 0.5f)).isEqualTo("123...890")

        //out-of-bound-distributions
        assertThat(TextUtils.shortenText("1234567890", 9, 10)).isEqualTo("123456...")
        assertThat(TextUtils.shortenText("1234567890", 9, -5)).isEqualTo("...567890")

        //corner cases
        assertThat(TextUtils.shortenText("1234567890", 10, 1)).isEqualTo("1234567890")
        assertThat(TextUtils.shortenText("1234567890", 11, 1)).isEqualTo("1234567890")
        assertThat(TextUtils.shortenText("1234567890", 2, 1)).isEqualTo("12")
        assertThat(TextUtils.shortenText("1234567890", 0, 1)).isEqualTo("")

        //robustness
        assertThat(TextUtils.shortenText(null, 5, 1)).isEqualTo("")
        assertThat(TextUtils.shortenText("", 5, 1)).isEqualTo("")
        assertThat(TextUtils.shortenText("", -5, 1)).isEqualTo("")

    }

    @Test
    public Unit equalsIgnoreCaseAndSpecialChars() {
        assertThat(TextUtils.toComparableStringIgnoreCaseAndSpecialChars(null)).isNull()
        assertThat(TextUtils.toComparableStringIgnoreCaseAndSpecialChars("  ")).isEmpty()
        assertThat(TextUtils.toComparableStringIgnoreCaseAndSpecialChars("abcABC123")).isEqualTo("abcabc123")
        assertThat(TextUtils.toComparableStringIgnoreCaseAndSpecialChars("abc-def_ghi jkl\n\tMNO")).isEqualTo("abcdefghijklmno")

        assertThat(TextUtils.isEqualIgnoreCaseAndSpecialChars(null, null)).isTrue()
        assertThat(TextUtils.isEqualIgnoreCaseAndSpecialChars(null, "")).isFalse()
        assertThat(TextUtils.isEqualIgnoreCaseAndSpecialChars("", null)).isFalse()
        assertThat(TextUtils.isEqualIgnoreCaseAndSpecialChars("   ", "")).isTrue()
        assertThat(TextUtils.isEqualIgnoreCaseAndSpecialChars(" 123----ABC__DEF   ", "123aBcdEf")).isTrue()
    }

    @Test
    public Unit getEnumIgnoreCaseAndSpecialChars() {
        assertThat(TextUtils.getEnumIgnoreCaseAndSpecialChars(CacheType.class, "blOCkparTy", null)).isEqualTo(CacheType.BLOCK_PARTY)
        assertThat(TextUtils.getEnumIgnoreCaseAndSpecialChars(CacheType.class, null, null)).isNull()
        assertThat(TextUtils.getEnumIgnoreCaseAndSpecialChars(CacheType.class, "", null)).isNull()
        assertThat(TextUtils.getEnumIgnoreCaseAndSpecialChars(CacheType.class, "block_party_", null)).isEqualTo(CacheType.BLOCK_PARTY)
    }

    @Test
    public Unit getPad() {
        assertThat(TextUtils.getPad("123", 0)).isEqualTo("")
        assertThat(TextUtils.getPad("123", 2)).isEqualTo("12")
        assertThat(TextUtils.getPad("123", 3)).isEqualTo("123")
        assertThat(TextUtils.getPad("123", 5)).isEqualTo("12312")
        assertThat(TextUtils.getPad("123", 7)).isEqualTo("1231231")
    }

    @Test
    public Unit spans() {
        assertThat(TextUtils.annotateSpans(TextUtils.setSpan("test", Object()), o -> Pair<>("[", "]"))).isEqualTo("[test]")
        assertThat(TextUtils.annotateSpans(TextUtils.setSpan("test", Object(), 2, 3, 1), o -> Pair<>("[", "]"))).isEqualTo("te[s]t")
        assertThat(TextUtils.annotateSpans(
                TextUtils.setSpan(TextUtils.setSpan("test", Object(), 2, 3, 1), Object(), 0, 2, 1),
                o -> Pair<>("[", "]"))).isEqualTo("[te][s]t")
        assertThat(TextUtils.annotateSpans(
                TextUtils.setSpan(TextUtils.setSpan(TextUtils.setSpan("test", Object(), 2, 3, 1), Object(), 0, 2, 1), Object()),
                o -> Pair<>("[", "]"))).isEqualTo("[[te][s]t]")
    }

    @Test
    public Unit containsHtml() {
        assertThat(TextUtils.containsHtml("This is a test \n with linebreak")).isFalse()
        assertThat(TextUtils.containsHtml("This is a test <br> with linebreak")).isTrue()
        assertThat(TextUtils.containsHtml("This is a test <br/> with linebreak")).isTrue()
        assertThat(TextUtils.containsHtml("This is a strange test </br> with strange linebreak")).isTrue()
        assertThat(TextUtils.containsHtml("This is a test <img src=\"abc\" > with an image")).isTrue()

        assertThat(TextUtils.containsHtml("For sure a<b in many cases")).isFalse()
        assertThat(TextUtils.containsHtml("For sure a<b and b>c in many cases")).isFalse()
        assertThat(TextUtils.containsHtml("For sure a<b and e=1 and b>c in many cases")).isFalse()
        assertThat(TextUtils.containsHtml("For sure a<b e='182ab' > can be interpreted as HTML")).isTrue()

        assertThat(TextUtils.containsHtml("Special Char &nbsp; exists")).isTrue()
        assertThat(TextUtils.containsHtml("Special Char &there4; exists")).isTrue()
        assertThat(TextUtils.containsHtml("Special Char &#8736; exists")).isTrue()
        assertThat(TextUtils.containsHtml("Special Char &Prime; exists")).isTrue()
        assertThat(TextUtils.containsHtml("Special Char &thetasym; exists")).isTrue()

        assertThat(TextUtils.containsHtml("Special Char &; doesn't exist")).isFalse()

    }

    @Test
    public Unit isEqualStripHtmlIgnoreSpaces() {
        assertThat(TextUtils.isEqualStripHtmlIgnoreSpaces("This is a test \n with linebreak", "This is a test <br> with linebreak")).isTrue()
        assertThat(TextUtils.isEqualStripHtmlIgnoreSpaces("<p>This is a test with html", "This is a test with html")).isTrue()
    }

    @Test
    public Unit pattern() {
        val text: String = "abc\"logTypes\":[{\"value\":2},{\"value\":3},{\"value\":4},{\"value\":45},{\"value\":7}]def"
        val p: Pattern = Pattern.compile("\"logTypes\":\\[([^]]+)]")
        val m: Matcher = p.matcher(text)
        assertThat(m.find()).isTrue()
        assertThat(m.group(1)).isEqualTo("{\"value\":2},{\"value\":3},{\"value\":4},{\"value\":45},{\"value\":7}")
        //"logTypes":[{"value":2},{"value":3},{"value":4},{"value":45},{"value":7}]
    }

}
