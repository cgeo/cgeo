package cgeo.geocaching;

import cgeo.geocaching.test.R;

import android.test.InstrumentationTestCase;

import java.io.InputStream;
import java.util.List;
import java.util.Scanner;

public class TrackablesTest extends InstrumentationTestCase {

    public void testLogPageWithTrackables() {
        List<cgTrackableLog> tbLogs = cgBase.parseTrackableLog(getFileContent(R.raw.log_with_2tb));
        assertNotNull(tbLogs);
        assertEquals(2, tbLogs.size());
        cgTrackableLog log = tbLogs.get(0);
        assertEquals("Steffen's Kaiserwagen", log.name);
        assertEquals("1QG1EE", log.trackCode);
    }

    public void testLogPageWithoutTrackables() {
        List<cgTrackableLog> tbLogs = cgBase.parseTrackableLog(getFileContent(R.raw.log_without_tb));
        assertNotNull(tbLogs);
        assertEquals(0, tbLogs.size());
    }

    private String getFileContent(int resourceId) {
        InputStream ins = getInstrumentation().getContext().getResources().openRawResource(resourceId);
        return new Scanner(ins).useDelimiter("\\A").next();
    }
}
