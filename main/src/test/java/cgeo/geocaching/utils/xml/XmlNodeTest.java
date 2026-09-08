package cgeo.geocaching.utils.xml;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.junit.Test;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;
import static org.xmlpull.v1.XmlPullParser.START_TAG;

public class XmlNodeTest {

    private List<XmlNode> parseTestXml(final String resourceName, final boolean namespaceAware, final boolean relaxed, final Predicate<XmlPullParser> nodeMarker) throws XmlPullParserException, IOException {
        final InputStream is = XmlNodeTest.class.getResourceAsStream(resourceName);
        Objects.requireNonNull(is);
        final XmlPullParser xpp = XmlUtils.createParser(is, namespaceAware, relaxed, "UTF-8");
        final List<XmlNode> nodes = new ArrayList<>();
        while (xpp.next() != XmlPullParser.END_DOCUMENT) {
            if (xpp.getEventType() == START_TAG && nodeMarker.test(xpp)) {
                nodes.add(XmlNode.scanNode(xpp));
                assertThat(xpp.getEventType()).isEqualTo(XmlPullParser.END_TAG);
            }
        }
        return nodes;
    }

    private List<XmlNode> parseExampleXml(final boolean namespaceAware) throws XmlPullParserException, IOException {
        final List<XmlNode> nodes = parseTestXml("/xml/example.xml", namespaceAware, false, xpp -> xpp.getName().equals("website"));
        assertThat(nodes).hasSize(3);
        return nodes;
    }

    private XmlNode parseGpxXml(final boolean namespaceAware) throws XmlPullParserException, IOException {
        final List<XmlNode> nodes = parseTestXml("/xml/gc3t1xg_gsak_110.gpx", namespaceAware, false, xpp -> xpp.getName().equals("wpt"));
        assertThat(nodes).hasSize(1);
        return nodes.get(0);
    }

    @Test
    public void testParseComplexAttribute() throws Exception {
        final XmlNode cgeoNode = parseExampleXml(false).get(0);
        assertThat(cgeoNode.getLocalName()).isEqualTo("website");
        assertThat(cgeoNode.getChild("address").getChild("street").getValue()).isEqualTo("Somewherestreet 5");
    }

    @Test
    public void testParseList() throws Exception {
        final XmlNode cgeoNode = parseExampleXml(false).get(0);
        assertThat(cgeoNode.getChildrenAsList("category").stream().map(XmlNode::getValue).collect(Collectors.toList()))
                .containsExactly("Fun", "Geocaching");
    }

    @Test
    public void testParseAttributes() throws Exception {
        final XmlNode cgeoNode = parseExampleXml(false).get(0);
        assertThat(cgeoNode.getChild("@url").getValue()).isEqualTo("https://cgeo.org");
    }

    @Test
    public void testParseNamespaceAware() throws Exception {
        final XmlNode cgeoNode1 = parseExampleXml(false).get(0);
        assertThat(cgeoNode1.getChild("status").getNamespace()).isEqualTo("");
        assertThat(cgeoNode1.getChild("status").getValue()).isEqualTo("green");
        final XmlNode cgeoNode2 = parseExampleXml(true).get(0);
        assertThat(cgeoNode2.getChild("status").getNamespace()).isEqualTo("http://cgeo.org/test");
        assertThat(cgeoNode2.getChild("status").getValue()).isEqualTo("green");
    }

    @Test
    public void testIterate() throws Exception {
        final XmlNode cgeoNode1 = parseExampleXml(true).get(0);
        final List<String> list = new ArrayList<>();
        cgeoNode1.forEach(child -> list.add(child.getLocalName()));
        assertThat(list).containsExactlyInAnyOrder("@url", "name", "category", "category", "address", "status");
    }

    @Test
    public void testMoveExtensions() throws Exception {
        final XmlNode gpxNode = parseGpxXml(true);
        assertThat(gpxNode.getChild("extensions").getChild("wptExtension").getChild("SmartName").getValue()).isEqualTo("Abus");
        gpxNode.getChild("extensions").forEach(gpxNode::addChild);
        gpxNode.removeChild("extensions");
        assertThat(gpxNode.getChild("wptExtension").getChild("SmartName").getValue()).isEqualTo("Abus");
    }

    @Test
    public void testReadComplexUTF8BOMGpx() throws Exception {
        //this file is UTF-8-BOM-encoded!
        final List<XmlNode> nodes = parseTestXml("/xml/ZUG.IN.ZWEI.TEILEN.gpx", true, false, xpp -> xpp.getName().equals("trkpt"));
        assertThat(nodes).hasSize(648);
        // first element:
        // <trkpt lat="47.464799880981445" lon="11.045894622802734">
        //        <ele>782.54296875</ele>
        //      </trkpt>
        assertThat(nodes.get(0).getChild("@lat").getValue()).isEqualTo("47.464799880981445");
        assertThat(nodes.get(0).getChild("@lon").getValue()).isEqualTo("11.045894622802734");
        assertThat(nodes.get(0).getChild("ele").getValue()).isEqualTo("782.54296875");

    }

    @Test
    public void testRelaxationAndNamespaces() throws Exception {
        try {
            parseTestXml("/xml/example_invalid.xml", false, false, xpp -> xpp.getName().equals("website"));
            fail("Expected XmlPullParserException");
        } catch (final XmlPullParserException e) {
            // expected because XML is invalid
        }

        //without namespaces
        final XmlNode cgeoNode = parseTestXml("/xml/example_invalid.xml", false, true, xpp -> xpp.getName().equals("website")).get(0);
        assertThat(cgeoNode.getChild("status").getNamespace()).isEqualTo("");
        assertThat(cgeoNode.getChild("status").getValue()).isEqualTo("green");
        assertThat(cgeoNode.getChild("status2").getValue()).isEqualTo("red");
        assertThat(cgeoNode.getChild("test").getValue()).isEqualTo("test");
        assertThat(cgeoNode.getChild("unclosed").getValue()).startsWith("notclosed");
        //with namespaces
        final XmlNode cgeoNode1 = parseTestXml("/xml/example_invalid.xml", true, true, xpp -> xpp.getName().equals("website")).get(0);
        assertThat(cgeoNode1.getChild("status").getNamespace()).isEqualTo("http://cgeo.org/test");
        assertThat(cgeoNode1.getChild("status").getValue()).isEqualTo("green");
        assertThat(cgeoNode1.getChild("status2").getValue()).isEqualTo("red");
        assertThat(cgeoNode1.getChild("test").getValue()).isEqualTo("test");
        assertThat(cgeoNode1.getChild("test").getNamespace()).isEqualTo(""); // undeclared namespace is empty
        assertThat(cgeoNode1.getChild("unclosed").getValue()).startsWith("notclosed");

    }



}
