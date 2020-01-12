package cgeo.geocaching.connector.gc;

import cgeo.geocaching.log.LogEntry;
import cgeo.geocaching.log.LogType;
import cgeo.geocaching.models.Image;
import cgeo.geocaching.models.Trackable;
import cgeo.geocaching.test.AbstractResourceInstrumentationTestCase;
import cgeo.geocaching.test.R;
import cgeo.geocaching.utils.TextUtils;

import java.util.List;

import static org.assertj.core.api.Java6Assertions.assertThat;

public class TrackablesTest extends AbstractResourceInstrumentationTestCase {

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

        for (final LogEntry entry : log) {
            assertThat(entry.log.startsWith("<div>")).isFalse();
        }
        assertThat(log.get(0).log).isEqualTo("Dropped in Una Bhan (GC49XCJ)");
    }

    public void testParseTrackableWithoutReleaseDate() {
        final Trackable trackable = parseTrackable(R.raw.tb14wfv);
        assertThat(trackable).isNotNull();
        assertThat(trackable.getName()).isEqualTo("The Brickster");
        assertThat(trackable.getOwner()).isEqualTo("Adrian C");
        assertThat(trackable.getGoal()).startsWith("I'm on the run from the law.");
        assertThat(trackable.getGoal()).endsWith("what I've seen.");
        assertThat(trackable.getDistance()).isGreaterThanOrEqualTo(11663.5f);
        // the next two items are normally available for trackables, but not for this one, so explicitly test for null
        assertThat(trackable.getReleased()).isNull();
        assertThat(trackable.getOrigin()).isNull();
    }

    public void testParseTrackableWithRetrievedDate() {
        final Trackable trackable = parseTrackable(R.raw.tb11jzk);
        assertThat(trackable).isNotNull();
        assertThat(trackable.getLogDate()).isNotNull();
        assertThat(trackable.getLogType()).isEqualTo(LogType.RETRIEVED_IT);
        assertThat(trackable.getLogGuid()).isEqualTo("2758cb91-a3b4-489f-9d99-1f5dd708c39f");
    }

    public void testParseTrackableWithDiscoveredDate() {
        final Trackable trackable = parseTrackable(R.raw.tb84bz5);
        assertThat(trackable).isNotNull();
        assertThat(trackable.getLogDate()).isNotNull();
        assertThat(trackable.getLogType()).isEqualTo(LogType.DISCOVERED_IT);
        assertThat(trackable.getLogGuid()).isEqualTo("c8093cd3-db0d-40db-b9f3-3d1671309d34");
    }

    public void testParseRelativeLink() {
        final Trackable trackable = parseTrackable(R.raw.tb4cwjx);
        assertThat(trackable).isNotNull();
        assertThat(trackable.getName()).isEqualTo("The Golden Lisa");
        final String goal = trackable.getGoal();
        assertThat(goal).isNotNull();
        assertThat(goal).doesNotContain("..");
        assertThat(goal).contains("href=\"https://www.geocaching.com/seek/cache_details.aspx?wp=GC3B7PD#\"");
    }

//    // test data adaption necessary
//    public void testParseSpeedManagerCompressedTrackable() {
//        final Trackable tbNormal = parseTrackable(R.raw.tb54vjj_no_speedmanager_html);
//        assertTB54VJJ(tbNormal);
//        final Trackable tbCompressed = parseTrackable(R.raw.tb54vjj_speedmanager_html);
//        assertTB54VJJ(tbCompressed);
//    }

    private static void assertTB54VJJ(final Trackable trackable) {
        assertThat(trackable).isNotNull();
        assertThat(trackable.getName()).isEqualTo("Krtek - Der kleine Maulwurf");
        final String goal = trackable.getGoal();
        assertThat(goal).isNotNull();
        assertThat(goal).startsWith("Bei meinem Besitzer auf der Couch");
        assertThat(goal).endsWith("Geocachern zusammen fotografieren.");
        assertThat(trackable.getDetails()).isEqualTo("Der kleine Maulwurf in etwas gr&ouml;&szlig;er :-)");
        assertThat(trackable.getGeocode()).isEqualTo("TB54VJJ");
        assertThat(trackable.getOrigin()).isEqualTo("Nordrhein-Westfalen, Germany");
        assertThat(trackable.getOwner()).isEqualTo("Lineflyer");
        // the icon url is manipulated during compression
        assertThat(trackable.getIconUrl()).endsWith("www.geocaching.com/images/wpttypes/21.gif");
        assertThat(trackable.getImage()).endsWith("img.geocaching.com/track/large/d9a475fa-da90-43ec-aec0-92afe26163e1.jpg");
        assertThat(trackable.getOwnerGuid()).isEqualTo("d11a3e3d-7db0-4d43-87f2-7893238844a6");
        assertThat(trackable.getSpottedGuid()).isNull();
        assertThat(trackable.getSpottedType()).isEqualTo(Trackable.SPOTTED_OWNER);
        assertThat(trackable.getReleased()).isNotNull();
        assertThat(trackable.getLogDate()).isNull();
        assertThat(trackable.getLogType()).isNull();
        assertThat(trackable.getLogGuid()).isNull();
        assertThat(trackable.getType()).isEqualTo("Travel Bug Dog Tag");
        final List<LogEntry> logs = trackable.getLogs();
        assertThat(logs).isNotNull();
        assertThat(logs).hasSize(10);
    }

    private Trackable parseTrackable(final int trackablePage) {
        final String pageContent = getFileContent(trackablePage);
        return GCParser.parseTrackable(TextUtils.replaceWhitespace(pageContent), null);
    }

    public void testParseMarkMissing() {
        final Trackable trackable = parseTrackable(R.raw.tb3f206);
        assertThat(trackable).isNotNull();
        final List<LogEntry> logs = trackable.getLogs();
        assertThat(logs).isNotNull();
        assertThat(logs).isNotEmpty();
        final LogEntry marked = logs.get(0);
        assertThat(marked.getType()).isEqualTo(LogType.MARKED_MISSING);
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
