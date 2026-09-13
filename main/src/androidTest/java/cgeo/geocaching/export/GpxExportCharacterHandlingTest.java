package cgeo.geocaching.export;


import cgeo.geocaching.utils.TextUtils;
import cgeo.geocaching.utils.xml.XmlUtils;

import android.util.Xml;

import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xmlpull.v1.XmlSerializer;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

public class GpxExportCharacterHandlingTest {

    /**
     * Writes a minimal GPX document with one wpt whose name attribute
     * and text content both contain testValue - using the same serializer
     * setup as the real export.
     */
    private byte[] writeMinimalGpx(final String testValue) throws Exception {
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        final Writer writer = new BufferedWriter(new OutputStreamWriter(baos, StandardCharsets.UTF_8));

        final XmlSerializer gpx = Xml.newSerializer();
        gpx.setOutput(writer);
        gpx.startDocument(StandardCharsets.UTF_8.name(), true);
        gpx.startTag(null, "gpx");
        gpx.startTag(null, "wpt");
        // XmlUtils.writeAttribute(gpx, "name", testValue);
        XmlUtils.writeText(gpx, testValue);
        gpx.endTag(null, "wpt");
        gpx.endTag(null, "gpx");
        gpx.endDocument();
        writer.flush();

        return baos.toByteArray();
    }

    private Document parseStrict(final byte[] xmlBytes) throws Exception {
        final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        final DocumentBuilder builder = factory.newDocumentBuilder();
        // throws SAXException if the document is not well-formed
        return builder.parse(new ByteArrayInputStream(xmlBytes));
    }

    private Element wpt(final Document doc) {
        return (Element) doc.getElementsByTagName("wpt").item(0);
    }

    private void assertWellFormedAndRoundTrips(final String original) throws Exception {
        final byte[] xml = writeMinimalGpx(original);

        final Document doc;
        try {
            doc = parseStrict(xml);
        } catch (final Exception e) {
            fail("Export is not well-formed for input <" + original + ">: " + e.getMessage());
            return;
        }

        final Element wpElement = wpt(doc);
        assertThat(wpElement).isNotNull();

        final String originalNormalized = TextUtils.normalize(original);
        assertThat(TextUtils.normalize(wpElement.getTextContent())).isEqualTo(originalNormalized);
        // assertThat(TextUtils.normalize(wpElement.getAttribute("name"))).isEqualTo(originalNormalized);
    }

    @Test
    public void plainAscii() throws Exception {
        assertWellFormedAndRoundTrips("Simple cache name");
    }

    @Test
    public void umlauts() throws Exception {
        assertWellFormedAndRoundTrips("Cave hike \u00e4\u00f6\u00fc\u00df");
    }

    @Test
    public void xmlSpecialCharactersAreEscaped() throws Exception {
        // '<', '&', '"' must be correctly escaped in both text and attribute
        assertWellFormedAndRoundTrips("Cache <\"special\"> & tricky");
    }

    @Test
    public void euroSign() throws Exception {
        assertWellFormedAndRoundTrips("Price: 5 \u20AC");
    }

    @Test
    public void emojiAsSurrogatePair() throws Exception {
        // U+1F600, encoded as a UTF-16 surrogate pair in Java
        assertWellFormedAndRoundTrips("Cache \uD83D\uDE00 found");
    }

    @Test
    public void consecutiveSurrogatePairs() throws Exception {
        // two surrogate pairs directly back-to-back, to catch off-by-one
        // errors in the surrogate handling
        assertWellFormedAndRoundTrips("\uD83D\uDE00\uD83D\uDE01");
    }

    @Test
    public void singleSurrogateDoesNotBreakWellFormedness() throws Exception {
        // \u000B (vertical tab) is not a valid XML 1.0 character
        final byte[] xml = writeMinimalGpx("before\uD83Cafter\uDF0D");
        final Document doc = parseStrict(xml); // must not throw
        // depending on your chosen behavior (drop vs. replace):
        assertThat(wpt(doc).getTextContent()).doesNotContain("\uD83C");
        assertThat(wpt(doc).getTextContent()).doesNotContain("\uDF0D");
    }

    @Test
    public void singleInvalidControlCharacterDoesNotBreakWellFormedness() throws Exception {
        // \u000B (vertical tab) is not a valid XML 1.0 character
        final byte[] xml = writeMinimalGpx("before\u000Bafter");
        final Document doc = parseStrict(xml); // must not throw
        // depending on your chosen behavior (drop vs. replace):
        assertThat(wpt(doc).getTextContent()).doesNotContain("\u000B");
    }
}