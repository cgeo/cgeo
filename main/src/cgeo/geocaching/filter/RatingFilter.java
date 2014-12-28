package cgeo.geocaching.filter;

import cgeo.geocaching.CgeoApplication;
import cgeo.geocaching.Geocache;
import cgeo.geocaching.R;
import cgeo.geocaching.gcvote.GCVote;

import java.util.Collections;
import java.util.List;

/**
 * Filter {@link Geocache}s if they have a locally stored {@link GCVote} rating. This filter will not do any network
 * request to find potentially missing local votes.
 *
 */
public class RatingFilter extends AbstractFilter implements IFilterFactory {

    protected RatingFilter() {
        super(CgeoApplication.getInstance().getString(R.string.caches_filter_rating));
    }

    @Override
    public boolean accepts(final Geocache cache) {
        return cache.getRating() > 0;
    }

    @Override
    public List<RatingFilter> getFilters() {
        return Collections.singletonList(this);
    }

}
