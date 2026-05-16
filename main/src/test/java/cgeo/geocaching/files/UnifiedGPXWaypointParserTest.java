package cgeo.geocaching.files;

import cgeo.geocaching.enumerations.CacheSize;
import cgeo.geocaching.enumerations.CacheType;
import cgeo.geocaching.models.Geocache;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import org.junit.Test;
import static org.assertj.core.api.Assertions.offset;
import static org.assertj.core.api.Java6Assertions.assertThat;

/**
 * Tests for {@link UnifiedGPXWaypointParser} driven through the public
 * {@link UnifiedGPXParser#parse} entry point, since the wpt parser is package-private
 * and only invoked from there.
 */
public class UnifiedGPXWaypointParserTest {

    private static final String GPX11_HEAD = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
            + "<gpx version=\"1.1\" creator=\"test\""
            + " xmlns=\"http://www.topografix.com/GPX/1/1\""
            + " xmlns:groundspeak=\"http://www.groundspeak.com/cache/1/0/1\""
            + " xmlns:gsak=\"http://www.gsak.net/xmlv1/6\""
            + " xmlns:cgeo=\"http://www.cgeo.org/wptext/1/0\""
            + " xmlns:oc=\"https://github.com/opencaching/gpx-extension-v1\""
            + " xmlns:terra=\"http://www.TerraCaching.com/GPX/1/0\">";
    private static final String GPX10_HEAD = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
            + "<gpx version=\"1.0\" creator=\"test\""
            + " xmlns=\"http://www.topografix.com/GPX/1/0\""
            + " xmlns:groundspeak=\"http://www.groundspeak.com/cache/1/0/1\">";
    private static final String GPX_FOOT = "</gpx>";

    private static UnifiedGPXParser.Result parse(final String xml) throws Exception {
        return UnifiedGPXParser.parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
    }

    private static Geocache firstWaypoint(final UnifiedGPXParser.Result result) {
        assertThat(result.waypoints).isNotEmpty();
        return result.waypoints.iterator().next();
    }

    // --- baseline ---------------------------------------------------------

    @Test
    public void barewptIsAcceptedAsGeocache() throws Exception {
        final UnifiedGPXParser.Result result = parse(GPX11_HEAD
                + "<wpt lat=\"48.5\" lon=\"9.25\"><name>GC1234</name></wpt>"
                + GPX_FOOT);
        final Geocache cache = firstWaypoint(result);
        assertThat(cache.getName()).isEqualTo("GC1234");
        assertThat(cache.getGeocode()).isEqualTo("GC1234");
        assertThat(cache.getCoords().getLatitude()).isEqualTo(48.5, offset(1e-9));
    }

    @Test
    public void wptWithSentinelZeroCoordsHasNoCoords() throws Exception {
        // GPX export uses (0,0) as a placeholder for "no coordinates"
        final UnifiedGPXParser.Result result = parse(GPX11_HEAD
                + "<wpt lat=\"0\" lon=\"0\"><name>GC1234</name></wpt>"
                + GPX_FOOT);
        // No coords + no internal connector => not imported as cache
        assertThat(result.waypoints).isEmpty();
    }

    // --- Groundspeak ------------------------------------------------------

    @Test
    public void groundspeakCoreFieldsPopulateGeocache() throws Exception {
        final UnifiedGPXParser.Result result = parse(GPX11_HEAD
                + "<wpt lat=\"48\" lon=\"9\">"
                + "  <name>GC9999</name>"
                + "  <sym>Geocache</sym>"
                + "  <type>Geocache|Traditional Cache</type>"
                + "  <extensions>"
                + "    <groundspeak:cache id=\"42\" archived=\"True\" available=\"False\">"
                + "      <groundspeak:name>Cool Cache</groundspeak:name>"
                + "      <groundspeak:placed_by>cacher123</groundspeak:placed_by>"
                + "      <groundspeak:owner>OwnerId</groundspeak:owner>"
                + "      <groundspeak:type>Traditional Cache</groundspeak:type>"
                + "      <groundspeak:container>Small</groundspeak:container>"
                + "      <groundspeak:difficulty>2.5</groundspeak:difficulty>"
                + "      <groundspeak:terrain>3.0</groundspeak:terrain>"
                + "      <groundspeak:country>Germany</groundspeak:country>"
                + "      <groundspeak:state>Baden-Württemberg</groundspeak:state>"
                + "      <groundspeak:encoded_hints>Pgvyu cnegva</groundspeak:encoded_hints>"
                + "      <groundspeak:short_description>Short text</groundspeak:short_description>"
                + "      <groundspeak:long_description>Long story</groundspeak:long_description>"
                + "    </groundspeak:cache>"
                + "  </extensions>"
                + "</wpt>"
                + GPX_FOOT);

        final Geocache cache = firstWaypoint(result);
        assertThat(cache.getName()).isEqualTo("Cool Cache");
        assertThat(cache.getCacheId()).isEqualTo("42");
        assertThat(cache.isArchived()).isTrue();
        assertThat(cache.isDisabled()).isTrue(); // available=False → disabled
        assertThat(cache.getOwnerDisplayName()).isEqualTo("cacher123");
        assertThat(cache.getOwnerUserId()).isEqualTo("OwnerId");
        assertThat(cache.getType()).isEqualTo(CacheType.TRADITIONAL);
        assertThat(cache.getSize()).isEqualTo(CacheSize.SMALL);
        assertThat(cache.getDifficulty()).isEqualTo(2.5f, offset(1e-6f));
        assertThat(cache.getTerrain()).isEqualTo(3.0f, offset(1e-6f));
        assertThat(cache.getLocation()).contains("Baden-Württemberg").contains("Germany");
        assertThat(cache.getHint()).isEqualTo("Pgvyu cnegva");
        assertThat(cache.getShortDescription()).isEqualTo("Short text");
        assertThat(cache.getDescription()).isEqualTo("Long story");
    }

