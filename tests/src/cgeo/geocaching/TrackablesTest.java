package cgeo.geocaching;

import cgeo.geocaching.test.AbstractResourceInstrumentationTestCase;
import cgeo.geocaching.test.R;
import cgeo.geocaching.utils.BaseUtils;

import java.util.List;

public class TrackablesTest extends AbstractResourceInstrumentationTestCase {

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

    public void testParseTrackableWithoutReleaseDate() {
        cgTrackable trackable = cgBase.parseTrackable(getFileContent(R.raw.tb14wfv), null, null);
        assertNotNull(trackable);
        assertEquals("The Brickster", trackable.getName());
        assertEquals("Adrian C", trackable.getOwner());
        assertTrue(trackable.getGoal().startsWith("I'm on the run from the law."));
        assertTrue(trackable.getGoal().endsWith("what I've seen."));
        assertTrue(trackable.getDistance() >= 11663.5f);
        // the next two items are normally available for trackables, but not for this one, so explicitly test for null
        assertNull(trackable.getReleased());
        assertNull(trackable.getOrigin());
    }

    private cgTrackable getTB2R124() {
        return cgBase.parseTrackable(BaseUtils.replaceWhitespace(getFileContent(R.raw.trackable_tb2r124)), null, null);
    }
}
