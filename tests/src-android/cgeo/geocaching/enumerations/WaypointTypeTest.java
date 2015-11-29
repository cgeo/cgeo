package cgeo.geocaching.enumerations;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class WaypointTypeTest {

    @Test
    public void testFindById() {
        assertThat(WaypointType.findById("random garbage")).isEqualTo(WaypointType.WAYPOINT);
    }

    @Test
    public void testConvertWaypointSym2Type() {
        assertThat(WaypointType.fromGPXString("unknown sym")).isEqualTo(WaypointType.WAYPOINT);

        assertThat(WaypointType.fromGPXString("Parking area")).isEqualTo(WaypointType.PARKING);
        assertThat(WaypointType.fromGPXString("Stages of a multicache")).isEqualTo(WaypointType.STAGE);
        assertThat(WaypointType.fromGPXString("Question to answer")).isEqualTo(WaypointType.PUZZLE);
        assertThat(WaypointType.fromGPXString("Trailhead")).isEqualTo(WaypointType.TRAILHEAD);
        assertThat(WaypointType.fromGPXString("Final location")).isEqualTo(WaypointType.FINAL);
        assertThat(WaypointType.fromGPXString("Reference point")).isEqualTo(WaypointType.WAYPOINT);

        assertThat(WaypointType.fromGPXString(WaypointType.PARKING.getL10n())).isEqualTo(WaypointType.PARKING);
        // new names of multi and mystery stages
        assertThat(WaypointType.fromGPXString("Physical Stage")).isEqualTo(WaypointType.STAGE);
        assertThat(WaypointType.fromGPXString("Virtual Stage")).isEqualTo(WaypointType.PUZZLE);
    }

}
