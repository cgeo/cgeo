package cgeo.geocaching.filter;

import cgeo.geocaching.Geocache;
import cgeo.geocaching.R;
import cgeo.geocaching.gcvote.GCVote;

import org.eclipse.jdt.annotation.NonNull;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Filter {@link Geocache}s if they have a locally stored {@link GCVote} rating. This filter will not do any network
 * request to find potentially missing local votes.
 *
 */
public class RatingFilter extends AbstractFilter {

    protected RatingFilter() {
        super(R.string.caches_filter_rating);
    }

    protected RatingFilter(final Parcel in) {
        super(in);
    }

    @Override
    public boolean accepts(@NonNull final Geocache cache) {
        return cache.getRating() > 0;
    }

    public static final Creator<RatingFilter> CREATOR
            = new Parcelable.Creator<RatingFilter>() {

        @Override
        public RatingFilter createFromParcel(final Parcel in) {
            return new RatingFilter(in);
        }

        @Override
        public RatingFilter[] newArray(final int size) {
            return new RatingFilter[size];
        }
    };
}