    @Test
    public void groundspeakLogsAreReturnedKeyedByGeocode() throws Exception {
        final UnifiedGPXParser.Result result = parse(GPX11_HEAD
                + "<wpt lat=\"48\" lon=\"9\">"
                + "  <name>GC9999</name>"
                + "  <extensions>"
                + "    <groundspeak:cache>"
                + "      <groundspeak:logs>"
                + "        <groundspeak:log id=\"1\">"
                + "          <groundspeak:date>2024-01-15T10:30:00Z</groundspeak:date>"
                + "          <groundspeak:type>Found it</groundspeak:type>"
                + "          <groundspeak:finder>finder1</groundspeak:finder>"
                + "          <groundspeak:text>nice hide</groundspeak:text>"
                + "        </groundspeak:log>"
                + "        <groundspeak:log id=\"2\">"
                + "          <groundspeak:date>2024-01-16T10:30:00Z</groundspeak:date>"
                + "          <groundspeak:type>Didn't find it</groundspeak:type>"
                + "          <groundspeak:finder>finder2</groundspeak:finder>"
                + "          <groundspeak:text>could not locate</groundspeak:text>"
                + "        </groundspeak:log>"
                + "      </groundspeak:logs>"
                + "    </groundspeak:cache>"
                + "  </extensions>"
                + "</wpt>"
                + GPX_FOOT);

        assertThat(result.logsByGeocode).hasSize(1);
        assertThat(result.logsByGeocode.get("GC9999")).hasSize(2);
        assertThat(result.logsByGeocode.get("GC9999").get(0).author).isEqualTo("finder1");
        assertThat(result.logsByGeocode.get("GC9999").get(1).author).isEqualTo("finder2");
    }

    @Test
    public void groundspeakTravelbugsAttachToInventory() throws Exception {
        final UnifiedGPXParser.Result result = parse(GPX11_HEAD
                + "<wpt lat=\"48\" lon=\"9\">"
                + "  <name>GC9999</name>"
                + "  <extensions>"
                + "    <groundspeak:cache>"
                + "      <groundspeak:travelbugs>"
                + "        <groundspeak:travelbug ref=\"TB1234\">"
                + "          <groundspeak:name>My TB</groundspeak:name>"
                + "        </groundspeak:travelbug>"
                + "      </groundspeak:travelbugs>"
                + "    </groundspeak:cache>"
                + "  </extensions>"
                + "</wpt>"
                + GPX_FOOT);

        final Geocache cache = firstWaypoint(result);
        assertThat(cache.getInventory()).hasSize(1);
        assertThat(cache.getInventory().get(0).getGeocode()).isEqualTo("TB1234");
        assertThat(cache.getInventory().get(0).getName()).isEqualTo("My TB");
    }

    // --- GSAK -------------------------------------------------------------

    @Test
    public void gsakBasicFlagsAndNote() throws Exception {
        final UnifiedGPXParser.Result result = parse(GPX11_HEAD
                + "<wpt lat=\"48\" lon=\"9\">"
                + "  <name>GC9999</name>"
                + "  <extensions>"
                + "    <gsak:wptExtension>"
                + "      <gsak:Watch>true</gsak:Watch>"
                + "      <gsak:FavPoints>42</gsak:FavPoints>"
                + "      <gsak:GcNote>my private note</gsak:GcNote>"
                + "      <gsak:IsPremium>true</gsak:IsPremium>"
                + "    </gsak:wptExtension>"
                + "  </extensions>"
                + "</wpt>"
                + GPX_FOOT);

        final Geocache cache = firstWaypoint(result);
        assertThat(cache.isOnWatchlist()).isTrue();
        assertThat(cache.getFavoritePoints()).isEqualTo(42);
        assertThat(cache.getPersonalNote()).isEqualTo("my private note");
        assertThat(cache.isPremiumMembersOnly()).isTrue();
    }

