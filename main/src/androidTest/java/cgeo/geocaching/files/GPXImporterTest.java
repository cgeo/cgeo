package cgeo.geocaching.files;

import cgeo.geocaching.SearchResult;
import cgeo.geocaching.connector.ConnectorFactory;
import cgeo.geocaching.connector.gc.GCConnector;
import cgeo.geocaching.connector.gc.GCUtils;
import cgeo.geocaching.enumerations.LoadFlags;
import cgeo.geocaching.list.PseudoList;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.models.Waypoint;
import cgeo.geocaching.storage.DataStore;
import cgeo.geocaching.test.AbstractResourceInstrumentationTestCase;
import cgeo.geocaching.test.CgeoTestUtils;
import cgeo.geocaching.test.R;
import cgeo.geocaching.utils.DisposableHandler;
import cgeo.geocaching.utils.FileUtils;
import cgeo.geocaching.utils.Log;

import android.net.Uri;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import static org.assertj.core.api.Java6Assertions.assertThat;

public class GPXImporterTest extends AbstractResourceInstrumentationTestCase {
    private TestHandler importStepHandler;
    private TestHandler progressHandler;
    private int listId;
    private File tempDir;
    private HandlerThread serviceThread;

    public void testGetWaypointsFileNameForGpxFile() throws IOException {
        final String[] gpxFiles = {"1234567.gpx", "1.gpx", "1234567.9.gpx",
                "1234567.GPX", "gpx.gpx.gpx", ".gpx",
                "1234567_query.gpx", "123-4.gpx", "123(5).gpx"};
        final String[] wptsFiles = {"1234567-wpts.gpx", "1-wpts.gpx", "1234567.9-wpts.gpx",
                "1234567-wpts.GPX", "gpx.gpx-wpts.gpx", "-wpts.gpx",
                "1234567_query-wpts.gpx", "123-wpts-4.gpx", "123-wpts(5).gpx"};
        for (int i = 0; i < gpxFiles.length; i++) {
            final String gpxFileName = gpxFiles[i];
            final String wptsFileName = wptsFiles[i];
            final File gpx = new File(tempDir, gpxFileName);
            final File wpts = new File(tempDir, wptsFileName);
            // the files need to exist - we create them
            assertThat(gpx.createNewFile()).isTrue();
            assertThat(wpts.createNewFile()).isTrue();
            // the "real" method check
            assertThat(GPXImporter.getWaypointsFileNameForGpxFile(gpx)).isEqualTo(wptsFileName);
            // they also need to be deleted, because of case sensitive tests that will not work correct on case insensitive file systems
            FileUtils.delete(gpx);
            FileUtils.delete(wpts);
        }
        final File gpx1 = new File(tempDir, "abc.gpx");
        assertThat(GPXImporter.getWaypointsFileNameForGpxFile(gpx1)).isNull();
    }

    public void testImportGpx() throws IOException {
        final String geocode = "GC31J2H";
        CgeoTestUtils.removeCacheCompletely(geocode);
        final File gc31j2h = new File(tempDir, "gc31j2h.gpx");
        CgeoTestUtils.copyResourceToFile(R.raw.gc31j2h, gc31j2h);

        final ImportGpxFileThread importThread = new ImportGpxFileThread(gc31j2h, listId, importStepHandler, progressHandler);
        runImportThread(importThread);

        assertImportStepMessages(GPXImporter.IMPORT_STEP_START, GPXImporter.IMPORT_STEP_READ_FILE, /*GPXImporter.IMPORT_STEP_STORE_STATIC_MAPS,*/ GPXImporter.IMPORT_STEP_FINISHED);
        final Geocache cache = DataStore.loadCache(geocode, LoadFlags.LOAD_CACHE_OR_DB);
        assert cache != null;
        assertThat(cache).isNotNull();
        assertCacheProperties(cache);

        assertThat(cache.getWaypoints()).isEmpty();
    }

    public void testImportOcGpx() throws IOException {
        final String geocode = "OCDDD2";
        CgeoTestUtils.removeCacheCompletely(geocode);
        final File ocddd2 = new File(tempDir, "ocddd2.gpx");
        CgeoTestUtils.copyResourceToFile(R.raw.ocddd2, ocddd2);

        final ImportGpxFileThread importThread = new ImportGpxFileThread(ocddd2, listId, importStepHandler, progressHandler);
        runImportThread(importThread);

        assertImportStepMessages(GPXImporter.IMPORT_STEP_START, GPXImporter.IMPORT_STEP_READ_FILE, /*GPXImporter.IMPORT_STEP_STORE_STATIC_MAPS,*/ GPXImporter.IMPORT_STEP_FINISHED);
        final Geocache cache = DataStore.loadCache(geocode, LoadFlags.LOAD_CACHE_OR_DB);
        assert cache != null;
        assertThat(cache).isNotNull();
        assertCacheProperties(cache);

        assertThat(cache.getWaypoints()).as("Number of imported waypoints").hasSize(4);
    }

