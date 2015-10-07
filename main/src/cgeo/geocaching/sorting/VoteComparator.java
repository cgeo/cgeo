package cgeo.geocaching.sorting;

import cgeo.geocaching.Geocache;

/**
 * sorts caches by the users own voting (if available at all)
 */
class VoteComparator extends AbstractCacheComparator {

    @Override
    protected int compareCaches(final Geocache cache1, final Geocache cache2) {
        // if there is no vote available, put that cache at the end of the list
        return Float.compare(cache2.getMyVote(), cache1.getMyVote());
    }
}
