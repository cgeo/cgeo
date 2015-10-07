package cgeo.geocaching.sorting;

import cgeo.geocaching.Geocache;

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
        return Float.compare(rating2 != 0.0 ? rating2 : 2.5f, rating1 != 0.0 ? rating1 : 2.5f);
    }
}