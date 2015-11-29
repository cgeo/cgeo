package cgeo.geocaching.files;

import android.sax.EndTextElementListener;
import android.sax.RootElement;
import android.util.Xml;

import org.junit.Test;

import java.io.StringReader;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

public class InvalidXMLCharacterFilterReaderTest  {

    @Test
    public void testFilterInvalid() throws Exception {
        final RootElement root = new RootElement("desc");
        final AtomicReference<String> description = new AtomicReference<String>();
        root.setEndTextElementListener(new EndTextElementListener() {

            @Override
            public void end(String body) {
                description.set(body);
            }
        });
        StringReader reader = new StringReader("<?xml version=\"1.0\" encoding=\"utf-8\"?><desc>Invalid&#xB;description</desc>");
        Xml.parse(new InvalidXMLCharacterFilterReader(reader), root.getContentHandler());
        assertThat(description.get()).isEqualTo("Invaliddescription");
    }
}
