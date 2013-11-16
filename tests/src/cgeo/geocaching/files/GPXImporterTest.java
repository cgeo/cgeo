package cgeo.geocaching.files;

import cgeo.geocaching.DataStore;
import cgeo.geocaching.Geocache;
import cgeo.geocaching.SearchResult;
import cgeo.geocaching.enumerations.CacheType;
import cgeo.geocaching.enumerations.LoadFlags;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.settings.TestSettings;
import cgeo.geocaching.test.AbstractResourceInstrumentationTestCase;
import cgeo.geocaching.test.R;
import cgeo.geocaching.utils.CancellableHandler;
import cgeo.geocaching.utils.Log;

import org.apache.commons.lang3.StringUtils;

import android.net.Uri;
import android.os.Message;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public class GPXImporterTest extends AbstractResourceInstrumentationTestCase {
    private TestHandler importStepHandler = new TestHandler();
    private TestHandler progressHandler = new TestHandler();
    private int listId;
    private File tempDir;
    private boolean importCacheStaticMaps;
    private boolean importWpStaticMaps;

    public void testGetWaypointsFileNameForGpxFile() throws IOException {
        final String[] gpxFiles = new String[] { "1234567.gpx", "1.gpx", "1234567.9.gpx",
                "1234567.GPX", "gpx.gpx.gpx", ".gpx",
                "1234567_query.gpx", "123-4.gpx", "123(5).gpx" };
        final String[] wptsFiles = new String[] { "1234567-wpts.gpx", "1-wpts.gpx", "1234567.9-wpts.gpx",
                "1234567-wpts.GPX", "gpx.gpx-wpts.gpx", "-wpts.gpx",
                "1234567_query-wpts.gpx", "123-wpts-4.gpx", "123-wpts(5).gpx" };
        for (int i = 0; i < gpxFiles.length; i++) {
            String gpxFileName = gpxFiles[i];
            String wptsFileName = wptsFiles[i];
            File gpx = new File(tempDir, gpxFileName);
            File wpts = new File(tempDir, wptsFileName);
            // the files need to exist - we create them
            assertTrue(gpx.createNewFile());
            assertTrue(wpts.createNewFile());
            // the "real" method check
            assertEquals(wptsFileName, GPXImporter.getWaypointsFileNameForGpxFile(gpx));
            // they also need to be deleted, because of case sensitive tests that will not work correct on case insensitive file systems
            gpx.delete();
            wpts.delete();
        }
        final File gpx1 = new File(tempDir, "abc.gpx");
        assertNull(GPXImporter.getWaypointsFileNameForGpxFile(gpx1));
    }

    public void testImportGpx() throws IOException {
        final String geocode = "GC31J2H";
        removeCacheCompletely(geocode);
        final File gc31j2h = new File(tempDir, "gc31j2h.gpx");
        copyResourceToFile(R.raw.gc31j2h, gc31j2h);

        final GPXImporter.ImportGpxFileThread importThread = new GPXImporter.ImportGpxFileThread(gc31j2h, listId, importStepHandler, progressHandler);
        runImportThread(importThread);

        assertEquals(4, importStepHandler.messages.size());
        final Iterator<Message> iMsg = importStepHandler.messages.iterator();
        assertEquals(GPXImporter.IMPORT_STEP_START, iMsg.next().what);
        assertEquals(GPXImporter.IMPORT_STEP_READ_FILE, iMsg.next().what);
        assertEquals(GPXImporter.IMPORT_STEP_STORE_STATIC_MAPS, iMsg.next().what);
        assertEquals(GPXImporter.IMPORT_STEP_FINISHED, iMsg.next().what);
        final SearchResult search = (SearchResult) importStepHandler.messages.get(3).obj;
        assertEquals(Collections.singletonList(geocode), new ArrayList<String>(search.getGeocodes()));
        final Geocache cache = DataStore.loadCache(geocode, LoadFlags.LOAD_CACHE_OR_DB);
        assertCacheProperties(cache);

        assertTrue(cache.getWaypoints().isEmpty());
    }

    public void testImportOcGpx() throws IOException {
        final String geocode = "OCDDD2";
        removeCacheCompletely(geocode);
        final File ocddd2 = new File(tempDir, "ocddd2.gpx");
        copyResourceToFile(R.raw.ocddd2, ocddd2);

        final GPXImporter.ImportGpxFileThread importThread = new GPXImporter.ImportGpxFileThread(ocddd2, listId, importStepHandler, progressHandler);
        runImportThread(importThread);

        assertEquals(4, importStepHandler.messages.size());
        final Iterator<Message> iMsg = importStepHandler.messages.iterator();
        assertEquals(GPXImporter.IMPORT_STEP_START, iMsg.next().what);
        assertEquals(GPXImporter.IMPORT_STEP_READ_FILE, iMsg.next().what);
        assertEquals(GPXImporter.IMPORT_STEP_STORE_STATIC_MAPS, iMsg.next().what);
        assertEquals(GPXImporter.IMPORT_STEP_FINISHED, iMsg.next().what);
        final SearchResult search = (SearchResult) importStepHandler.messages.get(3).obj;
        assertEquals(Collections.singletonList(geocode), new ArrayList<String>(search.getGeocodes()));
        final Geocache cache = DataStore.loadCache(geocode, LoadFlags.LOAD_CACHE_OR_DB);
        assertCacheProperties(cache);

        assertEquals("Incorrect number of waypoints imported", 3, cache.getWaypoints().size());
    }

    private void runImportThread(GPXImporter.ImportThread importThread) {
        importThread.start();
        try {
            importThread.join();
        } catch (InterruptedException e) {
            Log.e("GPXImporterTest.runImportThread", e);
        }
        importStepHandler.waitForCompletion();
    }

    public void testImportGpxWithWaypoints() throws IOException {
        final File gc31j2h = new File(tempDir, "gc31j2h.gpx");
        copyResourceToFile(R.raw.gc31j2h, gc31j2h);
        copyResourceToFile(R.raw.gc31j2h_wpts, new File(tempDir, "gc31j2h-wpts.gpx"));

        final GPXImporter.ImportGpxFileThread importThread = new GPXImporter.ImportGpxFileThread(gc31j2h, listId, importStepHandler, progressHandler);
        runImportThread(importThread);

        assertImportStepMessages(GPXImporter.IMPORT_STEP_START, GPXImporter.IMPORT_STEP_READ_FILE, GPXImporter.IMPORT_STEP_READ_WPT_FILE, GPXImporter.IMPORT_STEP_STORE_STATIC_MAPS, GPXImporter.IMPORT_STEP_FINISHED);
        final SearchResult search = (SearchResult) importStepHandler.messages.get(4).obj;
        assertEquals(Collections.singletonList("GC31J2H"), new ArrayList<String>(search.getGeocodes()));
        final Geocache cache = DataStore.loadCache("GC31J2H", LoadFlags.LOAD_CACHE_OR_DB);
        assertCacheProperties(cache);
        assertEquals(2, cache.getWaypoints().size());
    }

    public void testImportGpxWithLowercaseNames() throws IOException {
        final File tc2012 = new File(tempDir, "tc2012.gpx");
        copyResourceToFile(R.raw.tc2012, tc2012);

        final GPXImporter.ImportGpxFileThread importThread = new GPXImporter.ImportGpxFileThread(tc2012, listId, importStepHandler, progressHandler);
        runImportThread(importThread);

        assertImportStepMessages(GPXImporter.IMPORT_STEP_START, GPXImporter.IMPORT_STEP_READ_FILE, GPXImporter.IMPORT_STEP_STORE_STATIC_MAPS, GPXImporter.IMPORT_STEP_FINISHED);
        final Geocache cache = DataStore.loadCache("AID1", LoadFlags.LOAD_CACHE_OR_DB);
        assertCacheProperties(cache);
        assertEquals("First Aid Station #1", cache.getName());
    }

    private void assertImportStepMessages(int... importSteps) {
        assertEquals(importSteps.length, importStepHandler.messages.size());
        for (int i = 0; i < importSteps.length; i++) {
            assertEquals(importSteps[i], importStepHandler.messages.get(i).what);
        }
    }

    public void testImportLoc() throws IOException {
        final File oc5952 = new File(tempDir, "oc5952.loc");
        copyResourceToFile(R.raw.oc5952_loc, oc5952);

        final GPXImporter.ImportLocFileThread importThread = new GPXImporter.ImportLocFileThread(oc5952, listId, importStepHandler, progressHandler);
        runImportThread(importThread);

        assertImportStepMessages(GPXImporter.IMPORT_STEP_START, GPXImporter.IMPORT_STEP_READ_FILE, GPXImporter.IMPORT_STEP_STORE_STATIC_MAPS, GPXImporter.IMPORT_STEP_FINISHED);
        final SearchResult search = (SearchResult) importStepHandler.messages.get(3).obj;
        assertEquals(Collections.singletonList("OC5952"), new ArrayList<String>(search.getGeocodes()));
        final Geocache cache = DataStore.loadCache("OC5952", LoadFlags.LOAD_CACHE_OR_DB);
        assertCacheProperties(cache);
    }

    private static void assertCacheProperties(Geocache cache) {
        assertNotNull(cache);
        assertFalse(cache.getLocation().startsWith(","));
        assertTrue(cache.isReliableLatLon());
    }

    public void testImportGpxError() throws IOException {
        final File gc31j2h = new File(tempDir, "gc31j2h.gpx");
        copyResourceToFile(R.raw.gc31j2h_err, gc31j2h);

        final GPXImporter.ImportGpxFileThread importThread = new GPXImporter.ImportGpxFileThread(gc31j2h, listId, importStepHandler, progressHandler);
        runImportThread(importThread);

        assertImportStepMessages(GPXImporter.IMPORT_STEP_START, GPXImporter.IMPORT_STEP_READ_FILE, GPXImporter.IMPORT_STEP_READ_FILE, GPXImporter.IMPORT_STEP_FINISHED_WITH_ERROR);
    }

    public void testImportGpxCancel() throws IOException {
        final File gc31j2h = new File(tempDir, "gc31j2h.gpx");
        copyResourceToFile(R.raw.gc31j2h, gc31j2h);

        progressHandler.cancel();
        final GPXImporter.ImportGpxFileThread importThread = new GPXImporter.ImportGpxFileThread(gc31j2h, listId, importStepHandler, progressHandler);
        runImportThread(importThread);

        assertImportStepMessages(GPXImporter.IMPORT_STEP_START, GPXImporter.IMPORT_STEP_READ_FILE, GPXImporter.IMPORT_STEP_CANCELED);
    }

    public void testImportGpxAttachment() {
        final String geocode = "GC31J2H";
        removeCacheCompletely(geocode);
        final Uri uri = Uri.parse("android.resource://cgeo.geocaching.test/raw/gc31j2h");

        final GPXImporter.ImportGpxAttachmentThread importThread = new GPXImporter.ImportGpxAttachmentThread(uri, getInstrumentation().getContext().getContentResolver(), listId, importStepHandler, progressHandler);
        runImportThread(importThread);

        assertImportStepMessages(GPXImporter.IMPORT_STEP_START, GPXImporter.IMPORT_STEP_READ_FILE, GPXImporter.IMPORT_STEP_STORE_STATIC_MAPS, GPXImporter.IMPORT_STEP_FINISHED);
        final SearchResult search = (SearchResult) importStepHandler.messages.get(3).obj;
        assertEquals(Collections.singletonList(geocode), new ArrayList<String>(search.getGeocodes()));
        final Geocache cache = DataStore.loadCache(geocode, LoadFlags.LOAD_CACHE_OR_DB);
        assertCacheProperties(cache);

        assertTrue(cache.getWaypoints().isEmpty());
    }

    public void testImportGpxZip() throws IOException {
        final String geocode = "GC31J2H";
        removeCacheCompletely(geocode);
        final File pq7545915 = new File(tempDir, "7545915.zip");
        copyResourceToFile(R.raw.pq7545915, pq7545915);

        final GPXImporter.ImportGpxZipFileThread importThread = new GPXImporter.ImportGpxZipFileThread(pq7545915, listId, importStepHandler, progressHandler);
        runImportThread(importThread);

        assertImportStepMessages(GPXImporter.IMPORT_STEP_START, GPXImporter.IMPORT_STEP_READ_FILE, GPXImporter.IMPORT_STEP_READ_WPT_FILE, GPXImporter.IMPORT_STEP_STORE_STATIC_MAPS, GPXImporter.IMPORT_STEP_FINISHED);
        final SearchResult search = (SearchResult) importStepHandler.messages.get(4).obj;
        assertEquals(Collections.singletonList(geocode), new ArrayList<String>(search.getGeocodes()));
        final Geocache cache = DataStore.loadCache(geocode, LoadFlags.LOAD_CACHE_OR_DB);
        assertCacheProperties(cache);
        assertEquals(1, cache.getWaypoints().size()); // this is the original pocket query result without test waypoint
    }

    public void testImportGpxZipErr() throws IOException {
        final File pqError = new File(tempDir, "pq_error.zip");
        copyResourceToFile(R.raw.pq_error, pqError);

        final GPXImporter.ImportGpxZipFileThread importThread = new GPXImporter.ImportGpxZipFileThread(pqError, listId, importStepHandler, progressHandler);
        runImportThread(importThread);

        assertImportStepMessages(GPXImporter.IMPORT_STEP_START, GPXImporter.IMPORT_STEP_FINISHED_WITH_ERROR);
    }

    public void testImportGpxZipAttachment() {
        final String geocode = "GC31J2H";
        removeCacheCompletely(geocode);
        final Uri uri = Uri.parse("android.resource://cgeo.geocaching.test/raw/pq7545915");

        final GPXImporter.ImportGpxZipAttachmentThread importThread = new GPXImporter.ImportGpxZipAttachmentThread(uri, getInstrumentation().getContext().getContentResolver(), listId, importStepHandler, progressHandler);
        runImportThread(importThread);

        assertImportStepMessages(GPXImporter.IMPORT_STEP_START, GPXImporter.IMPORT_STEP_READ_FILE, GPXImporter.IMPORT_STEP_READ_WPT_FILE, GPXImporter.IMPORT_STEP_STORE_STATIC_MAPS, GPXImporter.IMPORT_STEP_FINISHED);
        final SearchResult search = (SearchResult) importStepHandler.messages.get(4).obj;
        assertEquals(Collections.singletonList(geocode), new ArrayList<String>(search.getGeocodes()));
        final Geocache cache = DataStore.loadCache(geocode, LoadFlags.LOAD_CACHE_OR_DB);
        assertCacheProperties(cache);
        assertEquals(1, cache.getWaypoints().size()); // this is the original pocket query result without test waypoint
    }

    static class TestHandler extends CancellableHandler {
        private final List<Message> messages = new ArrayList<Message>();
        private long lastMessage = System.currentTimeMillis();

        @Override
        public synchronized void handleRegularMessage(Message msg) {
            final Message msg1 = Message.obtain();
            msg1.copyFrom(msg);
            messages.add(msg1);
            lastMessage = System.currentTimeMillis();
            notify();
        }

        public synchronized void waitForCompletion(final long milliseconds, final int maxMessages) {
            try {
                while (System.currentTimeMillis() - lastMessage <= milliseconds && messages.size() <= maxMessages) {
                    wait(milliseconds);
                }
            } catch (InterruptedException e) {
                // intentionally left blank
            }
        }

        public void waitForCompletion() {
            // Use reasonable defaults
            waitForCompletion(1000, 10);
        }
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        final String globalTempDir = System.getProperty("java.io.tmpdir");
        assertTrue("java.io.tmpdir is not defined", StringUtils.isNotBlank(globalTempDir));

        tempDir = new File(globalTempDir, "cgeogpxesTest");
        tempDir.mkdir();
        assertTrue("Could not create directory " + tempDir.getPath(), tempDir.exists());
        // workaround to get storage initialized
        DataStore.getAllHistoryCachesCount();
        listId = DataStore.createList("cgeogpxesTest");

        importCacheStaticMaps = Settings.isStoreOfflineMaps();
        TestSettings.setStoreOfflineMaps(true);
        importWpStaticMaps = Settings.isStoreOfflineWpMaps();
        TestSettings.setStoreOfflineWpMaps(true);
    }

    @Override
    protected void tearDown() throws Exception {
        final SearchResult search = DataStore.getBatchOfStoredCaches(null, CacheType.ALL, listId);
        final List<Geocache> cachesInList = new ArrayList<Geocache>();
        cachesInList.addAll(search.getCachesFromSearchResult(LoadFlags.LOAD_CACHE_OR_DB));
        DataStore.markDropped(cachesInList);
        DataStore.removeList(listId);
        deleteDirectory(tempDir);
        TestSettings.setStoreOfflineMaps(importCacheStaticMaps);
        TestSettings.setStoreOfflineWpMaps(importWpStaticMaps);
        super.tearDown();
    }

    private static void deleteDirectory(File dir) {
        for (File f : dir.listFiles()) {
            if (f.isFile()) {
                f.delete();
            } else if (f.isDirectory()) {
                deleteDirectory(f);
            }
        }
        dir.delete();
    }

}
