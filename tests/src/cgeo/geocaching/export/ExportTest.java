package cgeo.geocaching.export;

import cgeo.CGeoTestCase;
import cgeo.geocaching.Geocache;
import cgeo.geocaching.LogEntry;
import cgeo.geocaching.cgData;
import cgeo.geocaching.enumerations.LoadFlags;
import cgeo.geocaching.enumerations.LogType;
import cgeo.geocaching.geopoint.Geopoint;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class ExportTest extends CGeoTestCase {

    public static void testGSAKExport() {
        final Geocache cache = new Geocache();
        cache.setGeocode("GCX1234");
        final LogEntry log = new LogEntry(1353244820000L, LogType.FOUND_IT, "Hidden in a tree");
        final StringBuilder logStr = new StringBuilder();
        FieldnoteExport.appendFieldNote(logStr, cache, log);
        assertEquals("Non matching export " + logStr.toString(), "GCX1234,2012-11-18T13:20:20Z,Found it,\"Hidden in a tree\"\n", logStr.toString());
    }

    public static void testGpxExportSmilies() throws InterruptedException, ExecutionException {
        final Geocache cache = new Geocache();
        cache.setGeocode("GCX1234");
        cache.setCoords(new Geopoint("N 49 44.000 E 8 37.000"));
        final LogEntry log = new LogEntry(1353244820000L, LogType.FOUND_IT, "Smile: \ud83d\ude0a");
        cache.getLogs().add(log);
        cgData.saveCache(cache, LoadFlags.SAVE_ALL);
        ArrayList<Geocache> exportList = new ArrayList<Geocache>();
        exportList.add(cache);
        GpxExportTester gpxExport = new GpxExportTester();
        File result = null;
        try {
            result = gpxExport.testExportSync(exportList);
        } finally {
            cgData.removeCache(cache.getGeocode(), LoadFlags.REMOVE_ALL);
        }

        assertNotNull(result);

        result.delete();
    }

    private static class GpxExportTester extends GpxExport {

        protected GpxExportTester() {
            super();
        }

        public File testExportSync(List<Geocache> caches) throws InterruptedException, ExecutionException {
            final ArrayList<String> geocodes = new ArrayList<String>(caches.size());
            for (final Geocache cache: caches) {
                geocodes.add(cache.getGeocode());
            }
            final ExportTask task = new ExportTask(null);
            task.execute(geocodes.toArray(new String[geocodes.size()]));
            return task.get();
        }

    }

}
