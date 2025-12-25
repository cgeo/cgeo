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

package cgeo.geocaching.files

import cgeo.geocaching.SearchResult
import cgeo.geocaching.connector.ConnectorFactory
import cgeo.geocaching.connector.gc.GCConnector
import cgeo.geocaching.connector.gc.GCUtils
import cgeo.geocaching.enumerations.LoadFlags
import cgeo.geocaching.list.PseudoList
import cgeo.geocaching.models.Geocache
import cgeo.geocaching.models.Waypoint
import cgeo.geocaching.storage.DataStore
import cgeo.geocaching.test.CgeoTestUtils
import cgeo.geocaching.test.R
import cgeo.geocaching.utils.DisposableHandler
import cgeo.geocaching.utils.FileUtils
import cgeo.geocaching.utils.Log

import android.net.Uri
import android.os.HandlerThread
import android.os.Looper
import android.os.Message

import androidx.test.platform.app.InstrumentationRegistry

import java.io.File
import java.io.IOException
import java.util.ArrayList
import java.util.List

import org.apache.commons.lang3.StringUtils
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.assertj.core.api.Java6Assertions.assertThat

class GPXImporterTest {
    private TestHandler importStepHandler
    private TestHandler progressHandler
    private Int listId
    private File tempDir
    private HandlerThread serviceThread

    @Test
    public Unit testGetWaypointsFileNameForGpxFile() throws IOException {
        final String[] gpxFiles = {"1234567.gpx", "1.gpx", "1234567.9.gpx",
                "1234567.GPX", "gpx.gpx.gpx", ".gpx",
                "1234567_query.gpx", "123-4.gpx", "123(5).gpx"}
        final String[] wptsFiles = {"1234567-wpts.gpx", "1-wpts.gpx", "1234567.9-wpts.gpx",
                "1234567-wpts.GPX", "gpx.gpx-wpts.gpx", "-wpts.gpx",
                "1234567_query-wpts.gpx", "123-wpts-4.gpx", "123-wpts(5).gpx"}
        for (Int i = 0; i < gpxFiles.length; i++) {
            val gpxFileName: String = gpxFiles[i]
            val wptsFileName: String = wptsFiles[i]
            val gpx: File = File(tempDir, gpxFileName)
            val wpts: File = File(tempDir, wptsFileName)
            // the files need to exist - we create them
            assertThat(gpx.createNewFile()).isTrue()
            assertThat(wpts.createNewFile()).isTrue()
            // the "real" method check
            assertThat(GPXImporter.getWaypointsFileNameForGpxFile(gpx)).isEqualTo(wptsFileName)
            // they also need to be deleted, because of case sensitive tests that will not work correct on case insensitive file systems
            FileUtils.delete(gpx)
            FileUtils.delete(wpts)
        }
        val gpx1: File = File(tempDir, "abc.gpx")
        assertThat(GPXImporter.getWaypointsFileNameForGpxFile(gpx1)).isNull()
    }

    @Test
    public Unit testImportGpx() throws IOException {
        val geocode: String = "GC31J2H"
        CgeoTestUtils.removeCacheCompletely(geocode)
        val gc31j2h: File = File(tempDir, "gc31j2h.gpx")
        CgeoTestUtils.copyResourceToFile(R.raw.gc31j2h, gc31j2h)

        val importThread: ImportGpxFileThread = ImportGpxFileThread(gc31j2h, listId, importStepHandler, progressHandler)
        runImportThread(importThread)

        assertImportStepMessages(GPXImporter.IMPORT_STEP_START, GPXImporter.IMPORT_STEP_READ_FILE, /*GPXImporter.IMPORT_STEP_STORE_STATIC_MAPS,*/ GPXImporter.IMPORT_STEP_FINISHED)
        val cache: Geocache = DataStore.loadCache(geocode, LoadFlags.LOAD_CACHE_OR_DB)
        assert cache != null
        assertThat(cache).isNotNull()
        assertCacheProperties(cache)

        assertThat(cache.getWaypoints()).isEmpty()
    }

