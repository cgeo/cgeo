package cgeo.geocaching.export;

import static org.assertj.core.api.Assertions.assertThat;

import cgeo.geocaching.enumerations.LoadFlags;
import cgeo.geocaching.enumerations.WaypointType;
import cgeo.geocaching.files.GPX10Parser;
import cgeo.geocaching.files.ParserException;
import cgeo.geocaching.list.StoredList;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.models.Waypoint;
import cgeo.geocaching.storage.DataStore;
import cgeo.geocaching.test.AbstractResourceInstrumentationTestCase;
import cgeo.geocaching.test.R;
import cgeo.geocaching.utils.Charsets;

import android.support.annotation.NonNull;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.CharEncoding;
import org.apache.commons.lang3.StringUtils;

public class GpxSerializerTest extends AbstractResourceInstrumentationTestCase {

    public static void testWriteEmptyGPX() throws Exception {
        final StringWriter writer = new StringWriter();
        new GpxSerializer().writeGPX(Collections.<String> emptyList(), writer, null);
        assertThat(writer.getBuffer().toString()).isEqualTo("<?xml version='1.0' encoding='UTF-8' standalone='yes' ?>" +
                "<gpx version=\"1.0\" creator=\"c:geo - http://www.cgeo.org/\" " +
                "xsi:schemaLocation=\"http://www.topografix.com/GPX/1/0 http://www.topografix.com/GPX/1/0/gpx.xsd " +
                "http://www.groundspeak.com/cache/1/0/1 http://www.groundspeak.com/cache/1/0/1/cache.xsd " +
                "http://www.gsak.net/xmlv1/6 http://www.gsak.net/xmlv1/6/gsak.xsd\" " +
                "xmlns=\"http://www.topografix.com/GPX/1/0\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" " +
                "xmlns:groundspeak=\"http://www.groundspeak.com/cache/1/0/1\" xmlns:gsak=\"http://www.gsak.net/xmlv1/6\" " +
                "xmlns:cgeo=\"http://www.cgeo.org/wptext/1/0\" />");
    }

    public void testProgressReporting() throws IOException, ParserException {
        final AtomicReference<Integer> importedCount = new AtomicReference<>(0);
        final StringWriter writer = new StringWriter();

        final Geocache cache = loadCacheFromResource(R.raw.gc1bkp3_gpx101);
        assertThat(cache).isNotNull();

        new GpxSerializer().writeGPX(Collections.singletonList("GC1BKP3"), writer, new GpxSerializer.ProgressListener() {

            @Override
            public void publishProgress(final int countExported) {
                importedCount.set(countExported);
            }
        });
        assertEquals("Progress listener not called", 1, importedCount.get().intValue());
    }

    /**
     * This test verifies that a loop of import, export, import leads to the same cache information.
     *
     */
    public void testStableExportImportExport() throws IOException, ParserException {
        final String geocode = "GC1BKP3";
        final int cacheResource = R.raw.gc1bkp3_gpx101;
        final Geocache cache = loadCacheFromResource(cacheResource);
        assertThat(cache).isNotNull();

        final String gpxFirst = getGPXFromCache(geocode);

        assertThat(gpxFirst.length()).isGreaterThan(0);

        final GPX10Parser parser = new GPX10Parser(StoredList.TEMPORARY_LIST.id);

        final InputStream stream = new ByteArrayInputStream(gpxFirst.getBytes(CharEncoding.UTF_8));
        final Collection<Geocache> caches = parser.parse(stream, null);
        assertThat(caches).isNotNull();
        assertThat(caches).hasSize(1);

        final String gpxSecond = getGPXFromCache(geocode);
        assertThat(replaceLogIds(gpxSecond)).isEqualTo(replaceLogIds(gpxFirst));
    }

    private static String replaceLogIds(final String gpx) {
        return gpx.replaceAll("log id=\"\\d*\"", "");
    }

    private static String getGPXFromCache(final String geocode) throws IOException {
        final StringWriter writer = new StringWriter();
        new GpxSerializer().writeGPX(Collections.singletonList(geocode), writer, null);
        return writer.toString();
    }

    public static void testStateFromStateCountry() throws Exception {
        final Geocache cache = withLocation("state, country");
        assertThat(GpxSerializer.getState(cache)).isEqualTo("state");
    }

    public static void testCountryFromStateCountry() throws Exception {
        final Geocache cache = withLocation("state, country");
        assertThat(GpxSerializer.getCountry(cache)).isEqualTo("country");
    }

