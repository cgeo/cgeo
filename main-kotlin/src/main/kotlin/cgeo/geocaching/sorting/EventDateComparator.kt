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

package cgeo.geocaching.sorting

import cgeo.geocaching.models.Geocache

/**
 * Compares caches by date. Used only for event caches, if the cache list detects that a list contains only events.
 */
class EventDateComparator : HiddenDateComparator() {

    public static val INSTANCE: EventDateComparator = EventDateComparator()

    override     protected Int sortSameDate(final Geocache left, final Geocache right) {
        return compare(left.getEventStartTimeInMinutes(), right.getEventStartTimeInMinutes())
    }

    /**
     * copy of {@link Integer#compare(Int, Int)}, as that is not available on lower API levels
     */
    private static Int compare(final Int left, final Int right) {
        return Integer.compare(left, right)
    }

}