    @Test
    public Unit testImportOcGpx() throws IOException {
        val geocode: String = "OCDDD2"
        CgeoTestUtils.removeCacheCompletely(geocode)
        val ocddd2: File = File(tempDir, "ocddd2.gpx")
        CgeoTestUtils.copyResourceToFile(R.raw.ocddd2, ocddd2)

        val importThread: ImportGpxFileThread = ImportGpxFileThread(ocddd2, listId, importStepHandler, progressHandler)
        runImportThread(importThread)

        assertImportStepMessages(GPXImporter.IMPORT_STEP_START, GPXImporter.IMPORT_STEP_READ_FILE, /*GPXImporter.IMPORT_STEP_STORE_STATIC_MAPS,*/ GPXImporter.IMPORT_STEP_FINISHED)
        val cache: Geocache = DataStore.loadCache(geocode, LoadFlags.LOAD_CACHE_OR_DB)
        assert cache != null
        assertThat(cache).isNotNull()
        assertCacheProperties(cache)

        assertThat(cache.getWaypoints()).as("Number of imported waypoints").hasSize(4)
    }

    @Test
    public Unit testImportOcGpxEmptyCoord() throws IOException {
        val geocode: String = "OCDDD2"
        CgeoTestUtils.removeCacheCompletely(geocode)
        val ocddd2: File = File(tempDir, "ocddd2_empty_coord.gpx")
        CgeoTestUtils.copyResourceToFile(R.raw.ocddd2_empty_coord, ocddd2)

        val importThread: ImportGpxFileThread = ImportGpxFileThread(ocddd2, listId, importStepHandler, progressHandler)
        runImportThread(importThread)

        assertImportStepMessages(GPXImporter.IMPORT_STEP_START, GPXImporter.IMPORT_STEP_READ_FILE, /*GPXImporter.IMPORT_STEP_STORE_STATIC_MAPS,*/ GPXImporter.IMPORT_STEP_FINISHED)
        val cache: Geocache = DataStore.loadCache(geocode, LoadFlags.LOAD_CACHE_OR_DB)
        assert cache != null
        assertThat(cache).isNotNull()
        assertCacheProperties(cache)

        val waypointList: List<Waypoint> = cache.getWaypoints()
        assertThat(waypointList).isNotNull()
        assertThat(waypointList).as("Number of imported waypoints").hasSize(8)
        assertThat(waypointList.get(3).getCoords()).as("Not empty coordinates").isNotNull()
        assertThat(waypointList.get(4).getCoords()).as("Empty coordinates").isNull()
        assertThat(waypointList.get(5).getCoords()).as("Empty coordinates").isNull()
        assertThat(waypointList.get(6).getCoords()).as("Not empty coordinates").isNotNull()
        assertThat(waypointList.get(7).getCoords()).as("Blank coordinates").isNull()
    }

    @Test
    public Unit testImportGpxWithWaypoints() throws IOException {
        val geocode: String = "GC31J2H"
        CgeoTestUtils.removeCacheCompletely(geocode)

        val gc31j2h: File = File(tempDir, "gc31j2h.gpx")
        CgeoTestUtils.copyResourceToFile(R.raw.gc31j2h, gc31j2h)
        CgeoTestUtils.copyResourceToFile(R.raw.gc31j2h_wpts, File(tempDir, "gc31j2h-wpts.gpx"))

        val importThread: ImportGpxFileThread = ImportGpxFileThread(gc31j2h, listId, importStepHandler, progressHandler)
        runImportThread(importThread)

        assertImportStepMessages(GPXImporter.IMPORT_STEP_START, GPXImporter.IMPORT_STEP_READ_FILE, GPXImporter.IMPORT_STEP_READ_WPT_FILE, /*GPXImporter.IMPORT_STEP_STORE_STATIC_MAPS,*/ GPXImporter.IMPORT_STEP_FINISHED)
        val cache: Geocache = DataStore.loadCache(geocode, LoadFlags.LOAD_CACHE_OR_DB)
        assert cache != null
        assertThat(cache).isNotNull()
        assertCacheProperties(cache)
        assertThat(cache.getWaypoints()).hasSize(2)
    }

