package cgeo.geocaching;

import cgeo.geocaching.test.R;
import cgeo.geocaching.utils.BaseUtils;

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

    public void testTrackable() {
        final cgTrackable trackable = getTB2R124();
        assertEquals("TB2R124", trackable.getGeocode());
        assertEquals("Bor. Dortmund - FC Schalke 04", trackable.getName());
        assertEquals("Spiridon Lui", trackable.getOwner());
    }

    public void testTrackableWithoutImage() {
        final cgTrackable trackable = getTB2R124();
        assertNull(trackable.getImage());
        assertNotNull(trackable.getDetails());
    }

    private String getFileContent(int resourceId) {
        InputStream ins = getInstrumentation().getContext().getResources().openRawResource(resourceId);
        return new Scanner(ins).useDelimiter("\\A").next();
    }

    private cgTrackable getTB2R124() {
        return cgBase.parseTrackable(BaseUtils.replaceWhitespace(getFileContent(R.raw.trackable_tb2r124)), null);
    }
}
