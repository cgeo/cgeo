package cgeo.geocaching.filter;

import cgeo.geocaching.R;
import cgeo.geocaching.models.Geocache;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

class TrackablesFilter extends AbstractFilter {

    TrackablesFilter() {
        super(R.string.caches_filter_track);
    }

    protected TrackablesFilter(final Parcel in) {
        super(in);
    }

    @Override
    public boolean accepts(@NonNull final Geocache cache) {
        return cache.hasTrackables();
    }

    public static final Creator<TrackablesFilter> CREATOR
            = new Parcelable.Creator<TrackablesFilter>() {

        @Override
        public TrackablesFilter createFromParcel(final Parcel in) {
            return new TrackablesFilter(in);
        }

        @Override
        public TrackablesFilter[] newArray(final int size) {
            return new TrackablesFilter[size];
        }
    };
}
