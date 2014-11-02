package cgeo.geocaching.export;

import static org.assertj.core.api.Assertions.assertThat;

import cgeo.CGeoTestCase;
import cgeo.geocaching.DataStore;
import cgeo.geocaching.Geocache;
import cgeo.geocaching.LogEntry;
import cgeo.geocaching.enumerations.LoadFlags;
import cgeo.geocaching.enumerations.LogType;
import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.utils.FileUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class ExportTest extends CGeoTestCase {

    public static void testGSAKExport() {
        final Geocache cache = new Geocache();
        cache.setGeocode("GCX1234");
        final LogEntry log = new LogEntry(1353244820000L, LogType.FOUND_IT, "Hidden in a tree");
        FieldNotes fieldNotes = new FieldNotes();
        fieldNotes.add(cache, log);
        assertEquals("Non matching export " + fieldNotes.getContent(), "GCX1234,2012-11-18T13:20:20Z,Found it,\"Hidden in a tree\"\n", fieldNotes.getContent());
    }

    public static void testGpxExportSmilies() throws InterruptedException, ExecutionException {
        final Geocache cache = new Geocache();
        cache.setGeocode("GCX1234");
        cache.setCoords(new Geopoint("N 49 44.000 E 8 37.000"));
        final LogEntry log = new LogEntry(1353244820000L, LogType.FOUND_IT, "Smile: \ud83d\ude0a");
        DataStore.saveCache(cache, LoadFlags.SAVE_ALL);
        DataStore.saveLogsWithoutTransaction(cache.getGeocode(), Collections.singletonList(log));
        ArrayList<Geocache> exportList = new ArrayList<Geocache>();
        exportList.add(cache);
        GpxExportTester gpxExport = new GpxExportTester();
        File result = null;
        try {
            result = gpxExport.testExportSync(exportList);
        } finally {
            DataStore.removeCache(cache.getGeocode(), LoadFlags.REMOVE_ALL);
        }

        assertThat(result).isNotNull();

        FileUtils.deleteIgnoringFailure(result);
    }

    private static class GpxExportTester extends GpxExport {

        public File testExportSync(List<Geocache> caches) throws InterruptedException, ExecutionException {
            final ArrayList<String> geocodes = new ArrayList<String>(caches.size());
            for (final Geocache cache : caches) {
                geocodes.add(cache.getGeocode());
            }
            final ExportTask task = new ExportTask(null);
            task.execute(geocodes.toArray(new String[geocodes.size()]));
            return task.get();
        }

    }

}
