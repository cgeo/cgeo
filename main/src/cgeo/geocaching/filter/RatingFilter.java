package cgeo.geocaching.filter;

import cgeo.geocaching.Geocache;
import cgeo.geocaching.R;
import cgeo.geocaching.gcvote.GCVote;

import org.eclipse.jdt.annotation.NonNull;

/**
 * Filter {@link Geocache}s if they have a locally stored {@link GCVote} rating. This filter will not do any network
 * request to find potentially missing local votes.
 *
 */
public class RatingFilter extends AbstractFilter {

    protected RatingFilter() {
        super(R.string.caches_filter_rating);
    }

    @Override
    public boolean accepts(@NonNull final Geocache cache) {
        return cache.getRating() > 0;
    }

}
