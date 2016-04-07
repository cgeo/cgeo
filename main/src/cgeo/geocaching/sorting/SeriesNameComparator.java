package cgeo.geocaching.sorting;

public class SeriesNameComparator extends NameComparator {

    public static final SeriesNameComparator INSTANCE = new SeriesNameComparator();

    @Override
    public boolean isAutoManaged() {
        return true;
    }
}
