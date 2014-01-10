package cgeo.geocaching.sorting;

import cgeo.geocaching.Geocache;

/**
 * sorts caches by the users own voting (if available at all)
 */
public class VoteComparator extends AbstractCacheComparator {

    @Override
    protected int compareCaches(Geocache cache1, Geocache cache2) {
        // if there is no vote available, put that cache at the end of the list
        return Float.compare(cache2.getMyVote(), cache1.getMyVote());
    }
}
