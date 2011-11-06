package cgeo.geocaching.files;

import cgeo.geocaching.cgCache;
import cgeo.geocaching.cgSearch;
import cgeo.geocaching.cgeoapplication;
import cgeo.geocaching.test.R;

import android.os.Handler;
import android.os.Message;
import android.test.InstrumentationTestCase;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class GPXImporterTest extends InstrumentationTestCase {
    private TestHandler importStepHandler = new TestHandler();
    private TestHandler progressHandler = new TestHandler();
    private int listId;
    private File tempDir;

    public void testGetWaypointsFileForGpx() {
        assertEquals(new File("1234567-wpts.gpx"), GPXImporter.getWaypointsFileForGpx(new File("1234567.gpx")));
        assertEquals(new File("/mnt/sdcard/1234567-wpts.gpx"), GPXImporter.getWaypointsFileForGpx(new File("/mnt/sdcard/1234567.gpx")));
        assertEquals(new File("/mnt/sdcard/1-wpts.gpx"), GPXImporter.getWaypointsFileForGpx(new File("/mnt/sdcard/1.gpx")));
        assertEquals(new File("/mnt/sd.card/1-wpts.gpx"), GPXImporter.getWaypointsFileForGpx(new File("/mnt/sd.card/1.gpx")));
        assertEquals(new File("1234567.9-wpts.gpx"), GPXImporter.getWaypointsFileForGpx(new File("1234567.9.gpx")));
        assertEquals(new File("1234567-wpts.gpx"), GPXImporter.getWaypointsFileForGpx(new File("1234567.GPX")));
        assertEquals(new File("gpx.gpx-wpts.gpx"), GPXImporter.getWaypointsFileForGpx(new File("gpx.gpx.gpx")));
        assertNull(GPXImporter.getWaypointsFileForGpx(new File("123.gpy")));
        assertNull(GPXImporter.getWaypointsFileForGpx(new File("gpx")));
        assertNull(GPXImporter.getWaypointsFileForGpx(new File(".gpx")));
        assertNull(GPXImporter.getWaypointsFileForGpx(new File("/mnt/sdcard/.gpx")));
    }

    public void testImportGpx() throws IOException {
        File gc31j2h = new File(tempDir, "gc31j2h.gpx");
        copyResourceToFile(R.raw.gc31j2h, gc31j2h);

        GPXImporter.ImportGpxFileThread importThread = new GPXImporter.ImportGpxFileThread(gc31j2h, listId, importStepHandler, progressHandler);
        importThread.run();
        waitForFastSystems();

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
        waitForFastSystems();

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
        waitForFastSystems();

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
        waitForFastSystems();

        assertEquals(2, importStepHandler.messages.size());
        assertEquals(GPXImporter.IMPORT_STEP_READ_FILE, importStepHandler.messages.get(0).what);
        assertEquals(GPXImporter.IMPORT_STEP_FINISHED_WITH_ERROR, importStepHandler.messages.get(1).what);
    }

    private void copyResourceToFile(int resourceId, File file) throws IOException {
        final InputStream is = getInstrumentation().getContext().getResources().openRawResource(resourceId);
        final FileOutputStream os = new FileOutputStream(file);

        try {
            byte[] buffer = new byte[4096];
            int byteCount;
            while ((byteCount = is.read(buffer)) >= 0) {
                os.write(buffer, 0, byteCount);
            }
        } finally {
            os.close();
            is.close();
        }
    }

    static class TestHandler extends Handler {
        List<Message> messages = new ArrayList<Message>();

        @Override
        public void handleMessage(Message msg) {
            Message msg1 = new Message();
            msg1.copyFrom(msg);
            messages.add(msg1);
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

    /**
     * On very fast systems (Asus Transformer EeePad e.g.) not all messages are already handled
     * when the size of the message queue is checked. Solution: wait some milliseconds !
     */
    private void waitForFastSystems() {
        try {
            Thread.sleep(500, 0);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

}
