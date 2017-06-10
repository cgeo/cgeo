package cgeo.geocaching.sorting;

import cgeo.geocaching.models.Geocache;

/**
 * sorts caches by gcvote.com rating
 *
 */
class RatingComparator extends AbstractCacheComparator {

    @Override
    protected int compareCaches(final Geocache cache1, final Geocache cache2) {
        final float rating1 = cache1.getRating();
        final float rating2 = cache2.getRating();

        // Voting can be disabled for caches, then assume an average rating instead
        final int ratingComparison = Float.compare(rating2 != 0.0 ? rating2 : 2.5f, rating1 != 0.0 ? rating1 : 2.5f);
        if (ratingComparison != 0) {
            return ratingComparison;
        }

        return compare(cache2.getVotes(), cache1.getVotes());
    }

    /**
     * copy of {@link Integer#compare(int, int)}, as that is not available on lower API levels
     *
     */
    private static int compare(final int left, final int right) {
        return left < right ? -1 : (left == right ? 0 : 1);
    }
}
