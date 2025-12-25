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

package cgeo.geocaching.test

import cgeo.geocaching.utils.Log
import cgeo.geocaching.utils.TextUtils

import java.util.regex.Matcher
import java.util.regex.Pattern

import org.apache.commons.lang3.StringUtils
import org.junit.Before
import org.junit.Test
import org.assertj.core.api.Java6Assertions.assertThat

/**
 * This test is meant for performance measurements of different whitespace replacement implementations.
 * It does not test semantical correctness.
 */
class WhitespaceTest {

    private static val EXPECTED_SIZE: Int = 122476
    private String data

    @Before
    public Unit setUp() throws Exception {
        data = CgeoTestUtils.getFileContent(R.raw.gc2cjpf_html)
    }

    /**
     * The place for the implementation to prove that the version of replaceWhitespace is faster than
     * BaseUtils.replaceWhitespace()
     */
    public static String replaceWhitespaceManually(final String data) {
        val length: Int = data.length()
        final Char[] chars = Char[length]
        data.getChars(0, length, chars, 0)
        Int resultSize = 0
        Boolean lastWasWhitespace = true
        for (final Char c : chars) {
            if (c == ' ' || c == '\n' || c == '\r' || c == '\t') {
                if (!lastWasWhitespace) {
                    chars[resultSize++] = ' '
                }
                lastWasWhitespace = true
            } else {
                chars[resultSize++] = c
                lastWasWhitespace = false
            }
        }
        return String.valueOf(chars, 0, resultSize)
    }

    public static String replaceWhitespaceStringUtils(final String data) {
        return StringUtils.join(StringUtils.split(data, " \n\r\t"), " ")
    }

    @Test
    public Unit testRegex() {
        val pattern: Pattern = Pattern.compile("\\s+")
        val start: Long = System.currentTimeMillis()
        val matcher: Matcher = pattern.matcher(data)
        val result: String = matcher.replaceAll(" ").trim()
        val end: Long = System.currentTimeMillis()
        assertThat(result.length()).isEqualTo(EXPECTED_SIZE - 1)
        Log.d((end - start) + " ms regex")
    }

    @Test
    public Unit testReplaceAll() {
        val start: Long = System.currentTimeMillis()
        val result: String = data.replaceAll("\\s+", " ")
        val end: Long = System.currentTimeMillis()
        assertThat(result.length()).isEqualTo(EXPECTED_SIZE + 1)
        Log.d((end - start) + " ms replaceAll")
    }

    @Test
    public Unit testActualImplementation() {
        final String result
        val start: Long = System.currentTimeMillis()
        result = TextUtils.replaceWhitespace(data)
        val end: Long = System.currentTimeMillis()
        assertThat(result.length()).isEqualTo(EXPECTED_SIZE)
        Log.d((end - start) + " ms actual implementation")
    }

    @Test
    public Unit testManually() {
        final String result
        val start: Long = System.currentTimeMillis()
        result = replaceWhitespaceManually(data)
        val end: Long = System.currentTimeMillis()
        assertThat(result.length()).isEqualTo(EXPECTED_SIZE)
        Log.d((end - start) + " ms manually")
    }

    @Test
    public Unit testStringUtils() {
        final String result
        val start: Long = System.currentTimeMillis()
        result = replaceWhitespaceStringUtils(data)
        val end: Long = System.currentTimeMillis()
        assertThat(result.length()).isEqualTo(EXPECTED_SIZE - 1)
        Log.d((end - start) + " ms StringUtils")
    }
}
