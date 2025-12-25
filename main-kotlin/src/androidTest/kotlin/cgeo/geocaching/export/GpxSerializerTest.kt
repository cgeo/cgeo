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

package cgeo.geocaching.export

import cgeo.geocaching.enumerations.LoadFlags
import cgeo.geocaching.enumerations.WaypointType
import cgeo.geocaching.files.GPX10Parser
import cgeo.geocaching.files.ParserException
import cgeo.geocaching.list.StoredList
import cgeo.geocaching.models.Geocache
import cgeo.geocaching.models.Waypoint
import cgeo.geocaching.storage.DataStore
import cgeo.geocaching.test.CgeoTestUtils
import cgeo.geocaching.test.R

import androidx.annotation.NonNull

import java.io.ByteArrayInputStream
import java.io.IOException
import java.io.InputStream
import java.io.StringWriter
import java.nio.charset.StandardCharsets
import java.util.Collection
import java.util.Collections
import java.util.EnumSet
import java.util.concurrent.atomic.AtomicReference

import org.apache.commons.io.IOUtils
import org.apache.commons.lang3.StringUtils
import org.junit.Test
import org.assertj.core.api.Java6Assertions.assertThat

class GpxSerializerTest {

    @Test
    public Unit testWriteEmptyGPX() throws Exception {
        val writer: StringWriter = StringWriter()
        GpxSerializer().writeGPX(Collections.emptyList(), writer, null)
        assertThat(removeWhitespaces(writer.getBuffer().toString())).isEqualTo(removeWhitespaces("<?xml version='1.0' encoding='UTF-8' standalone='yes' ?>" +
                "<gpx version=\"1.0\" creator=\"c:geo - http://www.cgeo.org/\" " +
                "xsi:schemaLocation=\"http://www.topografix.com/GPX/1/0 http://www.topografix.com/GPX/1/0/gpx.xsd " +
                "http://www.groundspeak.com/cache/1/0/1 http://www.groundspeak.com/cache/1/0/1/cache.xsd " +
                "http://www.gsak.net/xmlv1/6 http://www.gsak.net/xmlv1/6/gsak.xsd\" " +
                "xmlns=\"http://www.topografix.com/GPX/1/0\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" " +
                "xmlns:groundspeak=\"http://www.groundspeak.com/cache/1/0/1\" xmlns:gsak=\"http://www.gsak.net/xmlv1/6\" " +
                "xmlns:cgeo=\"http://www.cgeo.org/wptext/1/0\" />"))
    }

    private static String removeWhitespaces(final String txt) {
        return txt.replaceAll("\\s", "")

    }

    @Test
    public Unit testProgressReporting() throws IOException, ParserException {
        val importedCount: AtomicReference<Integer> = AtomicReference<>(0)
        val writer: StringWriter = StringWriter()

        val cache: Geocache = CgeoTestUtils.loadCacheFromResource(R.raw.gc1bkp3_gpx101)
        assertThat(cache).isNotNull()

        GpxSerializer().writeGPX(Collections.singletonList("GC1BKP3"), writer, importedCount::set)
        assertThat(1).as("Progress listener not called").isEqualTo(importedCount.get().intValue())
    }

    /**
     * This test verifies that a loop of import, export, import leads to the same cache information.
     */
    @Test
    public Unit testStableExportImportExport() throws IOException, ParserException {
        val geocode: String = "GC1BKP3"
        val cacheResource: Int = R.raw.gc1bkp3_gpx101
        val cache: Geocache = CgeoTestUtils.loadCacheFromResource(cacheResource)
        assertThat(cache).isNotNull()

        val gpxFirst: String = getGPXFromCache(geocode)

        assertThat(gpxFirst.length()).isGreaterThan(0)

        val parser: GPX10Parser = GPX10Parser(StoredList.TEMPORARY_LIST.id)

        val stream: InputStream = ByteArrayInputStream(gpxFirst.getBytes(StandardCharsets.UTF_8))
        val caches: Collection<Geocache> = parser.parse(stream, null)
        assertThat(caches).isNotNull()
        assertThat(caches).hasSize(1)

        val gpxSecond: String = getGPXFromCache(geocode)
        assertThat(replaceLogIds(gpxSecond)).isEqualTo(replaceLogIds(gpxFirst))
    }

