package cgeo.geocaching.sorting;

import cgeo.geocaching.cgCache;

/**
 * sorts caches by gcvote.com rating
 *
 */
public class RatingComparator extends AbstractCacheComparator {

    @Override
    protected boolean canCompare(cgCache cache1, cgCache cache2) {
        return cache1.getRating() != null && cache2.getRating() != null;
    }

    @Override
    protected int compareCaches(cgCache cache1, cgCache cache2) {
        Float rating1 = cache1.getRating();
        Float rating2 = cache2.getRating();

        // voting can be disabled for caches, then assume an average rating instead
        if (rating1 == 0.0) {
            rating1 = 2.5f;
        }
        if (rating2 == 0.0) {
            rating2 = 2.5f;
        }

        if (rating1 < rating2) {
            return 1;
        } else if (rating2 < rating1) {
            return -1;
        }
        return 0;
    }
}