    public void testImportOcGpxEmptyCoord() throws IOException {
        final String geocode = "OCDDD2";
        CgeoTestUtils.removeCacheCompletely(geocode);
        final File ocddd2 = new File(tempDir, "ocddd2_empty_coord.gpx");
        CgeoTestUtils.copyResourceToFile(R.raw.ocddd2_empty_coord, ocddd2);

        final ImportGpxFileThread importThread = new ImportGpxFileThread(ocddd2, listId, importStepHandler, progressHandler);
        runImportThread(importThread);

        assertImportStepMessages(GPXImporter.IMPORT_STEP_START, GPXImporter.IMPORT_STEP_READ_FILE, /*GPXImporter.IMPORT_STEP_STORE_STATIC_MAPS,*/ GPXImporter.IMPORT_STEP_FINISHED);
        final Geocache cache = DataStore.loadCache(geocode, LoadFlags.LOAD_CACHE_OR_DB);
        assert cache != null;
        assertThat(cache).isNotNull();
        assertCacheProperties(cache);

        final List<Waypoint> waypointList = cache.getWaypoints();
        assertThat(waypointList).isNotNull();
        assertThat(waypointList).as("Number of imported waypoints").hasSize(8);
        assertThat(waypointList.get(3).getCoords()).as("Not empty coordinates").isNotNull();
        assertThat(waypointList.get(4).getCoords()).as("Empty coordinates").isNull();
        assertThat(waypointList.get(5).getCoords()).as("Empty coordinates").isNull();
        assertThat(waypointList.get(6).getCoords()).as("Not empty coordinates").isNotNull();
        assertThat(waypointList.get(7).getCoords()).as("Blank coordinates").isNull();
    }

    public void testImportGpxWithWaypoints() throws IOException {
        final String geocode = "GC31J2H";
        CgeoTestUtils.removeCacheCompletely(geocode);

        final File gc31j2h = new File(tempDir, "gc31j2h.gpx");
        CgeoTestUtils.copyResourceToFile(R.raw.gc31j2h, gc31j2h);
        CgeoTestUtils.copyResourceToFile(R.raw.gc31j2h_wpts, new File(tempDir, "gc31j2h-wpts.gpx"));

        final ImportGpxFileThread importThread = new ImportGpxFileThread(gc31j2h, listId, importStepHandler, progressHandler);
        runImportThread(importThread);

        assertImportStepMessages(GPXImporter.IMPORT_STEP_START, GPXImporter.IMPORT_STEP_READ_FILE, GPXImporter.IMPORT_STEP_READ_WPT_FILE, /*GPXImporter.IMPORT_STEP_STORE_STATIC_MAPS,*/ GPXImporter.IMPORT_STEP_FINISHED);
        final Geocache cache = DataStore.loadCache(geocode, LoadFlags.LOAD_CACHE_OR_DB);
        assert cache != null;
        assertThat(cache).isNotNull();
        assertCacheProperties(cache);
        assertThat(cache.getWaypoints()).hasSize(2);
    }

    public void testImportGpxWithWaypointsEmptyCoord() throws IOException {
        final String geocode = "GC31J2H";
        CgeoTestUtils.removeCacheCompletely(geocode);

        final File gc31j2h = new File(tempDir, "gc31j2h.gpx");
        CgeoTestUtils.copyResourceToFile(R.raw.gc31j2h, gc31j2h);
        CgeoTestUtils.copyResourceToFile(R.raw.gc31j2h_wpts_empty_coord, new File(tempDir, "gc31j2h-wpts.gpx"));

        final ImportGpxFileThread importThread = new ImportGpxFileThread(gc31j2h, listId, importStepHandler, progressHandler);
        runImportThread(importThread);

        assertImportStepMessages(GPXImporter.IMPORT_STEP_START, GPXImporter.IMPORT_STEP_READ_FILE, GPXImporter.IMPORT_STEP_READ_WPT_FILE, /*GPXImporter.IMPORT_STEP_STORE_STATIC_MAPS,*/ GPXImporter.IMPORT_STEP_FINISHED);
        final Geocache cache = DataStore.loadCache(geocode, LoadFlags.LOAD_CACHE_OR_DB);
        assert cache != null;
        assertThat(cache).isNotNull();
        assertCacheProperties(cache);

        final List<Waypoint> waypointList = cache.getWaypoints();
        assertThat(waypointList).isNotNull();
        assertThat(waypointList).as("Number of imported waypoints").hasSize(2);
        assertThat(waypointList.get(1).getCoords()).as("Empty Coordinates").isNull();
    }

