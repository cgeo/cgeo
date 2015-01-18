package cgeo.geocaching.filter;

import cgeo.geocaching.CgeoApplication;
import cgeo.geocaching.Geocache;
import cgeo.geocaching.R;
import cgeo.geocaching.gcvote.GCVote;

import org.eclipse.jdt.annotation.NonNull;

import java.util.Collections;
import java.util.List;

/**
 * Filter {@link Geocache}s if they have a locally stored <b>own</b> {@link GCVote} rating. This filter will not do any
 * network request to find potentially missing local votes.
 *
 */
public class OwnRatingFilter extends AbstractFilter implements IFilterFactory {

    protected OwnRatingFilter() {
        super(CgeoApplication.getInstance().getString(R.string.caches_filter_own_rating));
    }

    @Override
    public boolean accepts(@NonNull final Geocache cache) {
        return cache.getMyVote() > 0;
    }

    @Override
    @NonNull
    public List<OwnRatingFilter> getFilters() {
        return Collections.singletonList(this);
    }

}
