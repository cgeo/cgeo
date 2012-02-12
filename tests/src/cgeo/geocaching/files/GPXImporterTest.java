package cgeo.geocaching.files;

import cgeo.geocaching.SearchResult;
import cgeo.geocaching.Settings;
import cgeo.geocaching.cgCache;
import cgeo.geocaching.cgeoapplication;
import cgeo.geocaching.enumerations.LoadFlags;
import cgeo.geocaching.test.AbstractResourceInstrumentationTestCase;
import cgeo.geocaching.test.R;
import cgeo.geocaching.utils.CancellableHandler;

import android.net.Uri;
import android.os.Message;
import android.util.Log;

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

    public void testGetWaypointsFileNameForGpxFile() throws IOException {
        String[] gpxFiles = new String[] { "1234567.gpx", "1.gpx", "1234567.9.gpx",
                "1234567.GPX", "gpx.gpx.gpx", ".gpx",
                "1234567_query.gpx", "123-4.gpx", "123(5).gpx" };
        String[] wptsFiles = new String[] { "1234567-wpts.gpx", "1-wpts.gpx", "1234567.9-wpts.gpx",
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
        File gpx1 = new File(tempDir, "abc.gpx");
        assertNull(GPXImporter.getWaypointsFileNameForGpxFile(gpx1));
    }

    public void testImportGpx() throws IOException {
        File gc31j2h = new File(tempDir, "gc31j2h.gpx");
        copyResourceToFile(R.raw.gc31j2h, gc31j2h);

        GPXImporter.ImportGpxFileThread importThread = new GPXImporter.ImportGpxFileThread(gc31j2h, listId, importStepHandler, progressHandler);
        runImportThread(importThread);

        assertEquals(4, importStepHandler.messages.size());
        Iterator<Message> iMsg = importStepHandler.messages.iterator();
        assertEquals(GPXImporter.IMPORT_STEP_START, iMsg.next().what);
        assertEquals(GPXImporter.IMPORT_STEP_READ_FILE, iMsg.next().what);
        assertEquals(GPXImporter.IMPORT_STEP_STORE_CACHES, iMsg.next().what);
        assertEquals(GPXImporter.IMPORT_STEP_FINISHED, iMsg.next().what);
        SearchResult search = (SearchResult) importStepHandler.messages.get(3).obj;
        assertEquals(Collections.singletonList("GC31J2H"), new ArrayList<String>(search.getGeocodes()));

        cgCache cache = cgeoapplication.getInstance().loadCache("GC31J2H", LoadFlags.LOADCACHEORDB);
        assertCacheProperties(cache);

        // can't assert, for whatever reason the waypoints are remembered in DB
        //        assertNull(cache.waypoints);
    }

    private void runImportThread(GPXImporter.ImportThread importThread) {
        importThread.start();
        try {
            importThread.join();
        } catch (InterruptedException e) {
            Log.e(Settings.tag, "GPXImporterTest.runImportThread", e);
        }
        importStepHandler.waitForCompletion();
    }

    public void testImportGpxWithWaypoints() throws IOException {
        File gc31j2h = new File(tempDir, "gc31j2h.gpx");
        copyResourceToFile(R.raw.gc31j2h, gc31j2h);
        copyResourceToFile(R.raw.gc31j2h_wpts, new File(tempDir, "gc31j2h-wpts.gpx"));

        GPXImporter.ImportGpxFileThread importThread = new GPXImporter.ImportGpxFileThread(gc31j2h, listId, importStepHandler, progressHandler);
        runImportThread(importThread);

        assertImportStepMessages(GPXImporter.IMPORT_STEP_START, GPXImporter.IMPORT_STEP_READ_FILE, GPXImporter.IMPORT_STEP_READ_WPT_FILE, GPXImporter.IMPORT_STEP_STORE_CACHES, GPXImporter.IMPORT_STEP_FINISHED);
        SearchResult search = (SearchResult) importStepHandler.messages.get(4).obj;
        assertEquals(Collections.singletonList("GC31J2H"), new ArrayList<String>(search.getGeocodes()));

        cgCache cache = cgeoapplication.getInstance().loadCache("GC31J2H", LoadFlags.LOADCACHEORDB);
        assertCacheProperties(cache);
        assertEquals(2, cache.getWaypoints().size());
    }

    private void assertImportStepMessages(int... importSteps) {
        assertEquals(importSteps.length, importStepHandler.messages.size());
        for (int i = 0; i < importSteps.length; i++) {
            assertEquals(importSteps[i], importStepHandler.messages.get(i).what);
        }
    }

    public void testImportLoc() throws IOException {
        File oc5952 = new File(tempDir, "oc5952.loc");
        copyResourceToFile(R.raw.oc5952_loc, oc5952);

        GPXImporter.ImportLocFileThread importThread = new GPXImporter.ImportLocFileThread(oc5952, listId, importStepHandler, progressHandler);
        runImportThread(importThread);

        assertImportStepMessages(GPXImporter.IMPORT_STEP_START, GPXImporter.IMPORT_STEP_READ_FILE, GPXImporter.IMPORT_STEP_STORE_CACHES, GPXImporter.IMPORT_STEP_FINISHED);
        SearchResult search = (SearchResult) importStepHandler.messages.get(3).obj;
        assertEquals(Collections.singletonList("OC5952"), new ArrayList<String>(search.getGeocodes()));

        cgCache cache = cgeoapplication.getInstance().loadCache("OC5952", LoadFlags.LOADCACHEORDB);
        assertCacheProperties(cache);
    }

    private static void assertCacheProperties(cgCache cache) {
        assertNotNull(cache);
        assertFalse(cache.getLocation().startsWith(","));
        assertTrue(cache.isReliableLatLon());
    }

    public void testImportGpxError() throws IOException {
        File gc31j2h = new File(tempDir, "gc31j2h.gpx");
        copyResourceToFile(R.raw.gc31j2h_err, gc31j2h);

        GPXImporter.ImportGpxFileThread importThread = new GPXImporter.ImportGpxFileThread(gc31j2h, listId, importStepHandler, progressHandler);
        runImportThread(importThread);

        assertImportStepMessages(GPXImporter.IMPORT_STEP_START, GPXImporter.IMPORT_STEP_READ_FILE, GPXImporter.IMPORT_STEP_READ_FILE, GPXImporter.IMPORT_STEP_FINISHED_WITH_ERROR);
    }

    public void testImportGpxCancel() throws IOException {
        File gc31j2h = new File(tempDir, "gc31j2h.gpx");
        copyResourceToFile(R.raw.gc31j2h, gc31j2h);

        progressHandler.cancel();
        GPXImporter.ImportGpxFileThread importThread = new GPXImporter.ImportGpxFileThread(gc31j2h, listId, importStepHandler, progressHandler);
        runImportThread(importThread);

        assertImportStepMessages(GPXImporter.IMPORT_STEP_START, GPXImporter.IMPORT_STEP_READ_FILE, GPXImporter.IMPORT_STEP_CANCELED);
    }

    public void testImportGpxAttachment() {
        Uri uri = Uri.parse("android.resource://cgeo.geocaching.test/raw/gc31j2h");

        GPXImporter.ImportGpxAttachmentThread importThread = new GPXImporter.ImportGpxAttachmentThread(uri, getInstrumentation().getContext().getContentResolver(), listId, importStepHandler, progressHandler);
        runImportThread(importThread);

        assertImportStepMessages(GPXImporter.IMPORT_STEP_START, GPXImporter.IMPORT_STEP_READ_FILE, GPXImporter.IMPORT_STEP_STORE_CACHES, GPXImporter.IMPORT_STEP_FINISHED);
        SearchResult search = (SearchResult) importStepHandler.messages.get(3).obj;
        assertEquals(Collections.singletonList("GC31J2H"), new ArrayList<String>(search.getGeocodes()));

        cgCache cache = cgeoapplication.getInstance().loadCache("GC31J2H", LoadFlags.LOADCACHEORDB);
        assertCacheProperties(cache);

        // can't assert, for whatever reason the waypoints are remembered in DB
        //        assertNull(cache.waypoints);
    }

    public void testImportGpxZip() throws IOException {
        File pq7545915 = new File(tempDir, "7545915.zip");
        copyResourceToFile(R.raw.pq7545915, pq7545915);

        GPXImporter.ImportGpxZipFileThread importThread = new GPXImporter.ImportGpxZipFileThread(pq7545915, listId, importStepHandler, progressHandler);
        runImportThread(importThread);

        assertImportStepMessages(GPXImporter.IMPORT_STEP_START, GPXImporter.IMPORT_STEP_READ_FILE, GPXImporter.IMPORT_STEP_READ_WPT_FILE, GPXImporter.IMPORT_STEP_STORE_CACHES, GPXImporter.IMPORT_STEP_FINISHED);
        SearchResult search = (SearchResult) importStepHandler.messages.get(4).obj;
        assertEquals(Collections.singletonList("GC31J2H"), new ArrayList<String>(search.getGeocodes()));

        cgCache cache = cgeoapplication.getInstance().loadCache("GC31J2H", LoadFlags.LOADCACHEORDB);
        assertCacheProperties(cache);
        assertEquals(1, cache.getWaypoints().size()); // this is the original pocket query result without test waypoint
    }

    public void testImportGpxZipErr() throws IOException {
        File pqError = new File(tempDir, "pq_error.zip");
        copyResourceToFile(R.raw.pq_error, pqError);

        GPXImporter.ImportGpxZipFileThread importThread = new GPXImporter.ImportGpxZipFileThread(pqError, listId, importStepHandler, progressHandler);
        runImportThread(importThread);

        assertImportStepMessages(GPXImporter.IMPORT_STEP_START, GPXImporter.IMPORT_STEP_FINISHED_WITH_ERROR);
    }

    public void testImportGpxZipAttachment() {
        Uri uri = Uri.parse("android.resource://cgeo.geocaching.test/raw/pq7545915");

        GPXImporter.ImportGpxZipAttachmentThread importThread = new GPXImporter.ImportGpxZipAttachmentThread(uri, getInstrumentation().getContext().getContentResolver(), listId, importStepHandler, progressHandler);
        runImportThread(importThread);

        assertImportStepMessages(GPXImporter.IMPORT_STEP_START, GPXImporter.IMPORT_STEP_READ_FILE, GPXImporter.IMPORT_STEP_READ_WPT_FILE, GPXImporter.IMPORT_STEP_STORE_CACHES, GPXImporter.IMPORT_STEP_FINISHED);
        SearchResult search = (SearchResult) importStepHandler.messages.get(4).obj;
        assertEquals(Collections.singletonList("GC31J2H"), new ArrayList<String>(search.getGeocodes()));

        cgCache cache = cgeoapplication.getInstance().loadCache("GC31J2H", LoadFlags.LOADCACHEORDB);
        assertCacheProperties(cache);
        assertEquals(1, cache.getWaypoints().size()); // this is the original pocket query result without test waypoint
    }

    static class TestHandler extends CancellableHandler {
        private final List<Message> messages = new ArrayList<Message>();
        private long lastMessage = System.currentTimeMillis();

        @Override
        public synchronized void handleRegularMessage(Message msg) {
            final Message msg1 = new Message();
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

        tempDir = new File(System.getProperty("java.io.tmpdir"), "cgeogpxesTest");
        tempDir.mkdir();

        // workaround to get storage initialized
        cgeoapplication.getInstance().getAllHistoricCachesCount();
        listId = cgeoapplication.getInstance().createList("cgeogpxesTest");
    }

    @Override
    protected void tearDown() throws Exception {
        cgeoapplication.getInstance().dropList(listId);
        cgeoapplication.getInstance().removeList(listId);
        deleteDirectory(tempDir);
        super.tearDown();
    }

    private void deleteDirectory(File dir) {
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