    public void testImportGpxWithLowercaseNames() throws IOException {
        final File tc2012 = new File(tempDir, "tc2012.gpx");
        CgeoTestUtils.copyResourceToFile(R.raw.tc2012, tc2012);

        final ImportGpxFileThread importThread = new ImportGpxFileThread(tc2012, listId, importStepHandler, progressHandler);
        runImportThread(importThread);

        assertImportStepMessages(GPXImporter.IMPORT_STEP_START, GPXImporter.IMPORT_STEP_READ_FILE, /*GPXImporter.IMPORT_STEP_STORE_STATIC_MAPS,*/ GPXImporter.IMPORT_STEP_FINISHED);
        final Geocache cache = DataStore.loadCache("AID1", LoadFlags.LOAD_CACHE_OR_DB);
        assert cache != null;
        assertThat(cache).isNotNull();
        assertCacheProperties(cache);
        assertThat(cache.getName()).isEqualTo("First Aid Station #1");
    }


    private void runImportThread(final AbstractImportThread importThread) {
        importThread.start();
        try {
            importThread.join();
        } catch (final InterruptedException e) {
            Log.e("GPXImporterTest.runImportThread", e);
        }
        importStepHandler.sendEmptyMessage(TestHandler.TERMINATION_MESSAGE); // send End Message

        importStepHandler.waitForCompletion();
    }

    private void assertImportStepMessages(final int... expectedSteps) {
        final ArrayList<Message> messages = new ArrayList<>(importStepHandler.messages); // copy to avoid CME
        assertThat(messages).hasSize(expectedSteps.length);
        for (int i = 0; i < Math.min(expectedSteps.length, messages.size()); i++) {
            assertThat(messages.get(i).what).isEqualTo(expectedSteps[i]);
        }
    }

    public void testImportLoc() throws IOException {
        final File oc5952 = new File(tempDir, "oc5952.loc");
        CgeoTestUtils.copyResourceToFile(R.raw.oc5952_loc, oc5952);

        final ImportLocFileThread importThread = new ImportLocFileThread(oc5952, listId, importStepHandler, progressHandler);
        runImportThread(importThread);

        assertImportStepMessages(GPXImporter.IMPORT_STEP_START, GPXImporter.IMPORT_STEP_READ_FILE, /*GPXImporter.IMPORT_STEP_STORE_STATIC_MAPS,*/ GPXImporter.IMPORT_STEP_FINISHED);
        final Geocache cache = DataStore.loadCache("OC5952", LoadFlags.LOAD_CACHE_OR_DB);
        assertCacheProperties(cache);
    }

    private static void assertCacheProperties(final Geocache cache) {
        assertThat(cache).isNotNull();
        assertThat(cache.getLocation().startsWith(",")).isFalse();
        if (GCConnector.getInstance().equals(ConnectorFactory.getConnector(cache))) {
            assertThat(String.valueOf(GCUtils.gcCodeToGcId(cache.getGeocode()))).isEqualTo(cache.getCacheId());
        }
    }

    public void testImportGpxError() throws IOException {
        final File gc31j2h = new File(tempDir, "gc31j2h.gpx");
        CgeoTestUtils.copyResourceToFile(R.raw.gc31j2h_err, gc31j2h);

        final ImportGpxFileThread importThread = new ImportGpxFileThread(gc31j2h, listId, importStepHandler, progressHandler);
        runImportThread(importThread);

        assertImportStepMessages(GPXImporter.IMPORT_STEP_START, GPXImporter.IMPORT_STEP_READ_FILE, GPXImporter.IMPORT_STEP_READ_FILE, GPXImporter.IMPORT_STEP_FINISHED_WITH_ERROR);
    }

    public void testImportGpxCancel() throws IOException {
        final File gc31j2h = new File(tempDir, "gc31j2h.gpx");
        CgeoTestUtils.copyResourceToFile(R.raw.gc31j2h, gc31j2h);

        progressHandler.dispose();
        final ImportGpxFileThread importThread = new ImportGpxFileThread(gc31j2h, listId, importStepHandler, progressHandler);
        runImportThread(importThread);

        assertImportStepMessages(GPXImporter.IMPORT_STEP_START, GPXImporter.IMPORT_STEP_READ_FILE, GPXImporter.IMPORT_STEP_CANCELED);
    }

