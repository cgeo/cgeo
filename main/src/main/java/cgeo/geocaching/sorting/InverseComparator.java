package cgeo.geocaching.sorting;

import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.storage.SqlBuilder;

import androidx.annotation.NonNull;

import java.util.Collections;
import java.util.List;

/**
 * comparator which inverses the sort order of the given other comparator
 */
public class InverseComparator implements CacheComparator {

    private final CacheComparator originalComparator;

    public InverseComparator(final CacheComparator comparator) {
        this.originalComparator = comparator;
    }

    @Override
    public int compare(final Geocache lhs, final Geocache rhs) {
        return originalComparator.compare(rhs, lhs);
    }

    @Override
    public String getSortableSection(@NonNull final Geocache cache) {
        return originalComparator.getSortableSection(cache);
    }

    @Override
    public void sort(final List<Geocache> list) {
        Collections.sort(list, this);
    }

    @Override
    public void addSortToSql(final SqlBuilder sql, final boolean sortDesc) {
        originalComparator.addSortToSql(sql, !sortDesc);
    }
}
