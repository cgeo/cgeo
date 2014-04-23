package cgeo.geocaching.utils;

import cgeo.org.kxml2.io.KXmlSerializer;

import org.apache.commons.lang3.CharEncoding;
import org.xmlpull.v1.XmlSerializer;

import java.io.IOException;
import java.io.StringWriter;

import junit.framework.TestCase;

public class XmlUtilsTest extends TestCase {

    private XmlSerializer xml;

    private StringWriter stringWriter;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        stringWriter = new StringWriter();
        xml = new KXmlSerializer();
        xml.setOutput(stringWriter);
        xml.startDocument(CharEncoding.UTF_8, null);
    }

    public void testSimpleText() throws Exception {
        XmlUtils.simpleText(xml, "", "tag", "text");
        assertXmlEquals("<tag>text</tag>");
    }

    public void testSimpleTextWithPrefix() throws Exception {
        XmlUtils.simpleText(xml, "prefix", "tag", "text");
        assertXmlEquals("<n0:tag xmlns:n0=\"prefix\">text</n0:tag>");
    }

    private void assertXmlEquals(final String expected) throws IOException {
        xml.endDocument();
        xml.flush();
        assertEquals("<?xml version='1.0' encoding='UTF-8' ?>" + expected, stringWriter.toString());
    }

    public void testMultipleTexts() throws Exception {
        XmlUtils.multipleTexts(xml, "", "tag1", "text1", "tag2", "text2");
        assertXmlEquals("<tag1>text1</tag1><tag2>text2</tag2>");
    }

}
