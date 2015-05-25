package cgeo.geocaching.filter;

import cgeo.geocaching.Geocache;
import cgeo.geocaching.R;

import org.eclipse.jdt.annotation.NonNull;

import android.os.Parcel;
import android.os.Parcelable;

class TrackablesFilter extends AbstractFilter {

    public TrackablesFilter() {
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
