package cgeo.geocaching.connector.gc;

import cgeo.geocaching.Image;
import cgeo.geocaching.LogEntry;
import cgeo.geocaching.Trackable;
import cgeo.geocaching.TrackableLog;
import cgeo.geocaching.enumerations.LogType;
import cgeo.geocaching.test.AbstractResourceInstrumentationTestCase;
import cgeo.geocaching.test.R;
import cgeo.geocaching.utils.TextUtils;

import java.util.List;

public class TrackablesTest extends AbstractResourceInstrumentationTestCase {

    public void testLogPageWithTrackables() {
        final List<TrackableLog> tbLogs = GCParser.parseTrackableLog(getFileContent(R.raw.log_with_2tb));
        assertNotNull(tbLogs);
        assertEquals(2, tbLogs.size());
        final TrackableLog log = tbLogs.get(0);
        assertEquals("Steffen's Kaiserwagen", log.name);
        assertEquals("1QG1EE", log.trackCode);
    }

    public void testLogPageWithoutTrackables() {
        final List<TrackableLog> tbLogs = GCParser.parseTrackableLog(getFileContent(R.raw.log_without_tb));
        assertNotNull(tbLogs);
        assertEquals(0, tbLogs.size());
    }

    public void testTrackable() {
        final Trackable trackable = getTB2R124();
        assertEquals("TB2R124", trackable.getGeocode());
        assertEquals("Bor. Dortmund - FC Schalke 04", trackable.getName());
        assertEquals("Spiridon Lui", trackable.getOwner());
    }

    public void testTrackableWithoutImage() {
        final Trackable trackable = getTB2R124();
        assertNull(trackable.getImage());
        assertNotNull(trackable.getDetails());
    }

    public void testTrackableWithLogImages() {
        final Trackable trackable = getTBXATG();
        assertEquals("TBXATG", trackable.getGeocode());

        final List<LogEntry> log = trackable.getLogs();
        assertNotNull(log);
        assertEquals(10, log.size());
        // log entry 4 has several images; just check first two

        final List<Image> log4Images = log.get(4).getLogImages();
        assertNotNull(log4Images);
        assertEquals(5, log4Images.size());
        assertEquals("http://img.geocaching.com/track/log/large/f2e24c50-394c-4d74-8fb4-87298d8bff1d.jpg", log4Images.get(0).getUrl());
        assertEquals("7b Welcome to Geowoodstock", log4Images.get(0).getTitle());
        assertEquals("http://img.geocaching.com/track/log/large/b57c29c3-134e-4202-a2a1-69ce8920b055.jpg", log4Images.get(1).getUrl());
        assertEquals("8 Crater Lake Natl Park Oregon", log4Images.get(1).getTitle());

        // third log entry has one image
        final List<Image> log5Images = log.get(5).getLogImages();
        assertNotNull(log5Images);
        assertEquals(1, log5Images.size());
        assertEquals("http://img.geocaching.com/track/log/large/0096b42d-4d10-45fa-9be2-2d08c0d5cc61.jpg", log5Images.get(0).getUrl());
        assertEquals("Traverski&#39;s GC Univ coin on tour", log5Images.get(0).getTitle());

        for (LogEntry entry : log) {
            assertFalse(entry.log.startsWith("<div>"));
        }
        assertEquals("traveling", log.get(0).log);
    }

    public void testParseTrackableWithoutReleaseDate() {
        final Trackable trackable = parseTrackable(R.raw.tb14wfv);
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

    public void testParseRelativeLink() {
        final Trackable trackable = parseTrackable(R.raw.tb4cwjx);
        assertNotNull(trackable);
        assertEquals("The Golden Lisa", trackable.getName());
        final String goal = trackable.getGoal();
        assertNotNull(goal);
        assertFalse(goal.contains(".."));
        assertTrue(goal.contains("href=\"http://www.geocaching.com/seek/cache_details.aspx?wp=GC3B7PD#\""));
    }

    public void testParseSpeedManagerCompressedTrackable() {
        final Trackable tbNormal = parseTrackable(R.raw.tb54vjj_no_speedmanager_html);
        assertTB54VJJ(tbNormal);
        final Trackable tbCompressed = parseTrackable(R.raw.tb54vjj_speedmanager_html);
        assertTB54VJJ(tbCompressed);
    }

    private static void assertTB54VJJ(final Trackable trackable) {
        assertNotNull(trackable);
        assertEquals("Krtek - Der kleine Maulwurf", trackable.getName());
        final String goal = trackable.getGoal();
        assertNotNull(goal);
        assertTrue(goal.startsWith("Bei meinem Besitzer auf der Couch"));
        assertTrue(goal.endsWith("Geocachern zusammen fotografieren."));
        assertEquals("Der kleine Maulwurf in etwas gr&ouml;&szlig;er :-)", trackable.getDetails());
        assertEquals("TB54VJJ", trackable.getGeocode());
        assertEquals("Nordrhein-Westfalen, Germany", trackable.getOrigin());
        assertEquals("Lineflyer", trackable.getOwner());
        // the icon url is manipulated during compression
        assertTrue(trackable.getIconUrl().endsWith("www.geocaching.com/images/wpttypes/21.gif"));
        assertTrue(trackable.getImage().endsWith("img.geocaching.com/track/display/d9a475fa-da90-43ec-aec0-92afe26163e1.jpg"));
        assertEquals("d11a3e3d-7db0-4d43-87f2-7893238844a6", trackable.getOwnerGuid());
        assertNull(trackable.getSpottedGuid());
        assertEquals(Trackable.SPOTTED_OWNER, trackable.getSpottedType());
        assertNotNull(trackable.getReleased());
        assertEquals("Travel Bug Dog Tag", trackable.getType());
        final List<LogEntry> logs = trackable.getLogs();
        assertNotNull(logs);
        assertEquals(10, logs.size());
    }

    private Trackable parseTrackable(int trackablePage) {
        final String pageContent = getFileContent(trackablePage);
        return GCParser.parseTrackable(TextUtils.replaceWhitespace(pageContent), null);
    }

    public void testParseMarkMissing() {
        final Trackable trackable = parseTrackable(R.raw.tb29ggq);
        assertNotNull(trackable);
        final List<LogEntry> logs = trackable.getLogs();
        assertNotNull(logs);
        assertFalse(logs.isEmpty());
        final LogEntry marked = logs.get(4);
        assertEquals(LogType.MARKED_MISSING, marked.type);
    }

    private Trackable getTB2R124() {
        return parseTrackable(R.raw.trackable_tb2r124);
    }

    private Trackable getTBXATG() {
        return parseTrackable(R.raw.trackable_tbxatg);
    }

    public void testParseTrackableNotExisting() {
        final Trackable trackable = GCParser.parseTrackable(getFileContent(R.raw.tb_not_existing), null);
        assertNull(trackable);
    }

}