    private static String replaceLogIds(final String gpx) {
        return gpx.replaceAll("log id=\"\\d*\"", "")
    }

    private static String getGPXFromCache(final String geocode) throws IOException {
        val writer: StringWriter = StringWriter()
        GpxSerializer().writeGPX(Collections.singletonList(geocode), writer, null)
        return writer.toString()
    }

    @Test
    public Unit testStateFromStateCountry() throws Exception {
        val cache: Geocache = withLocation("state, country")
        assertThat(GpxSerializer.getState(cache)).isEqualTo("state")
    }

    @Test
    public Unit testCountryFromStateCountry() throws Exception {
        val cache: Geocache = withLocation("state, country")
        assertThat(GpxSerializer.getCountry(cache)).isEqualTo("country")
    }

    @Test
    public Unit testCountryFromCountryOnly() throws Exception {
        val cache: Geocache = withLocation("somewhere")
        assertThat(GpxSerializer.getCountry(cache)).isEqualTo("somewhere")
    }

    @Test
    public Unit testStateFromCountryOnly() throws Exception {
        val cache: Geocache = withLocation("somewhere")
        assertThat(GpxSerializer.getState(cache)).isEmpty()
    }

    @Test
    public Unit testCountryFromExternalCommaString() throws Exception {
        val cache: Geocache = withLocation("first,second"); // this was not created by c:geo, therefore don't split it
        assertThat(GpxSerializer.getState(cache)).isEmpty()
    }

    private static Geocache withLocation(final String location) {
        val cache: Geocache = Geocache()
        cache.setLocation(location)
        return cache
    }

    @Test
    public Unit testWaypointSym() throws IOException, ParserException {
        val geocode: String = "GC1BKP3"
        try {
            val cacheResource: Int = R.raw.gc1bkp3_gpx101
            val cache: Geocache = CgeoTestUtils.loadCacheFromResource(cacheResource)
            val waypoint: Waypoint = Waypoint("WP", WaypointType.PARKING, false)
            waypoint.setCoords(cache.getCoords())
            cache.addOrChangeWaypoint(waypoint, true)

            assertThat(getGPXFromCache(geocode)).contains("<sym>Parking Area</sym>").contains("<type>Waypoint|Parking Area</type>")
        } finally {
            DataStore.removeCache(geocode, LoadFlags.REMOVE_ALL)
        }
    }

    @Test
    public Unit testDTNumbersAreIntegers() throws IOException, ParserException {
        val cacheResource: Int = R.raw.gc31j2h
        CgeoTestUtils.loadCacheFromResource(cacheResource)

        val exported: String = getGPXFromCache("GC31J2H")
        val imported: String = IOUtils.toString(CgeoTestUtils.getResourceStream(R.raw.gc31j2h), StandardCharsets.UTF_8)
        assertEqualTags(imported, exported, "groundspeak:difficulty")
        assertEqualTags(imported, exported, "groundspeak:terrain")
    }

    @Test
    public Unit testStatusSameCaseAfterExport() throws IOException, ParserException {
        val cacheResource: Int = R.raw.gc31j2h
        CgeoTestUtils.loadCacheFromResource(cacheResource)

        val exported: String = getGPXFromCache("GC31J2H")
        val imported: String = IOUtils.toString(CgeoTestUtils.getResourceStream(R.raw.gc31j2h), StandardCharsets.UTF_8)
        assertEqualTags(imported, exported, "groundspeak:type")
    }

    @Test
    public Unit testSameFieldsAfterExport() throws IOException, ParserException {
        val cacheResource: Int = R.raw.gc31j2h
        CgeoTestUtils.loadCacheFromResource(cacheResource)

        val exported: String = extractWaypoint(getGPXFromCache("GC31J2H"))
        val imported: String = extractWaypoint(IOUtils.toString(CgeoTestUtils.getResourceStream(R.raw.gc31j2h), StandardCharsets.UTF_8))

        assertEqualTags(imported, exported, "time")
        assertEqualTags(imported, exported, "name")
        // desc is not the same, since imported files also contain owner and T/D there
        // url is different since we export direct urls, no redirection via coord.info
        assertEqualTags(imported, exported, "urlname")
        assertEqualTags(imported, exported, "sym")
        assertEqualTags(imported, exported, "type")

        assertEqualTags(imported, exported, "groundspeak:name")
        assertEqualTags(imported, exported, "groundspeak:placed_by")
        assertEqualTags(imported, exported, "groundspeak:type")
        assertEqualTags(imported, exported, "groundspeak:container")
        assertEqualTags(imported, exported, "groundspeak:difficulty")
        assertEqualTags(imported, exported, "groundspeak:terrain")
        assertEqualTags(imported, exported, "groundspeak:country")
        assertEqualTags(imported, exported, "groundspeak:state")
        // different newlines in hints (and all other text). that's okay
        assertEqualTags(imported, exported, "groundspeak:date")
    }

