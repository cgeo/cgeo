package cgeo.geocaching.sorting;

import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.storage.SqlBuilder;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * comparator which compares by a multitude of comparators
 */
public class MultiComparator implements CacheComparator {

    private final List<CacheComparator> comparators = new ArrayList<>();

    public MultiComparator add(final CacheComparator ... comparators) {
        for (CacheComparator cc: comparators) {
            if (cc != null) {
                this.comparators.add(cc);
            }
        }
        return this;
    }

    @Override
    public int compare(final Geocache lhs, final Geocache rhs) {
        for (CacheComparator cc : comparators) {
            final int c = cc.compare(rhs, lhs);
            if (c != 0) {
                return c;
            }
        }
        return 0;
    }

    @Override
    public String getSortableSection(@NonNull final Geocache cache) {
        return comparators.isEmpty() ? "" : comparators.get(0).getSortableSection(cache);
    }

    @Override
    public void sort(final List<Geocache> list) {
        Collections.sort(list, this);
    }

    @Override
    public void addSortToSql(final SqlBuilder sql, final boolean sortDesc) {
        for (CacheComparator cc : comparators) {
            cc.addSortToSql(sql, sortDesc);
        }
    }
}