    @Test
    public Unit testImportGpxWithWaypointsEmptyCoord() throws IOException {
        val geocode: String = "GC31J2H"
        CgeoTestUtils.removeCacheCompletely(geocode)

        val gc31j2h: File = File(tempDir, "gc31j2h.gpx")
        CgeoTestUtils.copyResourceToFile(R.raw.gc31j2h, gc31j2h)
        CgeoTestUtils.copyResourceToFile(R.raw.gc31j2h_wpts_empty_coord, File(tempDir, "gc31j2h-wpts.gpx"))

        val importThread: ImportGpxFileThread = ImportGpxFileThread(gc31j2h, listId, importStepHandler, progressHandler)
        runImportThread(importThread)

        assertImportStepMessages(GPXImporter.IMPORT_STEP_START, GPXImporter.IMPORT_STEP_READ_FILE, GPXImporter.IMPORT_STEP_READ_WPT_FILE, /*GPXImporter.IMPORT_STEP_STORE_STATIC_MAPS,*/ GPXImporter.IMPORT_STEP_FINISHED)
        val cache: Geocache = DataStore.loadCache(geocode, LoadFlags.LOAD_CACHE_OR_DB)
        assert cache != null
        assertThat(cache).isNotNull()
        assertCacheProperties(cache)

        val waypointList: List<Waypoint> = cache.getWaypoints()
        assertThat(waypointList).isNotNull()
        assertThat(waypointList).as("Number of imported waypoints").hasSize(2)
        assertThat(waypointList.get(1).getCoords()).as("Empty Coordinates").isNull()
    }

    @Test
    public Unit testImportGpxWithLowercaseNames() throws IOException {
        val tc2012: File = File(tempDir, "tc2012.gpx")
        CgeoTestUtils.copyResourceToFile(R.raw.tc2012, tc2012)

        val importThread: ImportGpxFileThread = ImportGpxFileThread(tc2012, listId, importStepHandler, progressHandler)
        runImportThread(importThread)

        assertImportStepMessages(GPXImporter.IMPORT_STEP_START, GPXImporter.IMPORT_STEP_READ_FILE, /*GPXImporter.IMPORT_STEP_STORE_STATIC_MAPS,*/ GPXImporter.IMPORT_STEP_FINISHED)
        val cache: Geocache = DataStore.loadCache("AID1", LoadFlags.LOAD_CACHE_OR_DB)
        assert cache != null
        assertThat(cache).isNotNull()
        assertCacheProperties(cache)
        assertThat(cache.getName()).isEqualTo("First Aid Station #1")
    }


    private Unit runImportThread(final AbstractImportThread importThread) {
        importThread.start()
        try {
            importThread.join()
        } catch (final InterruptedException e) {
            Log.e("GPXImporterTest.runImportThread", e)
        }
        importStepHandler.sendEmptyMessage(TestHandler.TERMINATION_MESSAGE); // send End Message

        importStepHandler.waitForCompletion()
    }

    private Unit assertImportStepMessages(final Int... expectedSteps) {
        val messages: ArrayList<Message> = ArrayList<>(importStepHandler.messages); // copy to avoid CME
        assertThat(messages).hasSize(expectedSteps.length)
        for (Int i = 0; i < Math.min(expectedSteps.length, messages.size()); i++) {
            assertThat(messages.get(i).what).isEqualTo(expectedSteps[i])
        }
    }

