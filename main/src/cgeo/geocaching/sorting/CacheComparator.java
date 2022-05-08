package cgeo.geocaching.sorting;

import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.storage.SqlBuilder;

import androidx.annotation.NonNull;

import java.util.Comparator;
import java.util.List;

public interface CacheComparator extends Comparator<Geocache> {

    String getSortableSection(@NonNull Geocache cache);

    /**
     * Sorts the given list of caches using this comparator. Respects implementations of {@link #beforeSort(List)} and{@link #afterSort(List)}
     */
    void sort(List<Geocache> list);

    default void addSortToSql(final SqlBuilder sql, final boolean sortDesc) {
        //do nothing by default
    }


}
