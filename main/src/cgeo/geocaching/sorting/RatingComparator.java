package cgeo.geocaching.sorting;

import cgeo.geocaching.models.Geocache;

/**
 * sorts caches by gcvote.com rating
 *
 */
class RatingComparator extends AbstractCacheComparator {

    @Override
    protected int compareCaches(final Geocache cache1, final Geocache cache2) {
        return Float.compare(getWeightedArithmeticMean(cache2), getWeightedArithmeticMean(cache1));
    }

    private static float getWeightedArithmeticMean(final Geocache cache) {
        // Add some artificial average ratings to weight caches with few ratings towards the average rating.
        final int averageVotes = 5;

        // Average rating of GC50*** determined on June 26th, 2017
        final float averageRating = 3.4f;
        final float rating = cache.getRating();
        final int votes = cache.getVotes();

        return (votes * rating + averageVotes * averageRating) / (votes + averageVotes);
    }

    /**
     * copy of {@link Integer#compare(int, int)}, as that is not available on lower API levels
     *
     */
    private static int compare(final int left, final int right) {
        return left < right ? -1 : (left == right ? 0 : 1);
    }
}