    public void testImportGpxAttachment() {
        final String geocode = "GC31J2H";
        CgeoTestUtils.removeCacheCompletely(geocode);
        final Uri uri = Uri.parse("android.resource://cgeo.geocaching.test/raw/gc31j2h");

        final ImportGpxAttachmentThread importThread = new ImportGpxAttachmentThread(uri, getInstrumentation().getContext().getContentResolver(), listId, importStepHandler, progressHandler);
        runImportThread(importThread);

        assertImportStepMessages(GPXImporter.IMPORT_STEP_START, GPXImporter.IMPORT_STEP_READ_FILE, /*GPXImporter.IMPORT_STEP_STORE_STATIC_MAPS,*/ GPXImporter.IMPORT_STEP_FINISHED);
        final Geocache cache = DataStore.loadCache(geocode, LoadFlags.LOAD_CACHE_OR_DB);
        assert cache != null;
        assertThat(cache).isNotNull();
        assertCacheProperties(cache);

        assertThat(cache.getWaypoints()).isEmpty();
    }

    public void testImportGpxZip() throws IOException {
        final String geocode = "GC31J2H";
        CgeoTestUtils.removeCacheCompletely(geocode);
        final File pq7545915 = new File(tempDir, "7545915.zip");
        CgeoTestUtils.copyResourceToFile(R.raw.pq7545915, pq7545915);

        final ImportGpxZipFileThread importThread = new ImportGpxZipFileThread(pq7545915, listId, importStepHandler, progressHandler);
        runImportThread(importThread);

        assertImportStepMessages(GPXImporter.IMPORT_STEP_START, GPXImporter.IMPORT_STEP_READ_FILE, GPXImporter.IMPORT_STEP_READ_WPT_FILE, /*GPXImporter.IMPORT_STEP_STORE_STATIC_MAPS,*/ GPXImporter.IMPORT_STEP_FINISHED);
        final Geocache cache = DataStore.loadCache(geocode, LoadFlags.LOAD_CACHE_OR_DB);
        assert cache != null;
        assertThat(cache).isNotNull();
        assertCacheProperties(cache);
        assertThat(cache.getWaypoints()).hasSize(1); // this is the original pocket query result without test waypoint
    }

    public void testImportGpxZipErr() throws IOException {
        final File pqError = new File(tempDir, "pq_error.zip");
        CgeoTestUtils.copyResourceToFile(R.raw.pq_error, pqError);

        final ImportGpxZipFileThread importThread = new ImportGpxZipFileThread(pqError, listId, importStepHandler, progressHandler);
        runImportThread(importThread);

        assertImportStepMessages(GPXImporter.IMPORT_STEP_START, GPXImporter.IMPORT_STEP_FINISHED_WITH_ERROR);
    }

    public void testImportGpxZipAttachment() {
        final String geocode = "GC31J2H";
        CgeoTestUtils.removeCacheCompletely(geocode);
        final Uri uri = Uri.parse("android.resource://cgeo.geocaching.test/raw/pq7545915");

        final ImportGpxZipAttachmentThread importThread = new ImportGpxZipAttachmentThread(uri, getInstrumentation().getContext().getContentResolver(), listId, importStepHandler, progressHandler);
        runImportThread(importThread);

        assertImportStepMessages(GPXImporter.IMPORT_STEP_START, GPXImporter.IMPORT_STEP_READ_FILE, GPXImporter.IMPORT_STEP_READ_WPT_FILE, /*GPXImporter.IMPORT_STEP_STORE_STATIC_MAPS,*/ GPXImporter.IMPORT_STEP_FINISHED);
        final Geocache cache = DataStore.loadCache(geocode, LoadFlags.LOAD_CACHE_OR_DB);
        assert cache != null;
        assertThat(cache).isNotNull();
        assertCacheProperties(cache);
        assertThat(cache.getWaypoints()).hasSize(1); // this is the original pocket query result without test waypoint
    }

    public void testImportGpxZipAttachmentCp437() {
        final String geocode = "GC448A";
        CgeoTestUtils.removeCacheCompletely(geocode);
        assertThat(R.raw.pq_cp437).isNotEqualTo(0); // avoid lint warning, fake usage of below resource
        final Uri uri = Uri.parse("android.resource://cgeo.geocaching.test/raw/pq_cp437");

        final ImportGpxZipAttachmentThread importThread = new ImportGpxZipAttachmentThread(uri, getInstrumentation().getContext().getContentResolver(), listId, importStepHandler, progressHandler);
        runImportThread(importThread);

        assertImportStepMessages(GPXImporter.IMPORT_STEP_START, GPXImporter.IMPORT_STEP_READ_FILE, /*GPXImporter.IMPORT_STEP_STORE_STATIC_MAPS,*/ GPXImporter.IMPORT_STEP_FINISHED);
        final Geocache cache = DataStore.loadCache(geocode, LoadFlags.LOAD_CACHE_OR_DB);
        assert cache != null;
        assertThat(cache).isNotNull();
        assertCacheProperties(cache);
        assertThat(cache.getWaypoints()).hasSize(0); // this is the original pocket query result without test waypoint
        assertThat(importThread.getSourceDisplayName()).isEqualTo("17157344_Großer Ümlaut Täst.gpx");
    }