    @Test
    public void gsakGeocodeOverrideTakesPrecedence() throws Exception {
        final UnifiedGPXParser.Result result = parse(GPX11_HEAD
                + "<wpt lat=\"48\" lon=\"9\">"
                + "  <name>SomeGarbageName</name>"
                + "  <extensions>"
                + "    <gsak:wptExtension>"
                + "      <gsak:Code>GCABC12</gsak:Code>"
                + "    </gsak:wptExtension>"
                + "  </extensions>"
                + "</wpt>"
                + GPX_FOOT);

        final Geocache cache = firstWaypoint(result);
        assertThat(cache.getGeocode()).isEqualTo("GCABC12");
    }

    @Test
    public void gsakUserDataPopulatesPersonalNoteWhenNoGcNote() throws Exception {
        final UnifiedGPXParser.Result result = parse(GPX11_HEAD
                + "<wpt lat=\"48\" lon=\"9\">"
                + "  <name>GC9999</name>"
                + "  <extensions>"
                + "    <gsak:wptExtension>"
                + "      <gsak:UserData>line1</gsak:UserData>"
                + "      <gsak:User2>line2</gsak:User2>"
                + "      <gsak:User4>line4</gsak:User4>"
                + "    </gsak:wptExtension>"
                + "  </extensions>"
                + "</wpt>"
                + GPX_FOOT);

        final Geocache cache = firstWaypoint(result);
        // userData1..4 collapse to personal note when GcNote not set
        assertThat(cache.getPersonalNote()).contains("line1").contains("line2").contains("line4");
    }

    @Test
    public void gsakOriginalCoordsCreateOriginalWaypoint() throws Exception {
        final UnifiedGPXParser.Result result = parse(GPX11_HEAD
                + "<wpt lat=\"48.6\" lon=\"9.4\">"
                + "  <name>GC9999</name>"
                + "  <extensions>"
                + "    <gsak:wptExtension>"
                + "      <gsak:LatBeforeCorrect>48.5</gsak:LatBeforeCorrect>"
                + "      <gsak:LonBeforeCorrect>9.3</gsak:LonBeforeCorrect>"
                + "    </gsak:wptExtension>"
                + "  </extensions>"
                + "</wpt>"
                + GPX_FOOT);

        final Geocache cache = firstWaypoint(result);
        assertThat(cache.hasUserModifiedCoords()).isTrue();
        assertThat(cache.getWaypoints()).hasSize(1);
        assertThat(cache.getWaypoints().get(0).getCoords().getLatitude())
                .isEqualTo(48.5, offset(1e-9));
    }

    // --- c:geo ------------------------------------------------------------

    @Test
    public void cgeoAssignedEmoji() throws Exception {
        final UnifiedGPXParser.Result result = parse(GPX11_HEAD
                + "<wpt lat=\"48\" lon=\"9\">"
                + "  <name>GC9999</name>"
                + "  <extensions>"
                + "    <cgeo:cacheExtension>"
                + "      <cgeo:assignedEmoji>128512</cgeo:assignedEmoji>"
                + "    </cgeo:cacheExtension>"
                + "  </extensions>"
                + "</wpt>"
                + GPX_FOOT);
        assertThat(firstWaypoint(result).getAssignedEmoji()).isEqualTo(128512);
    }

    // --- OpenCaching ------------------------------------------------------

    @Test
    public void openCachingRequiresPasswordFlag() throws Exception {
        final UnifiedGPXParser.Result result = parse(GPX11_HEAD
                + "<wpt lat=\"48\" lon=\"9\">"
                + "  <name>OC1234</name>"
                + "  <extensions>"
                + "    <oc:cache>"
                + "      <oc:requires_password>true</oc:requires_password>"
                + "    </oc:cache>"
                + "  </extensions>"
                + "</wpt>"
                + GPX_FOOT);
        assertThat(firstWaypoint(result).isLogPasswordRequired()).isTrue();
    }

    // --- GPX 1.0 vs 1.1 extension placement -------------------------------

