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
		float vote1 = 0;
		if (cache1.myVote != null) {
			vote1 = cache1.myVote;
		}

		float vote2 = 0;
		if (cache2.myVote != null) {
			vote2 = cache2.myVote;
		}

		// compare
		if (vote1 < vote2) {
			return 1;
		} else if (vote2 < vote1) {
			return -1;
		}
		return 0;
	}
}
