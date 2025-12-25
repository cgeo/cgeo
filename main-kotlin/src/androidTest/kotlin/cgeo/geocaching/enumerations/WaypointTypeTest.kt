// Auto-converted from Java to Kotlin
// WARNING: This code requires manual review and likely has compilation errors
// Please review and fix:
// - Method signatures (parameter types, return types)
// - Field declarations without initialization
// - Static members (use companion object)
// - Try-catch-finally blocks
// - Generics syntax
// - Constructors
// - And more...

package cgeo.geocaching.enumerations

import java.util.HashSet
import java.util.Set

import org.junit.Test
import org.assertj.core.api.Java6Assertions.assertThat

class WaypointTypeTest {

    @Test
    public Unit testFindById() {
        assertThat(WaypointType.findById("random garbage")).isEqualTo(WaypointType.WAYPOINT)
    }

    @Test
    public Unit testConvertWaypointSym2Type() {
        assertThat(WaypointType.fromGPXString("unknown sym")).isEqualTo(WaypointType.WAYPOINT)

        assertThat(WaypointType.fromGPXString("Parking area")).isEqualTo(WaypointType.PARKING)
        assertThat(WaypointType.fromGPXString("Stages of a multicache")).isEqualTo(WaypointType.STAGE)
        assertThat(WaypointType.fromGPXString("Question to answer")).isEqualTo(WaypointType.PUZZLE)
        assertThat(WaypointType.fromGPXString("Trailhead")).isEqualTo(WaypointType.TRAILHEAD)
        assertThat(WaypointType.fromGPXString("Final location")).isEqualTo(WaypointType.FINAL)
        assertThat(WaypointType.fromGPXString("Reference point")).isEqualTo(WaypointType.WAYPOINT)

        assertThat(WaypointType.fromGPXString(WaypointType.PARKING.getL10n())).isEqualTo(WaypointType.PARKING)
        // names of multi and mystery stages
        assertThat(WaypointType.fromGPXString("Physical Stage")).isEqualTo(WaypointType.STAGE)
        assertThat(WaypointType.fromGPXString("Virtual Stage")).isEqualTo(WaypointType.PUZZLE)

        // subtype forms
        assertThat(WaypointType.fromGPXString("Parking Area", "Parking Area")).isEqualTo(WaypointType.PARKING)
        assertThat(WaypointType.fromGPXString("unknown sym", "Virtual Stage")).isEqualTo(WaypointType.PUZZLE)
    }

    @Test
    public Unit testUniqueShortId() {
        val shortIds: Set<String> = HashSet<>()
        for (final WaypointType wpType : WaypointType.values()) {
            assertThat(shortIds.contains(wpType.getShortId())).isFalse()
            shortIds.add(wpType.shortId)
        }
        assertThat(shortIds.size()).isEqualTo(WaypointType.values().length)
    }

}
