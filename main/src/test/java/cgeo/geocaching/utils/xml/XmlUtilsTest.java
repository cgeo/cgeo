package cgeo.geocaching.utils.xml;

import android.util.Xml;

import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;

import org.junit.Before;
import org.junit.Test;
import org.xmlpull.v1.XmlSerializer;
import static org.assertj.core.api.Assertions.assertThat;

public class XmlUtilsTest {

    private XmlSerializer xml;

    private StringWriter stringWriter;

    @Before
    public void setUp() throws Exception {
        stringWriter = new StringWriter();
        xml = Xml.newSerializer();
        xml.setOutput(stringWriter);
        xml.startDocument(StandardCharsets.UTF_8.name(), null);
    }

    @Test
    public void testSimpleText() throws Exception {
        XmlUtils.simpleText(xml, "", "tag", "text");
        assertXmlEquals("<tag>text</tag>");
    }

    @Test
    public void testSimpleTextWithPrefix() throws Exception {
        XmlUtils.simpleText(xml, "prefix", "tag", "text");
        assertXmlEquals("<n0:tag xmlns:n0=\"prefix\">text</n0:tag>");
    }


    @Test
    public void testMultipleTexts() throws Exception {
        XmlUtils.multipleTexts(xml, "", "tag1", "text1", "tag2", "text2");
        assertXmlEquals("<tag1>text1</tag1><tag2>text2</tag2>");
    }

    @Test
    public void testSkipIllegalChars() throws Exception {
        XmlUtils.simpleText(xml, "", "tag", "Vom\u0001 Gasthaus\u000f zur \u000bPyramide\u0020aus \u0018Glas\u0009");
        assertXmlEquals("<tag>Vom Gasthaus zur Pyramide\u0020aus Glas\u0009</tag>");
    }

    @Test
    public void testEmojiPassesThrough() throws Exception {
        // 🦆 DUCK emoji (U+1F986) — the original bug report
        final String name = "\uD83E\uDD86Alles f\u00FCr den Cache\uD83E\uDD86 Lab Bonus";
        final String htmlName = "<name>&#129414;Alles für den Cache&#129414; Lab Bonus</name>";
        XmlUtils.simpleText(xml, "", "name", name);
        assertXmlEquals(htmlName);
    }

    private void assertXmlEquals(final String expected) throws IOException {
        xml.endDocument();
        xml.flush();
        assertThat(stringWriter.toString()).isEqualTo("<?xml version='1.0' encoding='UTF-8' ?>" + expected);
    }
}
