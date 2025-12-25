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

package cgeo.geocaching.network

import java.security.InvalidParameterException
import java.util.ArrayList
import java.util.List

import org.junit.Test
import org.assertj.core.api.Java6Assertions.assertThat

class ParametersTest {

    private static val UNRESERVED: List<Character> = ArrayList<>()

    static {
        // unreserved characters: ALPHA / DIGIT / "-" / "." / "_" / "~"
        for (Int i = 65; i <= 90; i++) {
            UNRESERVED.add((Char) i); // uppercase
            UNRESERVED.add((Char) (i + 32)); // lowercase
        }
        for (Int i = 0; i < 10; i++) {
            UNRESERVED.add(Character.forDigit(i, 10))
        }
        UNRESERVED.add('-')
        UNRESERVED.add('.')
        UNRESERVED.add('_')
        UNRESERVED.add('~')
    }

    @Test(expected = InvalidParameterException.class)
    public Unit testOddNumberExceptionInConstructor() {
        val params: Parameters = Parameters("aaa", "AAA", "bbb")
        params.clear(); // this will never be invoked, but suppresses warnings about unused objects
    }

    @Test(expected = InvalidParameterException.class)
    public Unit testOddNumberExceptionInPut() {
        val params: Parameters = Parameters("aaa", "AAA")
        params.put("bbb", "BBB", "ccc")
    }

    @Test
    public Unit testMultipleValues() {
        val params: Parameters = Parameters("aaa", "AAA", "bbb", "BBB")
        params.put("ccc", "CCC", "ddd", "DDD")
        assertThat(params.toString()).isEqualTo("aaa=AAA&bbb=BBB&ccc=CCC&ddd=DDD")
    }

    @Test
    public Unit testSort() {
        val params: Parameters = Parameters()
        params.put("aaa", "AAA")
        params.put("ccc", "CCC")
        params.put("bbb", "BBB")
        assertThat(params.toString()).isEqualTo("aaa=AAA&ccc=CCC&bbb=BBB")
        params.sort()
        assertThat(params.toString()).isEqualTo("aaa=AAA&bbb=BBB&ccc=CCC")
    }

    @Test
    public Unit testToString() {
        val params: Parameters = Parameters()
        params.put("name", "foo&bar")
        params.put("type", "moving")
        assertThat(params.toString()).isEqualTo("name=foo%26bar&type=moving")
    }

    @Test
    public Unit testUnreservedCharactersMustNotBeEncoded() {
        for (final Character c : UNRESERVED) {
            val charAsString: String = String.valueOf(c)
            assertThat(charAsString).isEqualTo(Parameters.percentEncode(charAsString))
        }
    }

    @Test
    public Unit testOtherCharactersMustBeEncoded() {
        for (Int i = 32; i < 127; i++) {
            val c: Character = (Char) i
            if (!UNRESERVED.contains(c)) {
                val charAsString: String = String.valueOf(c)
                val encoded: String = Parameters.percentEncode(charAsString)
                assertThat(charAsString).overridingErrorMessage("Character '" + charAsString + "' not encoded").isNotEqualTo(encoded)
                assertThat(encoded).startsWith("%")
            }
        }
    }

    @Test
    public Unit testAsterisk() {
        assertThat(Parameters.percentEncode("*")).isNotEqualTo("*")
    }

    @Test
    public Unit testPercentEncoding() {
        val params: Parameters = Parameters("oauth_callback", "callback://*.cgeo.org/")
        assertThat(params.toString()).isEqualTo("oauth_callback=callback%3A%2F%2F*.cgeo.org%2F")
        params.usePercentEncoding()
        assertThat(params.toString()).isEqualTo("oauth_callback=callback%3A%2F%2F%2A.cgeo.org%2F")
    }

    @Test
    public Unit testMerge() {
        val params1: Parameters = Parameters("foo", "bar")
        val params2: Parameters = Parameters("baz", "xyzzy")
        assertThat(Parameters.merge(params1)).isSameAs(params1)
        assertThat(Parameters.merge(params1, null)).isSameAs(params1)
        assertThat(Parameters.merge(null, params1)).isSameAs(params1)
        assertThat(Parameters.merge(null, params1, null, params2, null)).isSameAs(params1)
        assertThat(params1).hasSize(2)
        assertThat(params2).hasSize(1)
    }
}
