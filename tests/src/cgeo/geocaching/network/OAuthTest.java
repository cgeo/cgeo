package cgeo.geocaching.network;

import java.util.ArrayList;
import java.util.List;

import junit.framework.TestCase;

public class OAuthTest extends TestCase {
    private static final List<Character> UNRESERVED;
    static {
        // unreserved characters: ALPHA / DIGIT / "-" / "." / "_" / "~"
        ArrayList<Character> unreserved = new ArrayList<Character>();
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
        UNRESERVED = unreserved;
    }

    public static void testUnreservedCharactersMustNotBeEncoded() {
        for (Character c : UNRESERVED) {
            final String charAsString = String.valueOf(c);
            assertEquals("wrong OAuth encoding for " + c, charAsString, OAuth.percentEncode(charAsString));
        }
    }

    public static void testOtherCharactersMustBeEncoded() {
        for (int i = 32; i < 127; i++) {
            final Character c = (char) i;
            if (!UNRESERVED.contains(c)) {
                final String charAsString = String.valueOf(c);
                final String encoded = OAuth.percentEncode(charAsString);
                assertFalse("Character '" + charAsString + "' not encoded", charAsString.equals(encoded));
                assertTrue(encoded.startsWith("%"));
            }
        }
    }

    public static void testAsterisk() {
        assertFalse("*".equals(OAuth.percentEncode("*")));
    }
}
