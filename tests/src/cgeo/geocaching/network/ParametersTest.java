package cgeo.geocaching.network;

import junit.framework.TestCase;
import org.eclipse.jdt.annotation.NonNull;

import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class ParametersTest extends TestCase {

    static List<Character> UNRESERVED;

    static {
        // unreserved characters: ALPHA / DIGIT / "-" / "." / "_" / "~"
        final ArrayList<Character> unreserved = new ArrayList<Character>();
        for (int i = 65; i <= 90; i++) {
            unreserved.add((char) i); // uppercase
            unreserved.add((char) (i + 32)); // lowercase
        }
        for (int i = 0; i < 10; i++) {
            unreserved.add(Character.forDigit(i, 10));
        }
        unreserved.add('-');
        unreserved.add('.');
        unreserved.add('_');
        unreserved.add('~');
        ParametersTest.UNRESERVED = unreserved;
    }

    public static void testException() {
        try {
            final Parameters params = new Parameters("aaa", "AAA", "bbb");
            params.clear(); // this will never be invoked, but suppresses warnings about unused objects
            fail("Exception not raised");
        } catch (InvalidParameterException e) {
            // Ok
        }
        try {
            final Parameters params = new Parameters("aaa", "AAA");
            params.put("bbb", "BBB", "ccc");
            fail("Exception not raised");
        } catch (InvalidParameterException e) {
            // Ok
        }
    }

    public static void testMultipleValues() {
        final Parameters params = new Parameters("aaa", "AAA", "bbb", "BBB");
        params.put("ccc", "CCC", "ddd", "DDD");
        assertThat(params.toString()).isEqualTo("aaa=AAA&bbb=BBB&ccc=CCC&ddd=DDD");
    }

    public static void testSort() {
        final Parameters params = new Parameters();
        params.put("aaa", "AAA");
        params.put("ccc", "CCC");
        params.put("bbb", "BBB");
        assertThat(params.toString()).isEqualTo("aaa=AAA&ccc=CCC&bbb=BBB");
        params.sort();
        assertThat(params.toString()).isEqualTo("aaa=AAA&bbb=BBB&ccc=CCC");
    }

    public static void testToString() {
        final Parameters params = new Parameters();
        params.put("name", "foo&bar");
        params.put("type", "moving");
        assertThat(params.toString()).isEqualTo("name=foo%26bar&type=moving");
    }

    public static void testUnreservedCharactersMustNotBeEncoded() {
        for (Character c : UNRESERVED) {
            final @NonNull
            String charAsString = String.valueOf(c);
            assertEquals("wrong OAuth encoding for " + c, charAsString, Parameters.percentEncode(charAsString));
        }
    }

    public static void testOtherCharactersMustBeEncoded() {
        for (int i = 32; i < 127; i++) {
            final Character c = (char) i;
            if (!UNRESERVED.contains(c)) {
                final @NonNull
                String charAsString = String.valueOf(c);
                final String encoded = Parameters.percentEncode(charAsString);
                assertThat(charAsString).overridingErrorMessage("Character '" + charAsString + "' not encoded").isNotEqualTo(encoded);
                assertThat(encoded).startsWith("%");
            }
        }
    }

    public static void testAsterisk() {
        assertThat("*".equals(Parameters.percentEncode("*"))).isFalse();
    }

    public static void testPercentEncoding() {
        final Parameters params = new Parameters("oauth_callback", "callback://www.cgeo.org/");
        assertThat(params.toString()).isEqualTo("oauth_callback=callback://www.cgeo.org/");
        params.usePercentEncoding();
        assertThat(params.toString()).isEqualTo("oauth_callback=callback%3A%2F%2Fwww.cgeo.org%2F");
    }
}
