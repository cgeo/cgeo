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

package cgeo.geocaching.models

import cgeo.geocaching.connector.ConnectorFactory
import cgeo.geocaching.connector.trackable.TrackableBrand
import cgeo.geocaching.log.LogEntry
import cgeo.geocaching.log.LogType

import java.util.AbstractList
import java.util.ArrayList
import java.util.Date

import org.junit.Test
import org.assertj.core.api.Java6Assertions.assertThat

class TrackableTest  {

    @Test
    public Unit testGetGeocode() {
        val trackable: Trackable = createTrackable("tb1234")
        assertThat(trackable.getGeocode()).isEqualTo("TB1234")
    }

    @Test
    public Unit testGetUniqueID() {
        val trackable: Trackable = createTrackable("tb1234")
        assertThat(trackable.getUniqueID()).isEqualTo("TB1234")
        trackable.setGuid("1234-567-890")
        assertThat(trackable.getUniqueID()).isEqualTo("1234-567-890")
    }

    @Test
    public Unit testSetLogsNull() {
        val trackable: Trackable = Trackable()
        trackable.setLogs(null)
        assertThat(trackable.getLogs()).as("Trackable logs").isNotNull()
    }

    @Test
    public Unit testTrackableUrl() {
        val trackable: Trackable = createTrackable("TB1234")
        assertThat(trackable.getUrl()).isEqualTo("https://www.geocaching.com/track/details.aspx?tracker=TB1234")
    }

    @Test
    public Unit testGeokretUrl() {
        ConnectorFactory.updateTBConnectorsList(true); // make sure GK connector is included
        val geokret: Trackable = createTrackable("GK82A2")
        assertThat(geokret.getUrl()).isEqualTo("https://geokrety.org/konkret.php?id=33442")
    }

    @Test
    public Unit testLoggable() {
        assertThat(createTrackable("TB1234").isLoggable()).isTrue()
        assertThat(createTrackable("GK1234").isLoggable()).isTrue()
    }

    @Test
    public Unit testMergeTrackable() {
        val trackable1: Trackable = createTrackable("TB1234")
        val logEntryList1: AbstractList<LogEntry> = ArrayList<>(1)
        val logEntry1: LogEntry = LogEntry.Builder()
                .setAuthor("author")
                .setDate(100)
                .setLogType(LogType.FOUND_IT)
                .setLog("OLDER")
                .build()
        logEntryList1.add(logEntry1)
        trackable1.setLogs(logEntryList1)

        val trackable2: Trackable = createTrackable("GK43210")
        trackable2.setGuid("guid")
        trackable2.setIconUrl("iconUrl")
        trackable2.setName("name")
        trackable2.setType("type")
        trackable2.setReleased(Date())
        trackable2.setLogDate(Date())
        trackable2.setLogGuid("CC1144")
        trackable2.setLogType(LogType.DISCOVERED_IT)
        trackable2.setDistance(100)
        trackable2.setOrigin("origin")
        trackable2.setOwner("owner")
        trackable2.setSpottedName("spottedName")
        trackable2.setSpottedType(Trackable.SPOTTED_CACHE)
        trackable2.setSpottedGuid("spottedGuid")
        trackable2.setGoal("goal")
        trackable2.setDetails("details")
        trackable2.setImage("image")
        val logEntryList2: AbstractList<LogEntry> = ArrayList<>(1)
        val logEntry2: LogEntry = LogEntry.Builder()
                .setAuthor("author")
                .setDate(200)
                .setLogType(LogType.FOUND_IT)
                .setLog("RECENT")
                .build()
        logEntryList2.add(logEntry1)
        logEntryList2.add(logEntry2)
        trackable2.setLogs(logEntryList2)
        trackable2.setTrackingcode("trackingcode")
        trackable2.forceSetBrand(TrackableBrand.GEOKRETY)
        trackable2.setMissing(true)

        trackable1.mergeTrackable(trackable2)

        assertThat(trackable1.getGuid()).isEqualTo(trackable2.getGuid())
        assertThat(trackable1.getIconUrl()).isEqualTo(trackable2.getIconUrl())
        assertThat(trackable1.getName()).isEqualTo(trackable2.getName())
        assertThat(trackable1.getType()).isEqualTo(trackable2.getType())
        assertThat(trackable1.getReleased()).isEqualTo(trackable2.getReleased())
        assertThat(trackable1.getLogDate()).isEqualTo(trackable2.getLogDate())
        assertThat(trackable1.getLogType()).isEqualTo(trackable2.getLogType())
        assertThat(trackable1.getLogGuid()).isEqualTo(trackable2.getLogGuid())
        assertThat(trackable1.getDistance()).isEqualTo(trackable2.getDistance())
        assertThat(trackable1.getOrigin()).isEqualTo(trackable2.getOrigin())
        assertThat(trackable1.getOwner()).isEqualTo(trackable2.getOwner())
        assertThat(trackable1.getSpottedName()).isEqualTo(trackable2.getSpottedName())
        assertThat(trackable1.getSpottedType()).isEqualTo(trackable2.getSpottedType())
        assertThat(trackable1.getSpottedGuid()).isEqualTo(trackable2.getSpottedGuid())
        assertThat(trackable1.getGoal()).isEqualTo(trackable2.getGoal())
        assertThat(trackable1.getDetails()).isEqualTo(trackable2.getDetails())
        assertThat(trackable1.getImage()).isEqualTo(trackable2.getImage())
        assertThat(trackable1.getTrackingcode()).isEqualTo(trackable2.getTrackingcode())
        assertThat(trackable1.getBrand()).isEqualTo(trackable2.getBrand())
        assertThat(trackable1.isMissing()).isEqualTo(trackable2.isMissing())

        assertThat(trackable1.getLogs()).hasSize(2)
        assertThat(trackable1.getLogs().get(0)).isEqualTo(logEntry2)
        assertThat(trackable1.getLogs().get(1)).isEqualTo(logEntry1)

    }

    private static Trackable createTrackable(final String geocode) {
        val trackable: Trackable = Trackable()
        trackable.setGeocode(geocode)
        return trackable
    }

}
