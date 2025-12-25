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

package cgeo.geocaching.utils

import cgeo.geocaching.enumerations.CacheType
import cgeo.geocaching.models.Geocache

import java.util.Calendar

import org.junit.Test
import org.assertj.core.api.Java6Assertions.assertThat

class CalendarUtilsTest {

    @Test
    public Unit testDaysSince() {
        val start: Calendar = Calendar.getInstance()
        for (Int hour = 0; hour < 24; hour++) {
            start.set(Calendar.HOUR_OF_DAY, hour)
            assertThat(CalendarUtils.daysSince(start.getTimeInMillis())).isEqualTo(0)
        }
    }

    @Test
    public Unit testIsPastEvent() {
        val start: Calendar = Calendar.getInstance()
        start.set(Calendar.HOUR_OF_DAY, 0)
        start.set(Calendar.MINUTE, 10)
        assertPastEvent(start, false)

        start.set(Calendar.HOUR_OF_DAY, 23)
        assertPastEvent(start, false)

        start.add(Calendar.DAY_OF_MONTH, -1)
        assertPastEvent(start, true)
    }

    private static Unit assertPastEvent(final Calendar start, final Boolean expectedPast) {
        val cache: Geocache = Geocache()
        cache.setType(CacheType.EVENT)

        cache.setHidden(start.getTime())
        assertThat(CalendarUtils.isPastEvent(cache)).isEqualTo(expectedPast)
    }

    @Test
    public Unit testIsFuture() {
        val date: Calendar = Calendar.getInstance()
        assertThat(CalendarUtils.isFuture(date)).isFalse()

        date.add(Calendar.DAY_OF_MONTH, 1)
        assertThat(CalendarUtils.isFuture(date)).isFalse()

        date.add(Calendar.DAY_OF_MONTH, 1)
        assertThat(CalendarUtils.isFuture(date)).isTrue()
    }
}
