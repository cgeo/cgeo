package cgeo.geocaching.utils.gpx;

import cgeo.geocaching.connector.gc.GCUtils;
import cgeo.geocaching.enumerations.WaypointType;
import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.log.LogEntry;
import cgeo.geocaching.log.LogType;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.models.Waypoint;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.TimeZone;

import org.junit.Test;
import static org.assertj.core.api.Assertions.assertThat;

public class GPXParserCompatibilityTest {

    static String document(final String body) {
        return "<gpx version=\"1.0\" xmlns=\"http://www.topografix.com/GPX/1/0\""
                + " xmlns:gs=\"http://www.groundspeak.com/cache/1/0\" xmlns:gsak=\"http://www.gsak.net/xmlv1/6\""
                + " xmlns:cgeo=\"http://www.cgeo.org/wptext/1/0\" xmlns:oc=\"https://github.com/opencaching/gpx-extension-v1\""
                + " xmlns:tc=\"http://www.TerraCaching.com/GPX/1/0\">" + body + "</gpx>";
    }

    static RecordingGPXParseHooks parse(final String xml, final GPXParser parser) throws Exception {
        return parse(xml, parser, new RecordingGPXParseHooks());
    }

    static RecordingGPXParseHooks parse(final String xml, final GPXParser parser, final RecordingGPXParseHooks hooks) throws Exception {
        try (InputStream stream = new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8))) {
            parser.parse(stream, hooks);
        }
        return hooks;
    }

    private static String cache(final String name, final String fields) {
        return "<wpt lat=\"48\" lon=\"9\"><name>" + name + "</name><sym>Geocache</sym>" + fields + "</wpt>";
    }

    private static Geocache parseCache(final String name, final String fields) throws Exception {
        return parse(document(cache(name, fields)), new GPXParser()).getGeocaches().get(0);
    }

    @Test
    public void testFoundSymbolOverridesDnf() throws Exception {
        final String found = "<wpt><name>GC12345</name><sym>Geocache Found</sym>"
                + "<gsak:wptExtension><gsak:DNF>true</gsak:DNF></gsak:wptExtension></wpt>";
        final List<Geocache> caches = parse(document(found + cache("GC23456", "")), new GPXParser()).getGeocaches();
        assertThat(caches).hasSize(2);
        assertThat(caches.get(0).isFound()).isTrue();
        assertThat(caches.get(0).isDNF()).isFalse();
        assertThat(caches.get(0).getCoords()).isNull();
        assertThat(caches.get(1).isFound()).isFalse();
    }

    @Test
    public void testOriginalCoordinatesFromGsakInBothExtensionLayouts() throws Exception {
        final String extensions = "<gsak:wptExtension><gsak:LatBeforeCorrect>51.223032</gsak:LatBeforeCorrect>"
                + "<gsak:LonBeforeCorrect>6.026147</gsak:LonBeforeCorrect><gsak:Code>GC4PGTG</gsak:Code></gsak:wptExtension>";
        for (final boolean gpx11 : new boolean[] {false, true}) {
            final Geocache parsed = parseCache("Some name", gpx11 ? "<extensions>" + extensions + "</extensions>" : extensions);
            assertThat(parsed.hasUserModifiedCoords()).isTrue();
            assertThat(parsed.getCoords()).isEqualTo(new Geopoint(48, 9));
            assertThat(parsed.getWaypoints()).hasSize(1);
            final Waypoint original = parsed.getWaypoints().get(0);
            assertThat(original.getWaypointType()).isEqualTo(WaypointType.ORIGINAL);
            assertThat(original.getGeocode()).isEqualTo("GC4PGTG");
            assertThat(original.getCoords()).isEqualTo(new Geopoint(51.223032, 6.026147));
        }
        for (final String latitude : new String[] {"", "NaN", "invalid", "91"}) {
            final Geocache parsed = parseCache("GC12345", "<gsak:wptExtension><gsak:LatBeforeCorrect>" + latitude
                    + "</gsak:LatBeforeCorrect><gsak:LonBeforeCorrect>9</gsak:LonBeforeCorrect></gsak:wptExtension>");
            assertThat(parsed.getWaypoints()).isEmpty();
            assertThat(parsed.hasUserModifiedCoords()).isFalse();
        }
    }

    @Test
    public void testUrlsAndLinksRestoreGuidAndGeocode() throws Exception {
        final String guid = "9946f030-a514-46d8-a050-a60e92fd2e1a";
        final String url = "https://www.geocaching.com/seek/cache_details.aspx?wp=GC31J2H&amp;guid=" + guid + "&amp;other=1";
        for (final String field : new String[] {"<url>" + url + "</url>", "<link href=\"" + url + "\"/>", "<link><href>" + url + "</href></link>"}) {
            final Geocache parsed = parseCache("Some title", field);
            assertThat(parsed.getGeocode()).isEqualTo("GC31J2H");
            assertThat(parsed.getGuid()).isEqualTo(guid);
            assertThat(parsed.getCacheId()).isEqualTo("2406611");
        }
    }

    @Test
    public void testWaymarkTitlesFromUrlNameAndLinkText() throws Exception {
        for (final String title : new String[] {"<urlname>Roman water pipe</urlname>", "<link><text>Roman water pipe</text></link>"}) {
            final String point = "<wpt><name>WM7BM7</name><sym>Waymark</sym>" + title + "</wpt>";
            final Geocache parsed = parse(document(point), new GPXParser()).getGeocaches().get(0);
            assertThat(parsed.getName()).isEqualTo("Roman water pipe");
        }
        assertThat(parseCache("WM7BM7", "<urlname>Fallback</urlname><gs:cache><gs:name>Explicit title</gs:name></gs:cache>").getName()).isEqualTo("Explicit title");
    }

    @Test
    public void testOpenCachingPasswordAndAlternativeListing() throws Exception {
        for (final boolean gpx11 : new boolean[] {false, true}) {
            final String extension = "<oc:cache><oc:requires_password>true</oc:requires_password><oc:other_code>GC12345</oc:other_code></oc:cache>";
            final Geocache parsed = parseCache("OC12345", "<cmt>Description</cmt>" + (gpx11 ? "<extensions>" + extension + "</extensions>" : extension));
            assertThat(parsed.isLogPasswordRequired()).isTrue();
            assertThat(parsed.getDescription()).contains("https://coord.info/GC12345").endsWith("Description");
        }
        assertThat(parseCache("OC12345", "<oc:cache><oc:requires_password>false</oc:requires_password></oc:cache>").isLogPasswordRequired()).isFalse();
    }

    @Test
    public void testDatesUseOffsetsAndConsumeEntireValue() throws Exception {
        final TimeZone previous = TimeZone.getDefault();
        try {
            TimeZone.setDefault(TimeZone.getTimeZone("GMT+02:00"));
            final long expected = Instant.parse("2011-11-07T07:00:00Z").toEpochMilli();
            for (final String date : new String[] {"2011-11-07T00:00:00.0000000-07:00", "2011-11-07T00:00:00-0700", "2011-11-07T07:00:00Z", "2011-11-07T08:00:00+01:00"}) {
                final String logs = "<gs:cache><gs:logs><gs:log><gs:type>Found it</gs:type><gs:date>" + date + "</gs:date></gs:log></gs:logs></gs:cache>";
                final RecordingGPXParseHooks hooks = parse(document(cache("GC12345", "<time>" + date + "</time>" + logs
                        + "<gsak:wptExtension><gsak:UserFound>" + date + "</gsak:UserFound></gsak:wptExtension>")), new GPXParser());
                assertThat(hooks.getGeocaches().get(0).getHiddenDate().getTime()).isEqualTo(expected);
                assertThat(hooks.getGeocaches().get(0).getVisitedDate()).isEqualTo(expected);
                assertThat(hooks.getGlobalItems().get(0).logs.get(0).date).isEqualTo(expected);
            }
            for (final String date : new String[] {"2011-11-07", "2011-11-07T00:00:00"}) {
                assertThat(parseCache("GC12345", "<time>" + date + "</time>").getHiddenDate().getTime())
                        .isEqualTo(Instant.parse("2011-11-06T22:00:00Z").toEpochMilli());
            }
            for (final String date : new String[] {"2011-11-07T00:00:00garbage", "2011-02-30", "2011-11-07T00:00:00+99:00"}) {
                assertThat(parseCache("GC12345", "<gsak:wptExtension><gsak:DNFDate>" + date + "</gsak:DNFDate></gsak:wptExtension>").getVisitedDate()).isZero();
            }
        } finally {
            TimeZone.setDefault(previous);
        }
    }

    @Test
    public void testCacheIdCorrectionIsLimitedToGeocachingCom() throws Exception {
        assertThat(parseCache("GC31J2H", "<gs:cache id=\"123456\"/>").getCacheId()).isEqualTo("2406611");
        assertThat(parseCache("GC31J2H", "").getCacheId()).isEqualTo("2406611");
        assertThat(parseCache("OC12345", "<gs:cache id=\"123456\"/>").getCacheId()).isEqualTo("123456");
    }

    @Test
    public void testLogsFromBothSourcesArePresentAndDiscardUnknownTypes() throws Exception {
        final String gs = "<gs:cache><gs:logs><gs:log id=\"12345\"><gs:type>Found it</gs:type></gs:log>"
                + "<gs:log><gs:type>Unknown value</gs:type></gs:log></gs:logs></gs:cache>";
        final String tc = "<tc:terracache><tc:logs><tc:log id=\"42\"><tc:type>Note</tc:type></tc:log>"
                + "<tc:log><tc:type>Unknown value</tc:type></tc:log></tc:logs></tc:terracache>";
        for (final boolean gsFirst : new boolean[] {false, true}) {
            final List<LogEntry> logs = parse(document(cache("GC12345", gsFirst ? gs + tc : tc + gs)), new GPXParser()).getGlobalItems().get(0).logs;
            assertThat(logs).extracting(log -> log.logType).containsExactlyInAnyOrder(gsFirst ? LogType.FOUND_IT : LogType.NOTE, gsFirst ? LogType.NOTE : LogType.FOUND_IT);
            assertThat(logs.get(0).serviceLogId).isEqualTo(GCUtils.logIdToLogCode(12345));
            assertThat(logs.get(1).serviceLogId).isNull();
        }
        assertThat(parse(document(cache("OC12345", gs)), new GPXParser()).getGlobalItems().get(0).logs.get(0).serviceLogId).isNull();
    }

    @Test
    public void testExtremcachingMetadataAndSessionReset() throws Exception {
        final String points = cache("GCEC12345", "") + "<wpt><name>00EC12345</name><type>Waypoint|Reference Point</type></wpt>";
        for (final String metadata : new String[] {"<url>https://extremcaching.com</url>", "<creator>extremcaching</creator>", "<metadata><link href=\"https://extremcaching.com\"/></metadata>"}) {
            final GPXParser parser = new GPXParser();
            final RecordingGPXParseHooks hooks = parse(document(metadata + points), parser);
            assertThat(hooks.getGeocaches().get(0).getGeocode()).isEqualTo("EC12345");
            assertThat(hooks.getWaypoints().get(0).getGeocode()).isEqualTo("EC12345");
            assertThat(parse(document(cache("GCEC12345", "")), parser).getGeocaches().get(0).getGeocode()).isEqualTo("GCEC12345");
        }
        assertThat(parse(document(points).replace("<gpx ", "<gpx creator=\"extremcaching\" "), new GPXParser()).getGeocaches().get(0).getGeocode()).isEqualTo("EC12345");
    }

    @Test
    public void testBlankExtensionTextClearsFallbackDescriptions() throws Exception {
        for (final String blank : new String[] {"", "  ", "nil"}) {
            final Geocache cache = parseCache("GC12345", "<desc>Short fallback</desc><cmt>Long fallback</cmt><gs:cache>"
                    + "<gs:short_description>" + blank + "</gs:short_description><gs:long_description>" + blank + "</gs:long_description></gs:cache>");
            assertThat(cache.getShortDescription()).isEmpty();
            assertThat(cache.getDescription()).isEmpty();
        }
        assertThat(parseCache("GC12345", "<desc>Keep me</desc><gs:cache/>").getShortDescription()).isEqualTo("Keep me");
    }

    @Test
    public void testTerraCachingMarkerDoesNotBecomeDescription() throws Exception {
        assertThat(parseCache("TC12345", "<desc>GC_WayPoint1</desc>").getShortDescription()).isEmpty();
    }

    @Test
    public void testGeocodeRecognitionKeepsLongAndUnknownIdentifiers() throws Exception {
        final String lab = "AL12345678-1234-1234-1234-123456789ABC_1234567890";
        assertThat(parseCache(lab, "").getGeocode()).isEqualTo(lab);
        assertThat(parseCache("Unknown title", "<desc>Find (" + lab.toLowerCase(java.util.Locale.US) + ") here</desc>").getGeocode()).isEqualTo(lab);
        assertThat(parseCache("ABCDE summit", "").getGeocode()).isEqualTo("ABCDE SUMMIT");
        assertThat(parseCache("12345678-1234-1234-1234-123456789ABC", "").getGeocode()).isEqualTo("12345678-1234-1234-1234-123456789ABC");
        assertThat(parseCache("Unknown title", "<desc>ABCDE (GC31J2H)</desc>").getGeocode()).isEqualTo("GC31J2H");
        assertThat(parseCache("Unknown title", "<desc>wordGC31J2Hword</desc>").getGeocode()).isEqualTo("UNKNOWN TITLE");
        assertThat(parseCache("Unknown title", "<desc>GC1</desc>").getGeocode()).isEqualTo("GC1");
        assertThat(parseCache("GC12345", "<url>https://coord.info/?wp=GC23456</url><gsak:wptExtension><gsak:Code>OC12345</gsak:Code></gsak:wptExtension>").getGeocode()).isEqualTo("OC12345");
    }

    @Test
    public void testCacheExtensionsClassifyEntriesWithoutTypeOrSymbol() throws Exception {
        for (final String extension : new String[] {"<gs:cache/>", "<tc:terracache/>", "<oc:cache/>"}) {
            assertThat(parse(document("<wpt><name>GC12345</name>" + extension + "</wpt>"), new GPXParser()).getGeocaches()).hasSize(1);
        }
    }

    @Test
    public void testParentTitleResolutionUsesOnlyCurrentSession() throws Exception {
        final GPXParser parser = new GPXParser();
        final String parent = cache("GC12345", "<gs:cache><gs:name>Parent title</gs:name></gs:cache>");
        final String child = "<wpt><name>AA12345</name><type>Waypoint|Reference Point</type>"
                + "<gsak:wptExtension><gsak:Parent> parent TITLE </gsak:Parent></gsak:wptExtension></wpt>";
        assertThat(parse(document(parent + child), parser).getWaypoints().get(0).getGeocode()).isEqualTo("GC12345");
        assertThat(parse(document(child), parser).getWaypoints().get(0).getGeocode()).isEqualTo("GC12345");
        parser.reset();
        assertThat(parse(document(child), parser).getGlobalItems().get(0).parentGeocode).isEqualTo("parent TITLE");
    }

    @Test
    public void testConflictingUserDefinedFlagsFollowDocumentOrder() throws Exception {
        final String gs = "<gsak:wptExtension><gsak:Child_ByGSAK>true</gsak:Child_ByGSAK></gsak:wptExtension>";
        final String cgeo = "<cgeo:userdefined>false</cgeo:userdefined>";
        for (final boolean gsFirst : new boolean[] {false, true}) {
            final String point = "<wpt><name>AA12345</name><type>Waypoint|Reference Point</type><cmt>Note</cmt><extensions>"
                    + (gsFirst ? gs + cgeo : cgeo + gs) + "</extensions></wpt>";
            final Waypoint parsed = parse(document(point), new GPXParser()).getWaypoints().get(0);
            assertThat(parsed.isUserDefined()).isEqualTo(!gsFirst);
            assertThat(parsed.getNote()).isEqualTo(gsFirst ? "Note" : "");
            assertThat(parsed.getUserNote()).isEqualTo(gsFirst ? "" : "Note");
            assertThat(parsed.isOriginalCoordsEmpty()).isEqualTo(gsFirst);
        }
    }

    @Test
    public void testInvalidXmlCharactersAndUtf8Bom() throws Exception {
        final String body = cache("GC12345", "<desc>A&#x1;B" + (char) 2 + "C&#3;D</desc>");
        assertThat(parse("\uFEFF" + document(body), new GPXParser()).getGeocaches().get(0).getShortDescription()).isEqualTo("ABCD");
    }
}
