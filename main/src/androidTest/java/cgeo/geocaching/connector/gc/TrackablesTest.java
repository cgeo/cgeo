package cgeo.geocaching.connector.gc;

import cgeo.geocaching.log.LogEntry;
import cgeo.geocaching.log.LogType;
import cgeo.geocaching.models.Image;
import cgeo.geocaching.models.Trackable;
import cgeo.geocaching.test.CgeoTestUtils;
import cgeo.geocaching.test.R;
import cgeo.geocaching.utils.TextUtils;

import java.util.List;

import org.junit.Test;
import static org.assertj.core.api.Java6Assertions.assertThat;

public class TrackablesTest {

    @Test
    public void testTrackable() {
        final Trackable trackable = getTB2R124();
        assertThat(trackable.getGeocode()).isEqualTo("TB2R124");
        assertThat(trackable.getName()).isEqualTo("Bor. Dortmund - FC Schalke 04");
        assertThat(trackable.getOwner()).isEqualTo("Spiridon Lui");
    }

    @Test
    public void testTrackableWithoutImage() {
        final Trackable trackable = getTB2R124();
        assertThat(trackable.getImage()).isNull();
        assertThat(trackable.getDetails()).isNotNull();
    }

    @Test
    public void testTrackableWithLogImages() {
        final Trackable trackable = parseTrackable(R.raw.tbxatg);
        assertThat(trackable.getGeocode()).isEqualTo("TBXATG");

        final List<LogEntry> log = trackable.getLogs();
        assertThat(log).isNotNull();
        assertThat(log).hasSize(10);

        final List<Image> logImages = log.get(5).logImages;
        assertThat(logImages).isNotNull();
        assertThat(logImages).hasSize(1);
        assertThat(logImages.get(0).getUrl()).isEqualTo("https://img.geocaching.com/track/log/large/3dc286d2-671e-4502-937a-f1bd35a13813.jpg");
        assertThat(logImages.get(0).getTitle()).isEqualTo("@Osaka");

        for (final LogEntry entry : log) {
            assertThat(entry.log.startsWith("<div>")).isFalse();
        }
        assertThat(log.get(1).log).isEqualTo("Dropped in Una Bhan (GC49XCJ)");
    }

    @Test
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

    @Test
    public void testParseTrackableWithRetrievedDate() {
        final Trackable trackable = parseTrackable(R.raw.tb11jzk);
        assertThat(trackable).isNotNull();
        assertThat(trackable.getLogDate()).isNotNull();
        assertThat(trackable.getLogType()).isEqualTo(LogType.RETRIEVED_IT);
        assertThat(trackable.getLogGuid()).isEqualTo("2758cb91-a3b4-489f-9d99-1f5dd708c39f");
    }

    @Test
    public void testParseTrackableWithDiscoveredDate() {
        final Trackable trackable = parseTrackable(R.raw.tb84bz5);
        assertThat(trackable).isNotNull();
        assertThat(trackable.getLogDate()).isNotNull();
        assertThat(trackable.getLogType()).isEqualTo(LogType.DISCOVERED_IT);
        assertThat(trackable.getLogGuid()).isEqualTo("c8093cd3-db0d-40db-b9f3-3d1671309d34");
    }

    @Test
    public void testParseRelativeLink() {
        final Trackable trackable = parseTrackable(R.raw.tb4cwjx);
        assertThat(trackable).isNotNull();
        assertThat(trackable.getName()).isEqualTo("The Golden Lisa");
        final String goal = trackable.getGoal();
        assertThat(goal).isNotNull();
        assertThat(goal).doesNotContain("..");
        assertThat(goal).contains("href=\"https://www.geocaching.com/seek/cache_details.aspx?wp=GC3B7PD#\"");
    }

    private Trackable parseTrackable(final int trackablePage) {
        final String pageContent = CgeoTestUtils.getFileContent(trackablePage);
        return GCParser.parseTrackable(TextUtils.replaceWhitespace(pageContent), null);
    }

    @Test
    public void testParseMarkMissing() {
        final Trackable trackable = parseTrackable(R.raw.tb3f206);
        assertThat(trackable).isNotNull();
        final List<LogEntry> logs = trackable.getLogs();
        assertThat(logs).isNotNull();
        assertThat(logs).isNotEmpty();
        final LogEntry marked = logs.get(0);
        assertThat(marked.logType).isEqualTo(LogType.MARKED_MISSING);
    }

    private Trackable getTB2R124() {
        return parseTrackable(R.raw.trackable_tb2r124);
    }

    @Test
    public void testParseTrackableNotExisting() {
        final Trackable trackable = GCParser.parseTrackable(CgeoTestUtils.getFileContent(R.raw.tb_not_existing), null);
        assertThat(trackable).isNull();
    }

}
