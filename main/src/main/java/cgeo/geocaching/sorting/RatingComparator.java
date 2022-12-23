package cgeo.geocaching.sorting;

import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.storage.SqlBuilder;

import androidx.annotation.NonNull;

import java.util.Locale;

/**
 * sorts caches by gcvote.com rating
 */
class RatingComparator extends AbstractCacheComparator {

    /**
     * Average rating of GC50*** determined on June 26th, 2017
     */
    private static final float AVERAGE_RATING = 3.4f;

    private static final int AVERAGE_VOTES = 5;

    @Override
    protected int compareCaches(final Geocache cache1, final Geocache cache2) {
        return Float.compare(getWeightedArithmeticMean(cache2), getWeightedArithmeticMean(cache1));
    }

    /**
     * Add some artificial average ratings to weight caches with few ratings towards the average rating.
     */
    private static float getWeightedArithmeticMean(final Geocache cache) {
        final float rating = cache.getRating();
        final int votes = cache.getVotes();

        return (votes * rating + AVERAGE_VOTES * AVERAGE_RATING) / (votes + AVERAGE_VOTES);
    }

    @Override
    public String getSortableSection(@NonNull final Geocache cache) {
        return String.format(Locale.getDefault(), "%.2f", getWeightedArithmeticMean(cache));
    }

    @Override
    public void addSortToSql(final SqlBuilder sql, final boolean sortDesc) {
        sql.addOrder(sql.getMainTableId() + ".rating", !sortDesc);
    }
}
