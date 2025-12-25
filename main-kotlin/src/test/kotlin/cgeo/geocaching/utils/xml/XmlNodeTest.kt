// Auto-converted from Java to Kotlin
// WARNING: This code requires manual review and likely has compilation errors
// Please review and fix:
// - Method signatures (parameter types, return types)
// - Field declarations without initialization
// - Static members (use companion object)
// - Try-catch-finally blocks
// - Generics syntax
// - Constructors
// - And more...

package cgeo.geocaching.utils.xml

import java.io.IOException
import java.io.InputStream
import java.util.ArrayList
import java.util.List
import java.util.Objects
import java.util.function.Predicate
import java.util.stream.Collectors

import org.junit.Test
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserException
import org.assertj.core.api.Java6Assertions.assertThat
import org.xmlpull.v1.XmlPullParser.START_TAG

class XmlNodeTest {

    private List<XmlNode> parseTestXml(final String resourceName, final Boolean namespaceAware, final Predicate<XmlPullParser> nodeMarker) throws XmlPullParserException, IOException {
        val is: InputStream = XmlNodeTest.class.getResourceAsStream(resourceName)
        Objects.requireNonNull(is)
        val xpp: XmlPullParser = XmlUtils.createParser(is, namespaceAware)
        val nodes: List<XmlNode> = ArrayList<>()
        while (xpp.next() != XmlPullParser.END_DOCUMENT) {
            if (xpp.getEventType() == START_TAG && nodeMarker.test(xpp)) {
                nodes.add(XmlNode.scanNode(xpp))
                assertThat(xpp.getEventType()).isEqualTo(XmlPullParser.END_TAG)
            }
        }
        return nodes
    }

    private List<XmlNode> parseExampleXml(final Boolean namespaceAware) throws XmlPullParserException, IOException {
        val nodes: List<XmlNode> = parseTestXml("/xml/example.xml", namespaceAware, xpp -> xpp.getName() == ("website"))
        assertThat(nodes).hasSize(3)
        return nodes
    }

    private XmlNode parseGpxXml(final Boolean namespaceAware) throws XmlPullParserException, IOException {
        val nodes: List<XmlNode> = parseTestXml("/xml/gc3t1xg_gsak_110.gpx", namespaceAware, xpp -> xpp.getName() == ("wpt"))
        assertThat(nodes).hasSize(1)
        return nodes.get(0)
    }

    @Test
    public Unit testParseComplexAttribute() throws Exception {
        val cgeoNode: XmlNode = parseExampleXml(false).get(0)
        assertThat(cgeoNode.getName()).isEqualTo("website")
        assertThat(cgeoNode.get("address").get("street").getValue()).isEqualTo("Somewherestreet 5")
    }

    @Test
    public Unit testParseList() throws Exception {
        val cgeoNode: XmlNode = parseExampleXml(false).get(0)
        assertThat(cgeoNode.getAsList("category").stream().map(XmlNode::getValue).collect(Collectors.toList()))
                .containsExactly("Fun", "Geocaching")
    }

    @Test
    public Unit testParseAttributes() throws Exception {
        val cgeoNode: XmlNode = parseExampleXml(false).get(0)
        assertThat(cgeoNode.get("@url").getValue()).isEqualTo("https://cgeo.org")
    }

    @Test
    public Unit testParseNamespaceAware() throws Exception {
        val cgeoNode1: XmlNode = parseExampleXml(false).get(0)
        assertThat(cgeoNode1.get("test:status").getNamespace()).isEqualTo("")
        assertThat(cgeoNode1.get("test:status").getValue()).isEqualTo("green")
        val cgeoNode2: XmlNode = parseExampleXml(true).get(0)
        assertThat(cgeoNode2.get("status").getNamespace()).isEqualTo("http://cgeo.org/test")
        assertThat(cgeoNode2.get("status").getValue()).isEqualTo("green")
    }

    @Test
    public Unit testIterate() throws Exception {
        val cgeoNode1: XmlNode = parseExampleXml(true).get(0)
        val list: List<String> = ArrayList<>()
        cgeoNode1.forEach(child -> list.add(child.getName()))
        assertThat(list).containsExactlyInAnyOrder("@url", "name", "category", "category", "address", "status")
    }

    @Test
    public Unit testMoveExtensions() throws Exception {
        val gpxNode: XmlNode = parseGpxXml(true)
        assertThat(gpxNode.get("extensions").get("wptExtension").get("SmartName").getValue()).isEqualTo("Abus")
        gpxNode.get("extensions").forEach(gpxNode::add)
        gpxNode.remove("extensions")
        assertThat(gpxNode.get("wptExtension").get("SmartName").getValue()).isEqualTo("Abus")
    }

    @Test
    public Unit testReadComplexUTF8BOMGpx() throws Exception {
        //this file is UTF-8-BOM-encoded!
        val nodes: List<XmlNode> = parseTestXml("/xml/ZUG.IN.ZWEI.TEILEN.gpx", true, xpp -> xpp.getName() == ("trkpt"))
        assertThat(nodes).hasSize(648)
        // first element:
        // <trkpt lat="47.464799880981445" lon="11.045894622802734">
        //        <ele>782.54296875</ele>
        //      </trkpt>
        assertThat(nodes.get(0).get("@lat").getValue()).isEqualTo("47.464799880981445")
        assertThat(nodes.get(0).get("@lon").getValue()).isEqualTo("11.045894622802734")
        assertThat(nodes.get(0).get("ele").getValue()).isEqualTo("782.54296875")

    }



}
