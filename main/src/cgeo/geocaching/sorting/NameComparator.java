package cgeo.geocaching.sorting;

import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.storage.SqlBuilder;
import cgeo.geocaching.utils.TextUtils;

import androidx.annotation.NonNull;

import org.apache.commons.lang3.StringUtils;

/**
 * sorts caches by name
 */
public class NameComparator extends AbstractCacheComparator {

    public static final NameComparator INSTANCE = new NameComparator();

    @Override
    protected boolean canCompare(final Geocache cache) {
        return StringUtils.isNotBlank(cache.getName());
    }

    @Override
    protected int compareCaches(final Geocache cache1, final Geocache cache2) {
        return TextUtils.COLLATOR.compare(cache1.getNameForSorting(), cache2.getNameForSorting());
    }

    @Override
    public String getSortableSection(@NonNull final Geocache cache) {
        return StringUtils.upperCase(StringUtils.substring(cache.getNameForSorting(), 0, 2));
    }

    @Override
    public void addSortToSql(final SqlBuilder sql, final boolean sortDesc) {
        sql.addOrder("LOWER(" + sql.getMainTableId() + ".name)", sortDesc);
    }
}
