package cgeo.geocaching.network;

import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import static org.assertj.core.api.Java6Assertions.assertThat;

public class ParametersTest {

    private static final List<Character> UNRESERVED = new ArrayList<>();

    static {
        // unreserved characters: ALPHA / DIGIT / "-" / "." / "_" / "~"
        for (int i = 65; i <= 90; i++) {
            UNRESERVED.add((char) i); // uppercase
            UNRESERVED.add((char) (i + 32)); // lowercase
        }
        for (int i = 0; i < 10; i++) {
            UNRESERVED.add(Character.forDigit(i, 10));
        }
        UNRESERVED.add('-');
        UNRESERVED.add('.');
        UNRESERVED.add('_');
        UNRESERVED.add('~');
    }

    @Test(expected = InvalidParameterException.class)
    public void testOddNumberExceptionInConstructor() {
        final Parameters params = new Parameters("aaa", "AAA", "bbb");
        params.clear(); // this will never be invoked, but suppresses warnings about unused objects
    }

    @Test(expected = InvalidParameterException.class)
    public void testOddNumberExceptionInPut() {
        final Parameters params = new Parameters("aaa", "AAA");
        params.put("bbb", "BBB", "ccc");
    }

    @Test
    public void testMultipleValues() {
        final Parameters params = new Parameters("aaa", "AAA", "bbb", "BBB");
        params.put("ccc", "CCC", "ddd", "DDD");
        assertThat(params.toString()).isEqualTo("aaa=AAA&bbb=BBB&ccc=CCC&ddd=DDD");
    }

    @Test
    public void testSort() {
        final Parameters params = new Parameters();
        params.put("aaa", "AAA");
        params.put("ccc", "CCC");
        params.put("bbb", "BBB");
        assertThat(params.toString()).isEqualTo("aaa=AAA&ccc=CCC&bbb=BBB");
        params.sort();
        assertThat(params.toString()).isEqualTo("aaa=AAA&bbb=BBB&ccc=CCC");
    }

    @Test
    public void testToString() {
        final Parameters params = new Parameters();
        params.put("name", "foo&bar");
        params.put("type", "moving");
        assertThat(params.toString()).isEqualTo("name=foo%26bar&type=moving");
    }

    @Test
    public void testUnreservedCharactersMustNotBeEncoded() {
        for (final Character c : UNRESERVED) {
            final String charAsString = String.valueOf(c);
            assertThat(charAsString).isEqualTo(Parameters.percentEncode(charAsString));
        }
    }

    @Test
    public void testOtherCharactersMustBeEncoded() {
        for (int i = 32; i < 127; i++) {
            final Character c = (char) i;
            if (!UNRESERVED.contains(c)) {
                final String charAsString = String.valueOf(c);
                final String encoded = Parameters.percentEncode(charAsString);
                assertThat(charAsString).overridingErrorMessage("Character '" + charAsString + "' not encoded").isNotEqualTo(encoded);
                assertThat(encoded).startsWith("%");
            }
        }
    }

    @Test
    public void testAsterisk() {
        assertThat(Parameters.percentEncode("*")).isNotEqualTo("*");
    }

    @Test
    public void testPercentEncoding() {
        final Parameters params = new Parameters("oauth_callback", "callback://*.cgeo.org/");
        assertThat(params.toString()).isEqualTo("oauth_callback=callback%3A%2F%2F*.cgeo.org%2F");
        params.usePercentEncoding();
        assertThat(params.toString()).isEqualTo("oauth_callback=callback%3A%2F%2F%2A.cgeo.org%2F");
    }

    @Test
    public void testMerge() {
        final Parameters params1 = new Parameters("foo", "bar");
        final Parameters params2 = new Parameters("baz", "xyzzy");
        assertThat(Parameters.merge(params1)).isSameAs(params1);
        assertThat(Parameters.merge(params1, null)).isSameAs(params1);
        assertThat(Parameters.merge(null, params1)).isSameAs(params1);
        assertThat(Parameters.merge(null, params1, null, params2, null)).isSameAs(params1);
        assertThat(params1).hasSize(2);
        assertThat(params2).hasSize(1);
    }
}
