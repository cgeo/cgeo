package cgeo.geocaching.sorting;

import cgeo.geocaching.models.Geocache;

/**
 * Compares caches by date. Used only for event caches, if the cache list detects that a list contains only events.
 */
public class EventDateComparator extends DateComparator {

    public static final EventDateComparator INSTANCE = new EventDateComparator();

    @Override
    protected int sortSameDate(final Geocache left, final Geocache right) {
        return compare(left.getEventStartTimeInMinutes(), right.getEventStartTimeInMinutes());
    }

    /**
     * copy of {@link Integer#compare(int, int)}, as that is not available on lower API levels
     */
    private static int compare(final int left, final int right) {
        return Integer.compare(left, right);
    }

}
