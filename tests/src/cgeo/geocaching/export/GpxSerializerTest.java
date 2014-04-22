package cgeo.geocaching.export;

import static org.assertj.core.api.Assertions.assertThat;

import cgeo.geocaching.Geocache;
import cgeo.geocaching.files.GPX10Parser;
import cgeo.geocaching.files.ParserException;
import cgeo.geocaching.list.StoredList;
import cgeo.geocaching.test.AbstractResourceInstrumentationTestCase;
import cgeo.geocaching.test.R;

import org.apache.commons.lang3.CharEncoding;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicReference;

public class GpxSerializerTest extends AbstractResourceInstrumentationTestCase {

    public static void testWriteEmptyGPX() throws Exception {
        final StringWriter writer = new StringWriter();
        new GpxSerializer().writeGPX(Collections.<String> emptyList(), writer, null);
        assertThat(writer.getBuffer().toString()).isEqualTo("<?xml version='1.0' encoding='UTF-8' standalone='yes' ?>" +
                "<gpx version=\"1.0\" creator=\"c:geo - http://www.cgeo.org/\" " +
                "xsi:schemaLocation=\"http://www.topografix.com/GPX/1/0 http://www.topografix.com/GPX/1/0/gpx.xsd " +
                "http://www.groundspeak.com/cache/1/0 http://www.groundspeak.com/cache/1/0/1/cache.xsd " +
                "http://www.gsak.net/xmlv1/4 http://www.gsak.net/xmlv1/4/gsak.xsd\" " +
                "xmlns=\"http://www.topografix.com/GPX/1/0\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" " +
                "xmlns:groundspeak=\"http://www.groundspeak.com/cache/1/0\" xmlns:gsak=\"http://www.gsak.net/xmlv1/4\" " +
                "xmlns:cgeo=\"http://www.cgeo.org/wptext/1/0\" />");
    }

    public void testProgressReporting() throws IOException, ParserException {
        final AtomicReference<Integer> importedCount = new AtomicReference<Integer>(0);
        final StringWriter writer = new StringWriter();

        Geocache cache = loadCacheFromResource(R.raw.gc1bkp3_gpx101);
        assertThat(cache).isNotNull();

        new GpxSerializer().writeGPX(Collections.singletonList("GC1BKP3"), writer, new GpxSerializer.ProgressListener() {

            @Override
            public void publishProgress(int countExported) {
                importedCount.set(countExported);
            }
        });
        assertEquals("Progress listener not called", 1, importedCount.get().intValue());
    }

    /**
     * This test verifies that a loop of import, export, import leads to the same cache information.
     *
     * @throws IOException
     * @throws ParserException
     */
    public void testStableExportImportExport() throws IOException, ParserException {
        final String geocode = "GC1BKP3";
        final int cacheResource = R.raw.gc1bkp3_gpx101;
        final Geocache cache = loadCacheFromResource(cacheResource);
        assertThat(cache).isNotNull();

        final String gpxFirst = getGPXFromCache(geocode);

        assertThat(gpxFirst.length() > 0).isTrue();

        final GPX10Parser parser = new GPX10Parser(StoredList.TEMPORARY_LIST_ID);

        final InputStream stream = new ByteArrayInputStream(gpxFirst.getBytes(CharEncoding.UTF_8));
        Collection<Geocache> caches = parser.parse(stream, null);
        assertThat(caches).isNotNull();
        assertThat(caches).hasSize(1);

        final String gpxSecond = getGPXFromCache(geocode);
        assertThat(replaceLogIds(gpxSecond)).isEqualTo(replaceLogIds(gpxFirst));
    }

    private static String replaceLogIds(String gpx) {
        return gpx.replaceAll("log id=\"\\d*\"", "");
    }

    private static String getGPXFromCache(String geocode) throws IOException {
        final StringWriter writer = new StringWriter();
        new GpxSerializer().writeGPX(Collections.singletonList(geocode), writer, null);
        return writer.toString();
    }

}