    public static void testCountryFromCountryOnly() throws Exception {
        final Geocache cache = withLocation("somewhere");
        assertThat(GpxSerializer.getCountry(cache)).isEqualTo("somewhere");
    }

    public static void testStateFromCountryOnly() throws Exception {
        final Geocache cache = withLocation("somewhere");
        assertThat(GpxSerializer.getState(cache)).isEmpty();
    }

    public static void testCountryFromExternalCommaString() throws Exception {
        final Geocache cache = withLocation("first,second"); // this was not created by c:geo, therefore don't split it
        assertThat(GpxSerializer.getState(cache)).isEmpty();
    }

    private static Geocache withLocation(final String location) {
        final Geocache cache = new Geocache();
        cache.setLocation(location);
        return cache;
    }

    public void testWaypointSym() throws IOException, ParserException {
        final String geocode = "GC1BKP3";
        try {
            final int cacheResource = R.raw.gc1bkp3_gpx101;
            final Geocache cache = loadCacheFromResource(cacheResource);
            final Waypoint waypoint = new Waypoint("WP", WaypointType.PARKING, false);
            waypoint.setCoords(cache.getCoords());
            cache.addOrChangeWaypoint(waypoint, true);

            assertThat(getGPXFromCache(geocode)).contains("<sym>Parking Area</sym>").contains("<type>Waypoint|Parking Area</type>");
        } finally {
            DataStore.removeCache(geocode, LoadFlags.REMOVE_ALL);
        }
    }

    public void testDTNumbersAreIntegers() throws IOException, ParserException {
        final int cacheResource = R.raw.gc31j2h;
        loadCacheFromResource(cacheResource);

        final String exported = getGPXFromCache("GC31J2H");
        final String imported = IOUtils.toString(getResourceStream(R.raw.gc31j2h), Charsets.UTF_8);
        assertEqualTags(imported, exported, "groundspeak:difficulty");
        assertEqualTags(imported, exported, "groundspeak:terrain");
    }

    public void testStatusSameCaseAfterExport() throws IOException, ParserException {
        final int cacheResource = R.raw.gc31j2h;
        loadCacheFromResource(cacheResource);

        final String exported = getGPXFromCache("GC31J2H");
        final String imported = IOUtils.toString(getResourceStream(R.raw.gc31j2h), Charsets.UTF_8);
        assertEqualTags(imported, exported, "groundspeak:type");
    }

    public void testSameFieldsAfterExport() throws IOException, ParserException {
        final int cacheResource = R.raw.gc31j2h;
        loadCacheFromResource(cacheResource);

        final String exported = extractWaypoint(getGPXFromCache("GC31J2H"));
        final String imported = extractWaypoint(IOUtils.toString(getResourceStream(R.raw.gc31j2h), Charsets.UTF_8));

        assertEqualTags(imported, exported, "time");
        assertEqualTags(imported, exported, "name");
        // desc is not the same, since imported files also contain owner and T/D there
        // url is different since we export direct urls, no redirection via coord.info
        assertEqualTags(imported, exported, "urlname");
        assertEqualTags(imported, exported, "sym");
        assertEqualTags(imported, exported, "type");

        assertEqualTags(imported, exported, "groundspeak:name");
        assertEqualTags(imported, exported, "groundspeak:placed_by");
        assertEqualTags(imported, exported, "groundspeak:type");
        assertEqualTags(imported, exported, "groundspeak:container");
        assertEqualTags(imported, exported, "groundspeak:difficulty");
        assertEqualTags(imported, exported, "groundspeak:terrain");
        assertEqualTags(imported, exported, "groundspeak:country");
        assertEqualTags(imported, exported, "groundspeak:state");
        // different newlines in hints (and all other text). that's okay
        assertEqualTags(imported, exported, "groundspeak:date");
    }

    @NonNull
    private static String extractWaypoint(final String gpx) {
        return StringUtils.substringBetween(gpx, "<wpt", "</wpt>");
    }

    private static void assertEqualTags(final String imported, final String exported, final String tag) {
        final String[] importedContent = StringUtils.substringsBetween(imported, "<" + tag + ">", "</" + tag + ">");
        final String[] exportedContent = StringUtils.substringsBetween(exported, "<" + tag + ">", "</" + tag + ">");
        assertThat(importedContent).isNotEmpty();
        assertThat(importedContent).isEqualTo(exportedContent);
    }

}
