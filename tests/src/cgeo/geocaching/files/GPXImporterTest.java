package cgeo.geocaching.files;

import cgeo.geocaching.cgCache;
import cgeo.geocaching.cgSearch;
import cgeo.geocaching.cgeoapplication;
import cgeo.geocaching.test.AbstractResourceInstrumentationTestCase;
import cgeo.geocaching.test.R;
import cgeo.geocaching.utils.CancellableHandler;

import android.os.Message;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class GPXImporterTest extends AbstractResourceInstrumentationTestCase {
    private TestHandler importStepHandler = new TestHandler();
    private TestHandler progressHandler = new TestHandler();
    private int listId;
    private File tempDir;

    public void testGetWaypointsFileNameForGpxFileName() {
        assertEquals("1234567-wpts.gpx", GPXImporter.getWaypointsFileNameForGpxFileName("1234567.gpx"));
        assertEquals("/mnt/sdcard/1234567-wpts.gpx", GPXImporter.getWaypointsFileNameForGpxFileName("/mnt/sdcard/1234567.gpx"));
        assertEquals("/mnt/sdcard/1-wpts.gpx", GPXImporter.getWaypointsFileNameForGpxFileName("/mnt/sdcard/1.gpx"));
        assertEquals("/mnt/sd.card/1-wpts.gpx", GPXImporter.getWaypointsFileNameForGpxFileName("/mnt/sd.card/1.gpx"));
        assertEquals("1234567.9-wpts.gpx", GPXImporter.getWaypointsFileNameForGpxFileName("1234567.9.gpx"));
        assertEquals("1234567-wpts.gpx", GPXImporter.getWaypointsFileNameForGpxFileName("1234567.GPX"));
        assertEquals("gpx.gpx-wpts.gpx", GPXImporter.getWaypointsFileNameForGpxFileName("gpx.gpx.gpx"));
        assertEquals("/mnt/sdcard/-wpts.gpx", GPXImporter.getWaypointsFileNameForGpxFileName("/mnt/sdcard/.gpx"));
        assertNull(GPXImporter.getWaypointsFileNameForGpxFileName("123.gpy"));
        assertNull(GPXImporter.getWaypointsFileNameForGpxFileName("gpx"));
        assertNull(GPXImporter.getWaypointsFileNameForGpxFileName(".gpx"));
    }

    public void testGetGpxFileNameForZipFileName() {
        assertEquals("1234567.gpx", GPXImporter.getGpxFileNameForZipFileName("1234567.zip"));
        assertEquals("/mnt/sdcard/1234567.gpx", GPXImporter.getGpxFileNameForZipFileName("/mnt/sdcard/1234567.zip"));
        assertEquals("1.gpx", GPXImporter.getGpxFileNameForZipFileName("1.zip"));
        assertEquals("/mnt/sd.card/1.gpx", GPXImporter.getGpxFileNameForZipFileName("/mnt/sd.card/1.zip"));
        assertEquals("1234567.9.gpx", GPXImporter.getGpxFileNameForZipFileName("1234567.9.zip"));
        assertEquals("1234567.gpx", GPXImporter.getGpxFileNameForZipFileName("1234567.ZIP"));
        assertEquals("zip.zip.gpx", GPXImporter.getGpxFileNameForZipFileName("zip.zip.zip"));
        assertEquals("/mnt/sdcard/.gpx", GPXImporter.getGpxFileNameForZipFileName("/mnt/sdcard/.zip"));
        assertNull(GPXImporter.getGpxFileNameForZipFileName("123.zap"));
        assertNull(GPXImporter.getGpxFileNameForZipFileName("zip"));
        assertNull(GPXImporter.getGpxFileNameForZipFileName(".zip"));
    }

    public void testImportGpx() throws IOException {
        File gc31j2h = new File(tempDir, "gc31j2h.gpx");
        copyResourceToFile(R.raw.gc31j2h, gc31j2h);

        GPXImporter.ImportGpxFileThread importThread = new GPXImporter.ImportGpxFileThread(gc31j2h, listId, importStepHandler, progressHandler);
        importThread.run();
        importStepHandler.waitForCompletion();

        assertEquals(3, importStepHandler.messages.size());
        assertEquals(GPXImporter.IMPORT_STEP_READ_FILE, importStepHandler.messages.get(0).what);
        assertEquals(GPXImporter.IMPORT_STEP_STORE_CACHES, importStepHandler.messages.get(1).what);
        assertEquals(GPXImporter.IMPORT_STEP_FINISHED, importStepHandler.messages.get(2).what);
        cgSearch search = (cgSearch) importStepHandler.messages.get(2).obj;
        assertEquals(Collections.singletonList("GC31J2H"), search.getGeocodes());

        cgCache cache = cgeoapplication.getInstance().getCacheByGeocode("GC31J2H");
        assertCacheProperties(cache);

        // can't assert, for whatever reason the waypoints are remembered in DB
        //        assertNull(cache.waypoints);
    }

    public void testImportGpxWithWaypoints() throws IOException {
        File gc31j2h = new File(tempDir, "gc31j2h.gpx");
        copyResourceToFile(R.raw.gc31j2h, gc31j2h);
        copyResourceToFile(R.raw.gc31j2h_wpts, new File(tempDir, "gc31j2h-wpts.gpx"));

        GPXImporter.ImportGpxFileThread importThread = new GPXImporter.ImportGpxFileThread(gc31j2h, listId, importStepHandler, progressHandler);
        importThread.run();
        importStepHandler.waitForCompletion();

        assertEquals(4, importStepHandler.messages.size());
        assertEquals(GPXImporter.IMPORT_STEP_READ_FILE, importStepHandler.messages.get(0).what);
        assertEquals(GPXImporter.IMPORT_STEP_READ_WPT_FILE, importStepHandler.messages.get(1).what);
        assertEquals(GPXImporter.IMPORT_STEP_STORE_CACHES, importStepHandler.messages.get(2).what);
        assertEquals(GPXImporter.IMPORT_STEP_FINISHED, importStepHandler.messages.get(3).what);
        cgSearch search = (cgSearch) importStepHandler.messages.get(3).obj;
        assertEquals(Collections.singletonList("GC31J2H"), search.getGeocodes());

        cgCache cache = cgeoapplication.getInstance().getCacheByGeocode("GC31J2H");
        assertCacheProperties(cache);
        assertEquals(2, cache.getWaypoints().size());
    }

    public void testImportLoc() throws IOException {
        File oc5952 = new File(tempDir, "oc5952.loc");
        copyResourceToFile(R.raw.oc5952_loc, oc5952);

        GPXImporter.ImportLocFileThread importThread = new GPXImporter.ImportLocFileThread(oc5952, listId, importStepHandler, progressHandler);
        importThread.run();
        importStepHandler.waitForCompletion();

        assertEquals(3, importStepHandler.messages.size());
        assertEquals(GPXImporter.IMPORT_STEP_READ_FILE, importStepHandler.messages.get(0).what);
        assertEquals(GPXImporter.IMPORT_STEP_STORE_CACHES, importStepHandler.messages.get(1).what);
        assertEquals(GPXImporter.IMPORT_STEP_FINISHED, importStepHandler.messages.get(2).what);
        cgSearch search = (cgSearch) importStepHandler.messages.get(2).obj;
        assertEquals(Collections.singletonList("OC5952"), search.getGeocodes());

        cgCache cache = cgeoapplication.getInstance().getCacheByGeocode("OC5952");
        assertCacheProperties(cache);
    }

    private static void assertCacheProperties(cgCache cache) {
        assertNotNull(cache);
        assertFalse(cache.getLocation().startsWith(","));
    }

    public void testImportGpxError() throws IOException {
        File gc31j2h = new File(tempDir, "gc31j2h.gpx");
        copyResourceToFile(R.raw.gc31j2h_err, gc31j2h);

        GPXImporter.ImportGpxFileThread importThread = new GPXImporter.ImportGpxFileThread(gc31j2h, listId, importStepHandler, progressHandler);
        importThread.run();
        importStepHandler.waitForCompletion();

        assertEquals(2, importStepHandler.messages.size());
        assertEquals(GPXImporter.IMPORT_STEP_READ_FILE, importStepHandler.messages.get(0).what);
        assertEquals(GPXImporter.IMPORT_STEP_FINISHED_WITH_ERROR, importStepHandler.messages.get(1).what);
    }

    public void testImportGpxCancel() throws IOException {
        File gc31j2h = new File(tempDir, "gc31j2h.gpx");
        copyResourceToFile(R.raw.gc31j2h, gc31j2h);

        progressHandler.cancel();
        GPXImporter.ImportGpxFileThread importThread = new GPXImporter.ImportGpxFileThread(gc31j2h, listId, importStepHandler, progressHandler);
        importThread.run();
        importStepHandler.waitForCompletion();

        assertEquals(2, importStepHandler.messages.size());
        assertEquals(GPXImporter.IMPORT_STEP_READ_FILE, importStepHandler.messages.get(0).what);
        assertEquals(GPXImporter.IMPORT_STEP_CANCELED, importStepHandler.messages.get(1).what);
    }

    public void testImportGpxZip() throws IOException {
        File pq7545915 = new File(tempDir, "7545915.zip");
        copyResourceToFile(R.raw.pq7545915, pq7545915);

        GPXImporter.ImportGpxZipFileThread importThread = new GPXImporter.ImportGpxZipFileThread(pq7545915, listId, importStepHandler, progressHandler);
        importThread.run();
        importStepHandler.waitForCompletion();

        assertEquals(4, importStepHandler.messages.size());
        assertEquals(GPXImporter.IMPORT_STEP_READ_FILE, importStepHandler.messages.get(0).what);
        assertEquals(GPXImporter.IMPORT_STEP_READ_WPT_FILE, importStepHandler.messages.get(1).what);
        assertEquals(GPXImporter.IMPORT_STEP_STORE_CACHES, importStepHandler.messages.get(2).what);
        assertEquals(GPXImporter.IMPORT_STEP_FINISHED, importStepHandler.messages.get(3).what);
        cgSearch search = (cgSearch) importStepHandler.messages.get(3).obj;
        assertEquals(Collections.singletonList("GC31J2H"), search.getGeocodes());

        cgCache cache = cgeoapplication.getInstance().getCacheByGeocode("GC31J2H");
        assertCacheProperties(cache);
        assertEquals(1, cache.getWaypoints().size()); // this is the original pocket query result without test waypoint
    }

    public void testImportGpxZipErr() throws IOException {
        // zip file name doesn't match name of gpx entry
        File pq1 = new File(tempDir, "1.zip");
        copyResourceToFile(R.raw.pq7545915, pq1);

        GPXImporter.ImportGpxZipFileThread importThread = new GPXImporter.ImportGpxZipFileThread(pq1, listId, importStepHandler, progressHandler);
        importThread.run();
        importStepHandler.waitForCompletion();

        assertEquals(1, importStepHandler.messages.size());
        assertEquals(GPXImporter.IMPORT_STEP_FINISHED_WITH_ERROR, importStepHandler.messages.get(0).what);
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
            }
        }

        public void waitForCompletion() {
            // Use reasonable defaults
            waitForCompletion(200, 10);
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
        cgeoapplication.getInstance().dropStored(listId);
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
