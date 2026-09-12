package cgeo.geocaching.files;

import java.io.IOException;
import java.io.StringReader;

import org.junit.Test;
import static org.assertj.core.api.Assertions.assertThat;

public class InvalidXMLCharacterFilterReaderTest {

    private static String filter(final String input) throws IOException {
        final InvalidXMLCharacterFilterReader reader = new InvalidXMLCharacterFilterReader(new StringReader(input));
        final char[] buf = new char[input.length() + 10];
        final int n = reader.read(buf, 0, buf.length);
        if (n <= 0) {
            return "";
        }
        return new String(buf, 0, n);
    }

    @Test
    public void testValidAsciiPassesThrough() throws IOException {
        assertThat(filter("hello world")).isEqualTo("hello world");
    }

    @Test
    public void testInvalidXmlCharIsFiltered() throws IOException {
        // U+000B (vertical tab) is not a valid XML character
        assertThat(filter("invalid\u000Bchar")).isEqualTo("invalidchar");
    }

    @Test
    public void testSurrogatePairPassesThrough() throws IOException {
        final String emojis = "\uD83E\uDD86\uD83D\uDE80\uD83C\uDF0D";
        assertThat(filter("before" + emojis + "after")).isEqualTo("before" + emojis + "after");
    }

    @Test
    public void testValidXmlControlCharactersPassThrough() throws IOException {
        assertThat(filter("before\ttab\nline\rreturn&#x9;after"))
                .isEqualTo("before\ttab\nline\rreturn&#x9;after");
    }

    @Test
    public void testInvalidXmlControlCharacterIsFiltered() throws IOException {
        // U+000B (vertical tab) is not a valid XML 1.0 character
        assertThat(filter("before\u000Bafter\u001F")).isEqualTo("beforeafter");
    }

    @Test
    public void testUnpairedSurrogateIsFiltered() throws IOException {
        assertThat(filter("before\uD83Eafter\uDD86")).isEqualTo("beforeafter");
    }

    @Test
    public void testSupplementaryCharacterReferencePassesThrough() throws IOException {
        // U+1F986 = 🦆
        assertThat(filter("before&#x1F986;&#129414;after\uD83E\uDD86"))
                .isEqualTo("before&#x1F986;&#129414;after\uD83E\uDD86");
    }

    @Test
    public void testSurrogateCharacterReferenceIsFiltered() throws IOException {
        // U+D83E is only a UTF-16 surrogate, not a valid XML codepoint
        assertThat(filter("before&#xD83E;after")).isEqualTo("beforeafter");
    }

    @Test
    public void testInvalidCharacterReferenceIsFiltered() throws IOException {
        // U+001F is not a valid XML 1.0 character
        assertThat(filter("before&#x0;&#x1F;after")).isEqualTo("beforeafter");
    }

    @Test
    public void testBmpCharacterPassesThrough() throws IOException {
        assertThat(filter("äöü € © Ω")).isEqualTo("äöü € © Ω");
    }
}
