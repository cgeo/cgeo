package cgeo.geocaching;

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

public class cgeogpxesTest extends InstrumentationTestCase {
    private int listId;
    private cgeogpxes cgeogpxes = new cgeogpxes();
    private File tempDir;
    private TestHandler importStepHandler = new TestHandler();
    private TestHandler progressHandler = new TestHandler();

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        tempDir = new File(System.getProperty("java.io.tmpdir"), "cgeogpxesTest");
        tempDir.mkdir();

        listId = cgeoapplication.getInstance().createList("cgeogpxesTest");
    }

    @Override
    protected void tearDown() throws Exception {
        cgeoapplication.getInstance().dropStored(listId);
        cgeoapplication.getInstance().removeList(listId);
        deleteDirectory(tempDir);
        super.tearDown();
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

    static class TestHandler extends Handler {
        List<Message> messages = new ArrayList<Message>();

        @Override
        public void handleMessage(Message msg) {
            Message msg1 = new Message();
            msg1.copyFrom(msg);
            messages.add(msg1);
        }
    }

    public void testFileNameMatches() {
        assertTrue(cgeogpxes.filenameBelongsToList("1234567.gpx"));
        assertTrue(cgeogpxes.filenameBelongsToList("1234567.GPX"));
        assertTrue(cgeogpxes.filenameBelongsToList(".gpx"));
        assertTrue(cgeogpxes.filenameBelongsToList("1234567.loc"));
        assertTrue(cgeogpxes.filenameBelongsToList("1234567.LOC"));

        assertFalse(cgeogpxes.filenameBelongsToList("1234567.gpy"));
        assertFalse(cgeogpxes.filenameBelongsToList("1234567.agpx"));
        assertFalse(cgeogpxes.filenameBelongsToList("1234567"));
        assertFalse(cgeogpxes.filenameBelongsToList(""));
        assertFalse(cgeogpxes.filenameBelongsToList("gpx"));

        assertFalse(cgeogpxes.filenameBelongsToList("1234567-wpts.gpx"));
    }

    public void testImportGpx() throws IOException {
        File gc31j2h = new File(tempDir, "gc31j2h.gpx");
        copyResourceToFile(R.raw.gc31j2h, gc31j2h);

        cgeogpxes.ImportGpxFileThread importThread = new cgeogpxes.ImportGpxFileThread(gc31j2h, listId, importStepHandler, progressHandler);
        importThread.run();

        assertEquals(1, importStepHandler.messages.size());
        cgSearch search = (cgSearch) importStepHandler.messages.get(0).obj;
        assertEquals(Collections.singletonList("GC31J2H"), search.getGeocodes());

        cgCache cache = cgeoapplication.getInstance().getCacheByGeocode("GC31J2H");
        assertNotNull(cache);

        // can't assert, for whatever reason the waypoints are remembered in DB
        //        assertNull(cache.waypoints);
    }

    public void testImportGpxWithWaypoints() throws IOException {
        File gc31j2h = new File(tempDir, "gc31j2h.gpx");
        copyResourceToFile(R.raw.gc31j2h, gc31j2h);
        copyResourceToFile(R.raw.gc31j2h_wpts, new File(tempDir, "gc31j2h-wpts.gpx"));

        cgeogpxes.ImportGpxFileThread importThread = new cgeogpxes.ImportGpxFileThread(gc31j2h, listId, importStepHandler, progressHandler);
        importThread.run();

        assertEquals(1, importStepHandler.messages.size());
        cgSearch search = (cgSearch) importStepHandler.messages.get(0).obj;
        assertEquals(Collections.singletonList("GC31J2H"), search.getGeocodes());

        cgCache cache = cgeoapplication.getInstance().getCacheByGeocode("GC31J2H");
        assertNotNull(cache);
        assertEquals(2, cache.waypoints.size());
    }

    public void testImportLoc() throws IOException {
        File oc5952 = new File(tempDir, "oc5952.loc");
        copyResourceToFile(R.raw.oc5952_loc, oc5952);

        cgeogpxes.ImportLocFileThread importThread = new cgeogpxes.ImportLocFileThread(oc5952, listId, importStepHandler, progressHandler);
        importThread.run();

        assertEquals(1, importStepHandler.messages.size());
        cgSearch search = (cgSearch) importStepHandler.messages.get(0).obj;
        assertEquals(Collections.singletonList("OC5952"), search.getGeocodes());

        cgCache cache = cgeoapplication.getInstance().getCacheByGeocode("OC5952");
        assertNotNull(cache);
    }
}
