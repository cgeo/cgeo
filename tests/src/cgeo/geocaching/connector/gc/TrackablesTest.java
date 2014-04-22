package cgeo.geocaching.connector.gc;

import static org.assertj.core.api.Assertions.assertThat;

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
        assertThat(tbLogs).isNotNull();
        assertThat(tbLogs).hasSize(2);
        final TrackableLog log = tbLogs.get(0);
        assertThat(log.name).isEqualTo("Steffen's Kaiserwagen");
        assertThat(log.trackCode).isEqualTo("1QG1EE");
    }

    public void testLogPageWithoutTrackables() {
        final List<TrackableLog> tbLogs = GCParser.parseTrackableLog(getFileContent(R.raw.log_without_tb));
        assertThat(tbLogs).isNotNull();
        assertThat(tbLogs).isEmpty();
    }

    public void testTrackable() {
        final Trackable trackable = getTB2R124();
        assertThat(trackable.getGeocode()).isEqualTo("TB2R124");
        assertThat(trackable.getName()).isEqualTo("Bor. Dortmund - FC Schalke 04");
        assertThat(trackable.getOwner()).isEqualTo("Spiridon Lui");
    }

    public void testTrackableWithoutImage() {
        final Trackable trackable = getTB2R124();
        assertThat(trackable.getImage()).isNull();
        assertThat(trackable.getDetails()).isNotNull();
    }

    public void testTrackableWithLogImages() {
        final Trackable trackable = getTBXATG();
        assertThat(trackable.getGeocode()).isEqualTo("TBXATG");

        final List<LogEntry> log = trackable.getLogs();
        assertThat(log).isNotNull();
        assertThat(log).hasSize(10);

        // log entry 4 has several images; just check the first one
        final List<Image> log4Images = log.get(4).getLogImages();
        assertThat(log4Images).isNotNull();
        assertThat(log4Images).hasSize(1);
        assertThat(log4Images.get(0).getUrl()).isEqualTo("http://imgcdn.geocaching.com/track/log/large/3dc286d2-671e-4502-937a-f1bd35a13813.jpg");
        assertThat(log4Images.get(0).getTitle()).isEqualTo("@Osaka");

        for (LogEntry entry : log) {
            assertThat(entry.log.startsWith("<div>")).isFalse();
        }
        assertThat(log.get(0).log).isEqualTo("Dropped in Una Bhan (GC49XCJ)");
    }

    public void testParseTrackableWithoutReleaseDate() {
        final Trackable trackable = parseTrackable(R.raw.tb14wfv);
        assertThat(trackable).isNotNull();
        assertThat(trackable.getName()).isEqualTo("The Brickster");
        assertThat(trackable.getOwner()).isEqualTo("Adrian C");
        assertThat(trackable.getGoal().startsWith("I'm on the run from the law.")).isTrue();
        assertThat(trackable.getGoal().endsWith("what I've seen.")).isTrue();
        assertThat(trackable.getDistance() >= 11663.5f).isTrue();
        // the next two items are normally available for trackables, but not for this one, so explicitly test for null
        assertThat(trackable.getReleased()).isNull();
        assertThat(trackable.getOrigin()).isNull();
    }

    public void testParseRelativeLink() {
        final Trackable trackable = parseTrackable(R.raw.tb4cwjx);
        assertThat(trackable).isNotNull();
        assertThat(trackable.getName()).isEqualTo("The Golden Lisa");
        final String goal = trackable.getGoal();
        assertThat(goal).isNotNull();
        assertThat(goal.contains("..")).isFalse();
        assertThat(goal.contains("href=\"http://www.geocaching.com/seek/cache_details.aspx?wp=GC3B7PD#\"")).isTrue();
    }

    public void testParseSpeedManagerCompressedTrackable() {
        final Trackable tbNormal = parseTrackable(R.raw.tb54vjj_no_speedmanager_html);
        assertTB54VJJ(tbNormal);
        final Trackable tbCompressed = parseTrackable(R.raw.tb54vjj_speedmanager_html);
        assertTB54VJJ(tbCompressed);
    }

    private static void assertTB54VJJ(final Trackable trackable) {
        assertThat(trackable).isNotNull();
        assertThat(trackable.getName()).isEqualTo("Krtek - Der kleine Maulwurf");
        final String goal = trackable.getGoal();
        assertThat(goal).isNotNull();
        assertThat(goal.startsWith("Bei meinem Besitzer auf der Couch")).isTrue();
        assertThat(goal.endsWith("Geocachern zusammen fotografieren.")).isTrue();
        assertEquals("Der kleine Maulwurf in etwas gr&ouml;&szlig;er :-)", trackable.getDetails());
        assertThat(trackable.getGeocode()).isEqualTo("TB54VJJ");
        assertEquals("Nordrhein-Westfalen, Germany", trackable.getOrigin());
        assertThat(trackable.getOwner()).isEqualTo("Lineflyer");
        // the icon url is manipulated during compression
        assertThat(trackable.getIconUrl().endsWith("www.geocaching.com/images/wpttypes/21.gif")).isTrue();
        assertThat(trackable.getImage().endsWith("img.geocaching.com/track/display/d9a475fa-da90-43ec-aec0-92afe26163e1.jpg")).isTrue();
        assertThat(trackable.getOwnerGuid()).isEqualTo("d11a3e3d-7db0-4d43-87f2-7893238844a6");
        assertThat(trackable.getSpottedGuid()).isNull();
        assertThat(trackable.getSpottedType()).isEqualTo(Trackable.SPOTTED_OWNER);
        assertThat(trackable.getReleased()).isNotNull();
        assertThat(trackable.getType()).isEqualTo("Travel Bug Dog Tag");
        final List<LogEntry> logs = trackable.getLogs();
        assertThat(logs).isNotNull();
        assertThat(logs).hasSize(10);
    }

    private Trackable parseTrackable(int trackablePage) {
        final String pageContent = getFileContent(trackablePage);
        return GCParser.parseTrackable(TextUtils.replaceWhitespace(pageContent), null);
    }

    public void testParseMarkMissing() {
        final Trackable trackable = parseTrackable(R.raw.tb3f206);
        assertThat(trackable).isNotNull();
        final List<LogEntry> logs = trackable.getLogs();
        assertThat(logs).isNotNull();
        assertThat(logs.isEmpty()).isFalse();
        final LogEntry marked = logs.get(0);
        assertThat(marked.type).isEqualTo(LogType.MARKED_MISSING);
    }

    private Trackable getTB2R124() {
        return parseTrackable(R.raw.trackable_tb2r124);
    }

    private Trackable getTBXATG() {
        return parseTrackable(R.raw.trackable_tbxatg);
    }

    public void testParseTrackableNotExisting() {
        final Trackable trackable = GCParser.parseTrackable(getFileContent(R.raw.tb_not_existing), null);
        assertThat(trackable).isNull();
    }

}