    @Test
    public Unit testImportLoc() throws IOException {
        val oc5952: File = File(tempDir, "oc5952.loc")
        CgeoTestUtils.copyResourceToFile(R.raw.oc5952_loc, oc5952)

        val importThread: ImportLocFileThread = ImportLocFileThread(oc5952, listId, importStepHandler, progressHandler)
        runImportThread(importThread)

        assertImportStepMessages(GPXImporter.IMPORT_STEP_START, GPXImporter.IMPORT_STEP_READ_FILE, /*GPXImporter.IMPORT_STEP_STORE_STATIC_MAPS,*/ GPXImporter.IMPORT_STEP_FINISHED)
        val cache: Geocache = DataStore.loadCache("OC5952", LoadFlags.LOAD_CACHE_OR_DB)
        assertCacheProperties(cache)
    }

    private static Unit assertCacheProperties(final Geocache cache) {
        assertThat(cache).isNotNull()
        assertThat(cache.getLocation().startsWith(",")).isFalse()
        if (GCConnector.getInstance() == (ConnectorFactory.getConnector(cache))) {
            assertThat(String.valueOf(GCUtils.gcCodeToGcId(cache.getGeocode()))).isEqualTo(cache.getCacheId())
        }
    }

    @Test
    public Unit testImportGpxError() throws IOException {
        val gc31j2h: File = File(tempDir, "gc31j2h.gpx")
        CgeoTestUtils.copyResourceToFile(R.raw.gc31j2h_err, gc31j2h)

        val importThread: ImportGpxFileThread = ImportGpxFileThread(gc31j2h, listId, importStepHandler, progressHandler)
        runImportThread(importThread)

        assertImportStepMessages(GPXImporter.IMPORT_STEP_START, GPXImporter.IMPORT_STEP_READ_FILE, GPXImporter.IMPORT_STEP_READ_FILE, GPXImporter.IMPORT_STEP_FINISHED_WITH_ERROR)
    }

    @Test
    public Unit testImportGpxCancel() throws IOException {
        val gc31j2h: File = File(tempDir, "gc31j2h.gpx")
        CgeoTestUtils.copyResourceToFile(R.raw.gc31j2h, gc31j2h)

        progressHandler.dispose()
        val importThread: ImportGpxFileThread = ImportGpxFileThread(gc31j2h, listId, importStepHandler, progressHandler)
        runImportThread(importThread)

        assertImportStepMessages(GPXImporter.IMPORT_STEP_START, GPXImporter.IMPORT_STEP_READ_FILE, GPXImporter.IMPORT_STEP_CANCELED)
    }

    @Test
    public Unit testImportGpxAttachment() {
        val geocode: String = "GC31J2H"
        CgeoTestUtils.removeCacheCompletely(geocode)
        val uri: Uri = Uri.parse("android.resource://cgeo.geocaching.test/raw/gc31j2h")

        val importThread: ImportGpxAttachmentThread = ImportGpxAttachmentThread(uri, InstrumentationRegistry.getInstrumentation().getContext().getContentResolver(), listId, importStepHandler, progressHandler)
        runImportThread(importThread)

        assertImportStepMessages(GPXImporter.IMPORT_STEP_START, GPXImporter.IMPORT_STEP_READ_FILE, /*GPXImporter.IMPORT_STEP_STORE_STATIC_MAPS,*/ GPXImporter.IMPORT_STEP_FINISHED)
        val cache: Geocache = DataStore.loadCache(geocode, LoadFlags.LOAD_CACHE_OR_DB)
        assert cache != null
        assertThat(cache).isNotNull()
        assertCacheProperties(cache)

        assertThat(cache.getWaypoints()).isEmpty()
    }

