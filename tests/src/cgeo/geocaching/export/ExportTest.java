package cgeo.geocaching.export;

import cgeo.CGeoTestCase;
import cgeo.geocaching.LogEntry;
import cgeo.geocaching.cgCache;
import cgeo.geocaching.enumerations.LogType;

public class ExportTest extends CGeoTestCase {

    public static void testGSAKExport() {
        final cgCache cache = new cgCache();
        cache.setGeocode("GCX1234");
        final LogEntry log = new LogEntry(1353244820000L, LogType.FOUND_IT, "Hidden in a tree");
        final StringBuilder logStr = new StringBuilder();
        FieldnoteExport.appendFieldNote(logStr, cache, log);
        assertEquals("Non matching export " + logStr.toString(), "GCX1234,2012-11-18T13:20:20Z,Found it,\"Hidden in a tree\"\n", logStr.toString());
    }

}
