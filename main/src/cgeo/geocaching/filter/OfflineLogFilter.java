package cgeo.geocaching.filter;

import cgeo.geocaching.R;
import cgeo.geocaching.models.Geocache;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;

public class OfflineLogFilter extends AbstractFilter {

    public static final Creator<OfflineLogFilter> CREATOR = new Parcelable.Creator<OfflineLogFilter>() {

        @Override
        public OfflineLogFilter createFromParcel(final Parcel in) {
            return new OfflineLogFilter(in);
        }

        @Override
        public OfflineLogFilter[] newArray(final int size) {
            return new OfflineLogFilter[size];
        }
    };

    protected OfflineLogFilter() {
        super(R.string.caches_filter_offline_log);
    }

    protected OfflineLogFilter(final Parcel in) {
        super(in);
    }

    @Override
    public boolean accepts(@NonNull final Geocache cache) {
        return cache.isLogOffline();
    }
}
