package cgeo.geocaching.files;

import static org.assertj.core.api.Assertions.assertThat;

import android.sax.EndTextElementListener;
import android.sax.RootElement;
import android.test.AndroidTestCase;
import android.util.Xml;

import java.io.StringReader;
import java.util.concurrent.atomic.AtomicReference;

public class InvalidXMLCharacterFilterReaderTest extends AndroidTestCase {

    public static void testFilterInvalid() throws Exception {
        final RootElement root = new RootElement("desc");
        final AtomicReference<String> description = new AtomicReference<>();
        root.setEndTextElementListener(new EndTextElementListener() {

            @Override
            public void end(final String body) {
                description.set(body);
            }
        });
        final StringReader reader = new StringReader("<?xml version=\"1.0\" encoding=\"utf-8\"?><desc>Invalid&#xB;description</desc>");
        Xml.parse(new InvalidXMLCharacterFilterReader(reader), root.getContentHandler());
        assertThat(description.get()).isEqualTo("Invaliddescription");
    }
}