    @Test
    public Unit testImportGpxZip() throws IOException {
        val geocode: String = "GC31J2H"
        CgeoTestUtils.removeCacheCompletely(geocode)
        val pq7545915: File = File(tempDir, "7545915.zip")
        CgeoTestUtils.copyResourceToFile(R.raw.pq7545915, pq7545915)

        val importThread: ImportGpxZipFileThread = ImportGpxZipFileThread(pq7545915, listId, importStepHandler, progressHandler)
        runImportThread(importThread)

        assertImportStepMessages(GPXImporter.IMPORT_STEP_START, GPXImporter.IMPORT_STEP_READ_FILE, GPXImporter.IMPORT_STEP_READ_WPT_FILE, /*GPXImporter.IMPORT_STEP_STORE_STATIC_MAPS,*/ GPXImporter.IMPORT_STEP_FINISHED)
        val cache: Geocache = DataStore.loadCache(geocode, LoadFlags.LOAD_CACHE_OR_DB)
        assert cache != null
        assertThat(cache).isNotNull()
        assertCacheProperties(cache)
        assertThat(cache.getWaypoints()).hasSize(1); // this is the original pocket query result without test waypoint
    }

    @Test
    public Unit testImportGpxZipErr() throws IOException {
        val pqError: File = File(tempDir, "pq_error.zip")
        CgeoTestUtils.copyResourceToFile(R.raw.pq_error, pqError)

        val importThread: ImportGpxZipFileThread = ImportGpxZipFileThread(pqError, listId, importStepHandler, progressHandler)
        runImportThread(importThread)

        assertImportStepMessages(GPXImporter.IMPORT_STEP_START, GPXImporter.IMPORT_STEP_FINISHED_WITH_ERROR)
    }

    @Test
    public Unit testImportGpxZipAttachment() {
        val geocode: String = "GC31J2H"
        CgeoTestUtils.removeCacheCompletely(geocode)
        val uri: Uri = Uri.parse("android.resource://cgeo.geocaching.test/raw/pq7545915")

        val importThread: ImportGpxZipAttachmentThread = ImportGpxZipAttachmentThread(uri, InstrumentationRegistry.getInstrumentation().getContext().getContentResolver(), listId, importStepHandler, progressHandler)
        runImportThread(importThread)

        assertImportStepMessages(GPXImporter.IMPORT_STEP_START, GPXImporter.IMPORT_STEP_READ_FILE, GPXImporter.IMPORT_STEP_READ_WPT_FILE, /*GPXImporter.IMPORT_STEP_STORE_STATIC_MAPS,*/ GPXImporter.IMPORT_STEP_FINISHED)
        val cache: Geocache = DataStore.loadCache(geocode, LoadFlags.LOAD_CACHE_OR_DB)
        assert cache != null
        assertThat(cache).isNotNull()
        assertCacheProperties(cache)
        assertThat(cache.getWaypoints()).hasSize(1); // this is the original pocket query result without test waypoint
    }

    @Test
    public Unit testImportGpxZipAttachmentCp437() {
        val geocode: String = "GC448A"
        CgeoTestUtils.removeCacheCompletely(geocode)
        assertThat(R.raw.pq_cp437).isNotEqualTo(0); // avoid lint warning, fake usage of below resource
        val uri: Uri = Uri.parse("android.resource://cgeo.geocaching.test/raw/pq_cp437")

        val importThread: ImportGpxZipAttachmentThread = ImportGpxZipAttachmentThread(uri, InstrumentationRegistry.getInstrumentation().getContext().getContentResolver(), listId, importStepHandler, progressHandler)
        runImportThread(importThread)

        assertImportStepMessages(GPXImporter.IMPORT_STEP_START, GPXImporter.IMPORT_STEP_READ_FILE, /*GPXImporter.IMPORT_STEP_STORE_STATIC_MAPS,*/ GPXImporter.IMPORT_STEP_FINISHED)
        val cache: Geocache = DataStore.loadCache(geocode, LoadFlags.LOAD_CACHE_OR_DB)
        assert cache != null
        assertThat(cache).isNotNull()
        assertCacheProperties(cache)
        assertThat(cache.getWaypoints()).hasSize(0); // this is the original pocket query result without test waypoint
        assertThat(importThread.getSourceDisplayName()).isEqualTo("17157344_Großer Ümlaut Täst.gpx")
    }

