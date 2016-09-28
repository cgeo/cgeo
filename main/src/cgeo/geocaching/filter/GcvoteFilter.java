package cgeo.geocaching.filter;

import cgeo.geocaching.R;
import cgeo.geocaching.gcvote.GCVote;
import cgeo.geocaching.models.Geocache;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Filter to display found caches eligible for voting on GCVote.
 */
class GcvoteFilter extends AbstractFilter {

    public static final Creator<GcvoteFilter> CREATOR = new Parcelable.Creator<GcvoteFilter>() {

        @Override
        public GcvoteFilter createFromParcel(final Parcel in) {
            return new GcvoteFilter(in);
        }

        @Override
        public GcvoteFilter[] newArray(final int size) {
            return new GcvoteFilter[size];
        }
    };

    GcvoteFilter() {
        super(R.string.caches_filter_missing_gcvote);
    }

    GcvoteFilter(final Parcel in) {
        super(in);
    }

    @Override
    public boolean accepts(final Geocache cache) {
        return cache.isFound() && !GCVote.isValidRating(cache.getMyVote()) && GCVote.isVotingPossible(cache);
    }

}
