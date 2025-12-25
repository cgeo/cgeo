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

package cgeo.geocaching.wherigo.openwig

import org.assertj.core.api.Java6Assertions.assertThat
import org.junit.Test

class EventTableTest {

    @Test
    public Unit testToString() {

        val et: EventTable = EventTable()
        et.rawset("Name", "myname")
        et.rawset("text", "sometext")
        et.rawset("bool", true)
        et.rawset("Int", 5)

        val zone: Zone = Zone()
        zone.rawset("Name", "myZone")
        et.rawset("zone", zone)

        //test that circulars are avoided
        et.rawset("same", et)

        val etString: String = et.toString()
        assertThat(etString).startsWith("[EventTable]myname")
        assertThat(etString).contains("text=sometext")
        assertThat(etString).contains("bool=true")
        assertThat(etString).contains("Int=5")
        assertThat(etString).contains("[Zone]myZone")
        assertThat(etString).contains("same=[EventTable]myname")
    }

}