    @Test
    public void gpx10ExtensionsAsDirectChildren() throws Exception {
        // GPX 1.0: <groundspeak:cache> is a direct child of <wpt>, not under <extensions>.
        final UnifiedGPXParser.Result result = parse(GPX10_HEAD
                + "<wpt lat=\"48\" lon=\"9\">"
                + "  <name>GC9999</name>"
                + "  <groundspeak:cache id=\"42\">"
                + "    <groundspeak:name>From 1.0</groundspeak:name>"
                + "    <groundspeak:difficulty>4</groundspeak:difficulty>"
                + "  </groundspeak:cache>"
                + "</wpt>"
                + GPX_FOOT);

        final Geocache cache = firstWaypoint(result);
        assertThat(cache.getName()).isEqualTo("From 1.0");
        assertThat(cache.getDifficulty()).isEqualTo(4f, offset(1e-6f));
    }

    // --- child waypoints --------------------------------------------------

    @Test
    public void childWaypointAttachesToInFileParent() throws Exception {
        final UnifiedGPXParser.Result result = parse(GPX11_HEAD
                + "<wpt lat=\"48\" lon=\"9\">"
                + "  <name>GC9999</name>"
                + "  <desc>Main Cache</desc>"
                + "  <sym>Geocache</sym>"
                + "  <type>Geocache|Traditional Cache</type>"
                + "</wpt>"
                + "<wpt lat=\"48.1\" lon=\"9.1\">"
                + "  <name>P19999</name>"           // parking; "P1" prefix + parent suffix
                + "  <desc>Parking</desc>"
                + "  <sym>Parking Area</sym>"
                + "  <type>Waypoint|Parking Area</type>"
                + "</wpt>"
                + GPX_FOOT);

        // The parking wpt should be attached to GC9999 (parent code derived as "GC" + name.substring(2))
        assertThat(result.waypoints).hasSize(1);
        assertThat(result.orphanWaypoints).isEmpty();
        final Geocache parent = firstWaypoint(result);
        assertThat(parent.getWaypoints()).hasSize(1);
        assertThat(parent.getWaypoints().get(0).getName()).isEqualTo("Parking");
    }

    @Test
    public void childWaypointWithoutInFileParentBecomesOrphan() throws Exception {
        final UnifiedGPXParser.Result result = parse(GPX11_HEAD
                + "<wpt lat=\"48.1\" lon=\"9.1\">"
                + "  <name>P19999</name>"
                + "  <desc>Parking</desc>"
                + "  <sym>Parking Area</sym>"
                + "  <type>Waypoint|Parking Area</type>"
                + "</wpt>"
                + GPX_FOOT);
        // The parent ("GC9999") is not in the file → orphan
        assertThat(result.waypoints).isEmpty();
        assertThat(result.orphanWaypoints).hasSize(1);
        assertThat(result.orphanWaypoints.get(0).parentGeocode).isEqualTo("GC9999");
        assertThat(result.orphanWaypoints.get(0).waypoint.getName()).isEqualTo("Parking");
    }

    @Test
    public void childWaypointUsesGsakParentWhenPresent() throws Exception {
        final UnifiedGPXParser.Result result = parse(GPX11_HEAD
                + "<wpt lat=\"48.1\" lon=\"9.1\">"
                + "  <name>SomeArbitraryName</name>"
                + "  <desc>Stage</desc>"
                + "  <type>Waypoint|Stages of a multicache</type>"
                + "  <extensions>"
                + "    <gsak:wptExtension>"
                + "      <gsak:Parent>GCEXPLICIT</gsak:Parent>"
                + "    </gsak:wptExtension>"
                + "  </extensions>"
                + "</wpt>"
                + GPX_FOOT);

        assertThat(result.orphanWaypoints).hasSize(1);
        assertThat(result.orphanWaypoints.get(0).parentGeocode).isEqualTo("GCEXPLICIT");
    }

    // --- mixed ------------------------------------------------------------

    @Test
    public void waypointsRoutesAndTracksCoexist() throws Exception {
        final UnifiedGPXParser.Result result = parse(GPX11_HEAD
                + "<wpt lat=\"1\" lon=\"1\"><name>GC1111</name></wpt>"
                + "<rte><rtept lat=\"2\" lon=\"2\"/></rte>"
                + "<trk><trkseg><trkpt lat=\"3\" lon=\"3\"/></trkseg></trk>"
                + GPX_FOOT);
        assertThat(result.waypoints).hasSize(1);
        assertThat(result.routes).hasSize(1);
        assertThat(result.tracks).hasSize(1);
    }

    // --- found state from sym --------------------------------------------

    @Test
    public void symGeocacheFoundMarksCacheFound() throws Exception {
        final UnifiedGPXParser.Result result = parse(GPX11_HEAD
                + "<wpt lat=\"48\" lon=\"9\">"
                + "  <name>GC9999</name>"
                + "  <sym>Geocache Found</sym>"
                + "</wpt>"
                + GPX_FOOT);
        assertThat(firstWaypoint(result).isFound()).isTrue();
    }
}
