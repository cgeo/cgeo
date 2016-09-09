package cgeo.geocaching.filter;

import cgeo.geocaching.R;
import cgeo.geocaching.gcvote.GCVote;
import cgeo.geocaching.models.Geocache;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;

import java.util.Collections;
import java.util.List;

/**
 * Filter {@link Geocache}s if they have a locally stored <b>own</b> {@link GCVote} rating. This filter will not do any
 * network request to find potentially missing local votes.
 *
 */
public class OwnRatingFilter extends AbstractFilter implements IFilterFactory {

    public static final Creator<OwnRatingFilter> CREATOR = new Parcelable.Creator<OwnRatingFilter>() {

        @Override
        public OwnRatingFilter createFromParcel(final Parcel in) {
            return new OwnRatingFilter(in);
        }

        @Override
        public OwnRatingFilter[] newArray(final int size) {
            return new OwnRatingFilter[size];
        }
    };

    protected OwnRatingFilter() {
        super(R.string.caches_filter_own_rating);
    }

    protected OwnRatingFilter(final Parcel in) {
        super(in);
    }

    @Override
    public boolean accepts(@NonNull final Geocache cache) {
        return cache.getMyVote() > 0;
    }

    @Override
    @NonNull
    public List<IFilter> getFilters() {
        return Collections.<IFilter> singletonList(this);
    }
}
