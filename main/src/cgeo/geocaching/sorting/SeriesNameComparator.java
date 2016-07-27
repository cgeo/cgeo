package cgeo.geocaching.sorting;

/**
 * Comparator for automatically sorting a cache series by number. This is actually only a marker class, since the
 * sorting is pure name sorting.
 */
public class SeriesNameComparator extends NameComparator {

    public static final SeriesNameComparator INSTANCE = new SeriesNameComparator();

    @Override
    public boolean isAutoManaged() {
        return true;
    }
}
