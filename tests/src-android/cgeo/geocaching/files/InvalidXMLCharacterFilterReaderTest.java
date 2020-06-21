package cgeo.geocaching.files;

import android.sax.RootElement;
import android.test.AndroidTestCase;
import android.util.Xml;

import java.io.StringReader;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Java6Assertions.assertThat;

public class InvalidXMLCharacterFilterReaderTest extends AndroidTestCase {

    public static void testFilterInvalid() throws Exception {
        final RootElement root = new RootElement("desc");
        final AtomicReference<String> description = new AtomicReference<>();
        root.setEndTextElementListener(description::set);
        final StringReader reader = new StringReader("<?xml version=\"1.0\" encoding=\"utf-8\"?><desc>Invalid&#xB;description</desc>");
        Xml.parse(new InvalidXMLCharacterFilterReader(reader), root.getContentHandler());
        assertThat(description.get()).isEqualTo("Invaliddescription");
    }

    public static void testGC5AYC6() throws Exception {
        final RootElement root = new RootElement("desc");
        final AtomicReference<String> description = new AtomicReference<>();
        root.setEndTextElementListener(description::set);
        final StringReader reader = new StringReader("<?xml version=\"1.0\" encoding=\"utf-8\"?><desc>V‹¥IR‡U½S©&#x15; by Master-Chief, Unknown Cache (5/2)</desc>");
        Xml.parse(new InvalidXMLCharacterFilterReader(reader), root.getContentHandler());
        assertThat(description.get()).isEqualTo("V‹¥IR‡U½S© by Master-Chief, Unknown Cache (5/2)");
    }
}
