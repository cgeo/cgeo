package cgeo.geocaching.export;


import cgeo.geocaching.connector.ConnectorFactory;
import cgeo.geocaching.enumerations.LoadFlags;
import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.log.LogEntry;
import cgeo.geocaching.log.LogType;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.storage.ContentStorage;
import cgeo.geocaching.storage.DataStore;

import android.net.Uri;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;

import org.apache.commons.io.Charsets;
import org.apache.commons.io.IOUtils;
import org.junit.Test;
import static org.assertj.core.api.Java6Assertions.assertThat;

public class ExportTest {

    @Test
    public void testGSAKExport() {
        final Geocache cache = new Geocache();
        cache.setGeocode("GCX1234");
        final LogEntry log = new LogEntry.Builder()
                .setDate(1353244820000L)
                .setLogType(LogType.FOUND_IT)
                .setLog("Hidden in a tree")
                .build();
        final FieldNotes fieldNotes = new FieldNotes();
        fieldNotes.add(cache, log);
        assertThat("GCX1234,2012-11-18T13:20:20Z,Found it,\"Hidden in a tree\"\n").as("Non matching export " + fieldNotes.getContent()).isEqualTo(fieldNotes.getContent());
    }

    @Test
    public void testGpxExportSmilies() throws InterruptedException, ExecutionException, IOException {
        final Geocache cache = new Geocache();
        cache.setGeocode("GCX1234");
        cache.setCoords(new Geopoint("N 49 44.000 E 8 37.000"));
        final LogEntry log = new LogEntry.Builder()
                .setDate(1353244820000L)
                .setLogType(LogType.FOUND_IT)
                .setLog("Smile: \ud83d\ude0a")
                .build();
        DataStore.saveCache(cache, LoadFlags.SAVE_ALL);
        DataStore.saveLogs(cache.getGeocode(), Collections.singletonList(log), true);
        assertCanExport(cache);
    }

    @Test
    public void testGpxExportUnknownConnector() throws InterruptedException, ExecutionException, IOException {
        final Geocache cache = new Geocache();
        cache.setGeocode("ABC123");
        cache.setCoords(new Geopoint("N 49 44.000 E 8 37.000"));
        DataStore.saveCache(cache, LoadFlags.SAVE_ALL);

        assertThat(ConnectorFactory.getConnector(cache).getName()).isEqualTo("Unknown caches");
        assertCanExport(cache);
    }

    private static void assertCanExport(final Geocache cache) throws InterruptedException, ExecutionException, IOException {
        // enforce storing in database, as GPX will not take information from cache
        cache.setDetailed(true);
        DataStore.saveCache(cache, LoadFlags.SAVE_ALL);

        final List<Geocache> exportList = Collections.singletonList(cache);
        final GpxExportTester gpxExport = new GpxExportTester();
        Uri result = null;
        try {
            result = gpxExport.testExportSync(exportList);
        } finally {
            DataStore.removeCache(cache.getGeocode(), LoadFlags.REMOVE_ALL);
        }

        assertThat(result).isNotNull();

        // make sure we actually exported waypoints
        final String gpx = IOUtils.toString(ContentStorage.get().openForRead(result), Charsets.toCharset((String) null));
        //final String gpx = org.apache.commons.io.FileUtils.readFileToString(result, (String) null);
        assertThat(gpx).contains("<wpt");
        assertThat(gpx).contains(cache.getGeocode());
        if (cache.getUrl() != null) {
            assertThat(gpx).contains("<url>");
        } else {
            assertThat(gpx).doesNotContain("<url>");
        }

        ContentStorage.get().delete(result);
        //FileUtils.deleteIgnoringFailure(result);
    }

    private static class GpxExportTester extends GpxExport {

        public Uri testExportSync(final List<Geocache> caches) throws InterruptedException, ExecutionException {
            final ArrayList<String> geocodes = new ArrayList<>(caches.size());
            for (final Geocache cache : caches) {
                geocodes.add(cache.getGeocode());
            }
            final ExportTask task = new ExportTask(null);
            task.execute(geocodes.toArray(new String[geocodes.size()]));
            return task.get();
        }

    }

}
