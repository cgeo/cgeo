package cgeo.geocaching.utils.xml;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.junit.Test;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.xmlpull.v1.XmlPullParser.START_TAG;

public class XmlNodeTest {

    private List<XmlNode> parseTestXml(final String resourceName, final boolean namespaceAware, final Predicate<XmlPullParser> nodeMarker) throws XmlPullParserException, IOException {
        final InputStream is = XmlNodeTest.class.getResourceAsStream(resourceName);
        final XmlPullParser xpp = XmlUtils.createParser(new InputStreamReader(is, StandardCharsets.UTF_8), namespaceAware);
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
        final List<XmlNode> nodes = parseTestXml("/xml/example.xml", namespaceAware, xpp -> xpp.getName().equals("website"));
        assertThat(nodes).hasSize(3);
        return nodes;
    }

    private XmlNode parseGpxXml(final boolean namespaceAware) throws XmlPullParserException, IOException {
        final List<XmlNode> nodes = parseTestXml("/xml/gc3t1xg_gsak_110.gpx", namespaceAware, xpp -> xpp.getName().equals("wpt"));
        assertThat(nodes).hasSize(1);
        return nodes.get(0);
    }

    @Test
    public void testParseComplexAttribute() throws Exception {
        final XmlNode cgeoNode = parseExampleXml(false).get(0);
        assertThat(cgeoNode.getName()).isEqualTo("website");
        assertThat(cgeoNode.get("address").get("street").getValue()).isEqualTo("Somewherestreet 5");
    }

    @Test
    public void testParseList() throws Exception {
        final XmlNode cgeoNode = parseExampleXml(false).get(0);
        assertThat(cgeoNode.getAsList("category").stream().map(XmlNode::getValue).collect(Collectors.toList()))
                .containsExactly("Fun", "Geocaching");
    }

    @Test
    public void testParseAttributes() throws Exception {
        final XmlNode cgeoNode = parseExampleXml(false).get(0);
        assertThat(cgeoNode.get("@url").getValue()).isEqualTo("https://cgeo.org");
    }

    @Test
    public void testParseNamespaceAware() throws Exception {
        final XmlNode cgeoNode1 = parseExampleXml(false).get(0);
        assertThat(cgeoNode1.get("test:status").getNamespace()).isEqualTo("");
        assertThat(cgeoNode1.get("test:status").getValue()).isEqualTo("green");
        final XmlNode cgeoNode2 = parseExampleXml(true).get(0);
        assertThat(cgeoNode2.get("status").getNamespace()).isEqualTo("http://cgeo.org/test");
        assertThat(cgeoNode2.get("status").getValue()).isEqualTo("green");
    }

    @Test
    public void testIterate() throws Exception {
        final XmlNode cgeoNode1 = parseExampleXml(true).get(0);
        final List<String> list = new ArrayList<>();
        cgeoNode1.forEach(child -> list.add(child.getName()));
        assertThat(list).containsExactlyInAnyOrder("@url", "name", "category", "category", "address", "status");
    }

    @Test
    public void testMoveExtensions() throws Exception {
        final XmlNode gpxNode = parseGpxXml(true);
        assertThat(gpxNode.get("extensions").get("wptExtension").get("SmartName").getValue()).isEqualTo("Abus");
        gpxNode.get("extensions").forEach(gpxNode::add);
        gpxNode.remove("extensions");
        assertThat(gpxNode.get("wptExtension").get("SmartName").getValue()).isEqualTo("Abus");
    }



}
