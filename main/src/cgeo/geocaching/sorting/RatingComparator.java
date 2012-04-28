package cgeo.geocaching.sorting;

import cgeo.geocaching.cgCache;

/**
 * sorts caches by gcvote.com rating
 *
 */
public class RatingComparator extends AbstractCacheComparator {

    @Override
    protected boolean canCompare(final cgCache cache1, final cgCache cache2) {
        return true;
    }

    @Override
    protected int compareCaches(final cgCache cache1, final cgCache cache2) {
        final float rating1 = cache1.getRating();
        final float rating2 = cache2.getRating();
        // Voting can be disabled for caches, then assume an average rating instead
        return Float.compare(rating2 != 0.0 ? rating2 : 2.5f, rating1 != 0.0 ? rating1 : 2.5f);
    }
}