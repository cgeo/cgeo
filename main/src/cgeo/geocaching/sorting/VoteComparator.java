package cgeo.geocaching.sorting;

import cgeo.geocaching.cgCache;

/**
 * sorts caches by the users own voting (if available at all)
 *
 * @author bananeweizen
 *
 */
public class VoteComparator extends AbstractCacheComparator {

    @Override
    protected boolean canCompare(cgCache cache1, cgCache cache2) {
        return true;
    }

    @Override
    protected int compareCaches(cgCache cache1, cgCache cache2) {
        // if there is no vote available, put that cache at the end of the list
        return Float.compare(cache2.getMyVote(), cache1.getMyVote());
    }
}