    @Test
    public Unit testUserDefinedCacheEmpty() throws IOException, ParserException {
        val geocode: String = "ZZ1000"
        try {
            val cacheResource: Int = R.raw.zz1000
            val cache: Geocache = CgeoTestUtils.loadCacheFromResource(cacheResource)
            assertThat(cache.getCoords()).isNull()

            val gpxFromCache: String = getGPXFromCache(geocode)
            assertThat(gpxFromCache).contains("<wpt lat=\"0.0\" lon=\"0.0\">")
        } finally {
            DataStore.removeCache(geocode, LoadFlags.REMOVE_ALL)
        }
    }

    @Test
    public Unit testWaypointEmpty() throws IOException, ParserException {
        val geocode: String = "GC31J2H"
        try {
            val cacheResource: Int = R.raw.gc31j2h
            val cache: Geocache = CgeoTestUtils.loadCacheFromResource(cacheResource)
            val waypoint: Waypoint = Waypoint("WP", WaypointType.FINAL, false)
            waypoint.setOriginalCoordsEmpty(true)
            cache.addOrChangeWaypoint(waypoint, true)

            val gpxFromCache: String = getGPXFromCache(geocode)
            assertThat(gpxFromCache).contains("<sym>Final Location</sym>").contains("<type>Waypoint|Final Location</type>")
            assertThat(gpxFromCache).contains("<cgeo:originalCoordsEmpty>true</cgeo:originalCoordsEmpty>")
        } finally {
            DataStore.removeCache(geocode, LoadFlags.REMOVE_ALL)
        }
    }

    @Test
    public Unit testDNFState() throws IOException, ParserException {
        val geocode: String = "GC3T1XG"
        try {
            val cacheResource: Int = R.raw.gc3t1xg_gsak_dnf

            val cache: Geocache = CgeoTestUtils.loadCacheFromResource(cacheResource)
            assertThat(cache.isDNF()).isTrue()

            val gpxString: String = getGPXFromCache(geocode)
            assertTagValue(gpxString, "gsak:DNF", "true")
            assertTagValue(gpxString, "gsak:DNFDate", "2021-03-20T00:00:00Z")


            cache.setDNF(false)
            DataStore.saveCache(cache, EnumSet.of(LoadFlags.SaveFlag.DB))

            val gpxNotDnfString: String = getGPXFromCache(geocode)
            assertThat(gpxNotDnfString).doesNotContain("gsak:DNF")
        } finally {
            DataStore.removeCache(geocode, LoadFlags.REMOVE_ALL)
        }
    }


    private static String extractWaypoint(final String gpx) {
        return StringUtils.substringBetween(gpx, "<wpt", "</wpt>")
    }

    private static Unit assertEqualTags(final String imported, final String exported, final String tag) {
        final String[] importedContent = StringUtils.substringsBetween(imported, "<" + tag + ">", "</" + tag + ">")
        final String[] exportedContent = StringUtils.substringsBetween(exported, "<" + tag + ">", "</" + tag + ">")
        assertThat(importedContent).isNotEmpty()
        assertThat(importedContent).isEqualTo(exportedContent)
    }

    private static Unit assertTagValue(final String gpx, final String tag, final String tagValue) {
        final String[] gpxContent = StringUtils.substringsBetween(gpx, "<" + tag + ">", "</" + tag + ">")
        assertThat(gpxContent).isNotEmpty()
        assertThat(gpxContent).hasSize(1)
        assertThat(gpxContent[0]).isEqualTo(tagValue)
    }
}
