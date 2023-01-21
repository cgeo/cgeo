package cgeo.geocaching.sorting;

import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.storage.SqlBuilder;

import androidx.annotation.NonNull;

import java.util.Locale;

/**
 * sorts caches by the users own voting (if available at all)
 */
class VoteComparator extends AbstractCacheComparator {

    @Override
    protected int compareCaches(final Geocache cache1, final Geocache cache2) {
        // if there is no vote available, put that cache at the end of the list
        return Float.compare(cache2.getMyVote(), cache1.getMyVote());
    }

    @Override
    public String getSortableSection(@NonNull final Geocache cache) {
        return String.format(Locale.getDefault(), "%.2f", cache.getMyVote());
    }

    @Override
    public void addSortToSql(final SqlBuilder sql, final boolean sortDesc) {
        sql.addOrder(sql.getMainTableId() + ".myvote", !sortDesc);
    }
}