    public void testImportGpxZipAttachmentEntities() {
        final String geocode = "GC448A";
        CgeoTestUtils.removeCacheCompletely(geocode);
        assertThat(R.raw.pq_entities).isNotEqualTo(0); // avoid lint warning, fake usage of below resource
        final Uri uri = Uri.parse("android.resource://cgeo.geocaching.test/raw/pq_entities");

        final ImportGpxZipAttachmentThread importThread = new ImportGpxZipAttachmentThread(uri, getInstrumentation().getContext().getContentResolver(), listId, importStepHandler, progressHandler);
        runImportThread(importThread);

        assertImportStepMessages(GPXImporter.IMPORT_STEP_START, GPXImporter.IMPORT_STEP_READ_FILE, /*GPXImporter.IMPORT_STEP_STORE_STATIC_MAPS,*/ GPXImporter.IMPORT_STEP_FINISHED);
        final Geocache cache = DataStore.loadCache(geocode, LoadFlags.LOAD_CACHE_OR_DB);
        assert cache != null;
        assertThat(cache).isNotNull();
        assertCacheProperties(cache);
        assertThat(cache.getWaypoints()).hasSize(0); // this is the original pocket query result without test waypoint
        assertThat(importThread.getSourceDisplayName()).isEqualTo("17157285_Großer Ümlaut Täst.gpx");
    }

    static class TestHandler extends DisposableHandler {
        private final List<Message> messages = new ArrayList<>();
        private long lastMessage = System.currentTimeMillis();
        private boolean receivedTerminationMessage = false;
        private static final int TERMINATION_MESSAGE = 9999;

        TestHandler(final Looper serviceLooper) {
            super(serviceLooper);
        }

        @Override
        public synchronized void handleRegularMessage(final Message msg) {
            final Message msg1 = Message.obtain();
            msg1.copyFrom(msg);
            if (msg1.what == TERMINATION_MESSAGE) {
                receivedTerminationMessage = true;
            } else {
                messages.add(msg1);
            }
            lastMessage = System.currentTimeMillis();
            notifyAll();
        }

        public synchronized void waitForCompletion(final long milliseconds) {
            try {
                while ((System.currentTimeMillis() - lastMessage <= milliseconds) && !hasTerminatingMessage()) {
                    wait(milliseconds);
                }
            } catch (final InterruptedException e) {
                // intentionally left blank
            }
        }

        private boolean hasTerminatingMessage() {
            return receivedTerminationMessage;
        }

        public void waitForCompletion() {
            // wait a maximum of 10 seconds
            waitForCompletion(10000);
        }
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        serviceThread = new HandlerThread("[" + getClass().getSimpleName() + "Thread]");
        serviceThread.start();
        final Looper serviceLooper = serviceThread.getLooper();
        importStepHandler = new TestHandler(serviceLooper);
        progressHandler = new TestHandler(serviceLooper);

        final String globalTempDir = System.getProperty("java.io.tmpdir");
        assertThat(StringUtils.isNotBlank(globalTempDir)).overridingErrorMessage("java.io.tmpdir is not defined").isTrue();

        tempDir = new File(globalTempDir, "cgeogpxesTest");
        FileUtils.mkdirs(tempDir);
        assertThat(tempDir).overridingErrorMessage("Could not create directory %s", tempDir.getPath()).exists();
        // workaround to get storage initialized
        DataStore.getAllStoredCachesCount(PseudoList.HISTORY_LIST.id);
        listId = DataStore.createList("cgeogpxesTest");
    }

    @Override
    protected void tearDown() throws Exception {
        final SearchResult search = DataStore.getBatchOfStoredCaches(null, listId);
        final List<Geocache> cachesInList = new ArrayList<>(search.getCachesFromSearchResult(LoadFlags.LOAD_CACHE_OR_DB));
        DataStore.markDropped(cachesInList);
        DataStore.removeList(listId);
        FileUtils.deleteDirectory(tempDir);
        serviceThread.quit();
        super.tearDown();
    }
}
