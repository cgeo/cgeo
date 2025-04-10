package cgeo.geocaching.sorting;

import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.storage.SqlBuilder;

import androidx.annotation.NonNull;

import java.util.Comparator;
import java.util.List;

public interface CacheComparator extends Comparator<Geocache> {

    String getSortableSection(@NonNull Geocache cache);

    /** Sorts the given list of caches using this comparator. */
    void sort(List<Geocache> list);

    /**
     * Can optionally be overridden to perform preparation (e.g. caching of values) before sort of a list via {@link #sort(List)}
     */
    default void beforeSort(final List<Geocache> list) {
        //by default, do nothing
    }

    /**
     * Can optionally be overridden to perform cleanup (e.g. deleting cached values) before sort of a list via {@link #sort(List)}
     */
    default void afterSort(final List<Geocache> list) {
        //by default, do nothing
    }

    default void addSortToSql(final SqlBuilder sql, final boolean sortDesc) {
        //do nothing by default
    }
}