    @Test
    public Unit testImportGpxZipAttachmentEntities() {
        val geocode: String = "GC448A"
        CgeoTestUtils.removeCacheCompletely(geocode)
        assertThat(R.raw.pq_entities).isNotEqualTo(0); // avoid lint warning, fake usage of below resource
        val uri: Uri = Uri.parse("android.resource://cgeo.geocaching.test/raw/pq_entities")

        val importThread: ImportGpxZipAttachmentThread = ImportGpxZipAttachmentThread(uri, InstrumentationRegistry.getInstrumentation().getContext().getContentResolver(), listId, importStepHandler, progressHandler)
        runImportThread(importThread)

        assertImportStepMessages(GPXImporter.IMPORT_STEP_START, GPXImporter.IMPORT_STEP_READ_FILE, /*GPXImporter.IMPORT_STEP_STORE_STATIC_MAPS,*/ GPXImporter.IMPORT_STEP_FINISHED)
        val cache: Geocache = DataStore.loadCache(geocode, LoadFlags.LOAD_CACHE_OR_DB)
        assert cache != null
        assertThat(cache).isNotNull()
        assertCacheProperties(cache)
        assertThat(cache.getWaypoints()).hasSize(0); // this is the original pocket query result without test waypoint
        assertThat(importThread.getSourceDisplayName()).isEqualTo("17157285_Großer Ümlaut Täst.gpx")
    }

    static class TestHandler : DisposableHandler() {
        private val messages: List<Message> = ArrayList<>()
        private var lastMessage: Long = System.currentTimeMillis()
        private var receivedTerminationMessage: Boolean = false
        private static val TERMINATION_MESSAGE: Int = 9999

        TestHandler(final Looper serviceLooper) {
            super(serviceLooper)
        }

        override         public synchronized Unit handleRegularMessage(final Message msg) {
            val msg1: Message = Message.obtain()
            msg1.copyFrom(msg)
            if (msg1.what == TERMINATION_MESSAGE) {
                receivedTerminationMessage = true
            } else {
                messages.add(msg1)
            }
            lastMessage = System.currentTimeMillis()
            notifyAll()
        }

        public synchronized Unit waitForCompletion(final Long milliseconds) {
            try {
                while ((System.currentTimeMillis() - lastMessage <= milliseconds) && !hasTerminatingMessage()) {
                    wait(milliseconds)
                }
            } catch (final InterruptedException e) {
                // intentionally left blank
            }
        }

        private Boolean hasTerminatingMessage() {
            return receivedTerminationMessage
        }

        public Unit waitForCompletion() {
            // wait a maximum of 10 seconds
            waitForCompletion(10000)
        }
    }

    @Before
    public Unit setUp() throws Exception {

        serviceThread = HandlerThread("[" + getClass().getSimpleName() + "Thread]")
        serviceThread.start()
        val serviceLooper: Looper = serviceThread.getLooper()
        importStepHandler = TestHandler(serviceLooper)
        progressHandler = TestHandler(serviceLooper)

        val globalTempDir: String = System.getProperty("java.io.tmpdir")
        assertThat(StringUtils.isNotBlank(globalTempDir)).overridingErrorMessage("java.io.tmpdir is not defined").isTrue()

        tempDir = File(globalTempDir, "cgeogpxesTest")
        FileUtils.mkdirs(tempDir)
        assertThat(tempDir).overridingErrorMessage("Could not create directory %s", tempDir.getPath()).exists()
        // workaround to get storage initialized
        DataStore.getAllStoredCachesCount(PseudoList.HISTORY_LIST.id)
        listId = DataStore.createList("cgeogpxesTest")
    }

    @After
    public Unit tearDown() {
        val search: SearchResult = DataStore.getBatchOfStoredCaches(null, listId)
        val cachesInList: List<Geocache> = ArrayList<>(search.getCachesFromSearchResult(LoadFlags.LOAD_CACHE_OR_DB))
        DataStore.markDropped(cachesInList)
        DataStore.removeList(listId)
        FileUtils.deleteDirectory(tempDir)
        serviceThread.quit()
    }
}
