package cgeo.geocaching.utils.gpx;

import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.models.Waypoint;

import java.io.InputStream;

import org.junit.Test;
import static org.assertj.core.api.Assertions.assertThat;

public class GPXUtilsTest {

    @Test
    public void testParseZipWithSeparateCacheAndWaypointFiles() throws Exception {
        final GPXParser parser = new GPXParser();
        final RecordingGPXParseHooks hooks = new RecordingGPXParseHooks();
        try (InputStream is = GPXParserTest.openResource("/pq7545915.zip")) {
            assertThat(GPXUtils.parseZip(is, parser, hooks)).isEqualTo(2);
        }

        // "7545915-wpts.gpx" sorts alphabetically BEFORE "7545915.gpx" - proves the two-pass ordering works
        assertThat(hooks.getGeocaches()).hasSize(1);
        final Geocache cache = hooks.getGeocaches().get(0);
        assertThat(cache.getGeocode()).isEqualTo("GC31J2H");
        assertThat(cache.getName()).isEqualTo("Hockenheimer City-Brunnen");

        assertThat(hooks.getWaypoints()).hasSize(1);
        final Waypoint waypoint = hooks.getWaypoints().get(0);
        assertThat(waypoint.getName()).isEqualTo("Parkplatz");
        assertThat(waypoint.getPrefix()).isEqualTo("00");
        assertThat(waypoint.getGeocode()).isEqualTo("GC31J2H");
    }

    @Test
    public void testParseZipWithNoGpxEntriesReturnsZeroWithoutThrowing() throws Exception {
        final GPXParser parser = new GPXParser();
        final RecordingGPXParseHooks hooks = new RecordingGPXParseHooks();
        try (InputStream is = GPXParserTest.openResource("/pq_error.zip")) {
            assertThat(GPXUtils.parseZip(is, parser, hooks)).isEqualTo(0);
        }
        assertThat(hooks.getGlobalItems()).isEmpty();
    }

    @Test
    public void testParseZipWithEntityEncodedFilename() throws Exception {
        final GPXParser parser = new GPXParser();
        final RecordingGPXParseHooks hooks = new RecordingGPXParseHooks();
        try (InputStream is = GPXParserTest.openResource("/pq_entities.zip")) {
            assertThat(GPXUtils.parseZip(is, parser, hooks)).isEqualTo(1);
        }
        assertThat(hooks.getGlobalItems()).isNotEmpty();
    }

    @Test
    public void testParseZipWithCp437EncodedFilename() throws Exception {
        final GPXParser parser = new GPXParser();
        final RecordingGPXParseHooks hooks = new RecordingGPXParseHooks();
        try (InputStream is = GPXParserTest.openResource("/pq_cp437.zip")) {
            assertThat(GPXUtils.parseZip(is, parser, hooks, "cp437")).isEqualTo(1);
        }
        assertThat(hooks.getGlobalItems()).isNotEmpty();
    }

    @Test
    public void testResetAllowsReuseOfParserAcrossUnrelatedZips() throws Exception {
        final GPXParser parser = new GPXParser();
        final RecordingGPXParseHooks firstHooks = new RecordingGPXParseHooks();
        try (InputStream is = GPXParserTest.openResource("/pq7545915.zip")) {
            GPXUtils.parseZip(is, parser, firstHooks);
        }
        parser.reset();

        final RecordingGPXParseHooks secondHooks = new RecordingGPXParseHooks();
        try (InputStream is = GPXParserTest.openResource("/pq_entities.zip")) {
            assertThat(GPXUtils.parseZip(is, parser, secondHooks)).isEqualTo(1);
        }
        assertThat(secondHooks.getGlobalItems()).isNotEmpty();
    }
}

