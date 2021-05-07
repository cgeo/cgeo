package cgeo.geocaching.sorting;

import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.storage.SqlBuilder;

import androidx.annotation.NonNull;

import java.util.Comparator;
import java.util.List;

public interface CacheComparator extends Comparator<Geocache> {

    /**
     * returns {@code true} if this is a comparator that is used by the app automatically and was not selected by the
     * user.
     */
    boolean isAutoManaged();

    String getSortableSection(@NonNull Geocache cache);

    /** Sorts the given list of caches using this comparator. Respects implementations of {@link #beforeSort(List)} and{@link #afterSort(List)} */
    void sort(List<Geocache> list, boolean inverse);

    default void addSortToSql(final SqlBuilder sql, final boolean sortDesc) {
        //do nothing by default
    }



}
