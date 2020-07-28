package cgeo.geocaching.filter;

import cgeo.geocaching.R;
import cgeo.geocaching.connector.ConnectorFactory;
import cgeo.geocaching.connector.IConnector;
import cgeo.geocaching.connector.capability.IVotingCapability;
import cgeo.geocaching.models.Geocache;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

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
    public boolean accepts(@NonNull final Geocache cache) {
        final IConnector connector = ConnectorFactory.getConnector(cache);
        return cache.isFound() && connector instanceof IVotingCapability && !((IVotingCapability) connector).isValidRating(cache.getMyVote()) && ((IVotingCapability) connector).supportsVoting(cache);
    }

}
