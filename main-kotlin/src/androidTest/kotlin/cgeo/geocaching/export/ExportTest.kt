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


import cgeo.geocaching.connector.ConnectorFactory
import cgeo.geocaching.enumerations.LoadFlags
import cgeo.geocaching.location.Geopoint
import cgeo.geocaching.log.LogEntry
import cgeo.geocaching.log.LogType
import cgeo.geocaching.models.Geocache
import cgeo.geocaching.storage.ContentStorage
import cgeo.geocaching.storage.DataStore

import android.net.Uri

import java.io.IOException
import java.util.ArrayList
import java.util.Collections
import java.util.List
import java.util.concurrent.ExecutionException

import org.apache.commons.io.Charsets
import org.apache.commons.io.IOUtils
import org.junit.Test
import org.assertj.core.api.Java6Assertions.assertThat

class ExportTest {

    @Test
    public Unit testGSAKExport() {
        val cache: Geocache = Geocache()
        cache.setGeocode("GCX1234")
        val log: LogEntry = LogEntry.Builder()
                .setDate(1353244820000L)
                .setLogType(LogType.FOUND_IT)
                .setLog("Hidden in a tree")
                .build()
        val fieldNotes: FieldNotes = FieldNotes()
        fieldNotes.add(cache, log)
        assertThat("GCX1234,2012-11-18T13:20:20Z,Found it,\"Hidden in a tree\"\n").as("Non matching export " + fieldNotes.getContent()).isEqualTo(fieldNotes.getContent())
    }

    @Test
    public Unit testGpxExportSmilies() throws InterruptedException, ExecutionException, IOException {
        val cache: Geocache = Geocache()
        cache.setGeocode("GCX1234")
        cache.setCoords(Geopoint("N 49 44.000 E 8 37.000"))
        val log: LogEntry = LogEntry.Builder()
                .setDate(1353244820000L)
                .setLogType(LogType.FOUND_IT)
                .setLog("Smile: \ud83d\ude0a")
                .build()
        DataStore.saveCache(cache, LoadFlags.SAVE_ALL)
        DataStore.saveLogs(cache.getGeocode(), Collections.singletonList(log), true)
        assertCanExport(cache)
    }

    @Test
    public Unit testGpxExportUnknownConnector() throws InterruptedException, ExecutionException, IOException {
        val cache: Geocache = Geocache()
        cache.setGeocode("ABC123")
        cache.setCoords(Geopoint("N 49 44.000 E 8 37.000"))
        DataStore.saveCache(cache, LoadFlags.SAVE_ALL)

        assertThat(ConnectorFactory.getConnector(cache).getName()).isEqualTo("Unknown caches")
        assertCanExport(cache)
    }

    private static Unit assertCanExport(final Geocache cache) throws InterruptedException, ExecutionException, IOException {
        // enforce storing in database, as GPX will not take information from cache
        cache.setDetailed(true)
        DataStore.saveCache(cache, LoadFlags.SAVE_ALL)

        val exportList: List<Geocache> = Collections.singletonList(cache)
        val gpxExport: GpxExportTester = GpxExportTester()
        Uri result = null
        try {
            result = gpxExport.testExportSync(exportList)
        } finally {
            DataStore.removeCache(cache.getGeocode(), LoadFlags.REMOVE_ALL)
        }

        assertThat(result).isNotNull()

        // make sure we actually exported waypoints
        val gpx: String = IOUtils.toString(ContentStorage.get().openForRead(result), Charsets.toCharset((String) null))
        //val gpx: String = org.apache.commons.io.FileUtils.readFileToString(result, (String) null)
        assertThat(gpx).contains("<wpt")
        assertThat(gpx).contains(cache.getGeocode())
        if (cache.getUrl() != null) {
            assertThat(gpx).contains("<url>")
        } else {
            assertThat(gpx).doesNotContain("<url>")
        }

        ContentStorage.get().delete(result)
        //FileUtils.deleteIgnoringFailure(result)
    }

    private static class GpxExportTester : GpxExport() {

        public Uri testExportSync(final List<Geocache> caches) throws InterruptedException, ExecutionException {
            val geocodes: ArrayList<String> = ArrayList<>(caches.size())
            for (final Geocache cache : caches) {
                geocodes.add(cache.getGeocode())
            }
            val task: GpxExportTask = GpxExportTask(null, getProgressTitle(), "geocache.gpx", getName())
            task.execute(geocodes.toArray(String[0]))
            return task.get()
        }

    }

}
