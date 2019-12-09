package cgeo.geocaching.filter;

import cgeo.geocaching.R;
import cgeo.geocaching.models.Geocache;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;

class StoredFilterFactory implements IFilterFactory {

    static class StateNotStoredFilter extends AbstractFilter {

        public static final Creator<StateNotStoredFilter> CREATOR
                = new Parcelable.Creator<StateNotStoredFilter>() {

            @Override
            public StateNotStoredFilter createFromParcel(final Parcel in) {
                return new StateNotStoredFilter(in);
            }

            @Override
            public StateNotStoredFilter[] newArray(final int size) {
                return new StateNotStoredFilter[size];
            }
        };

        StateNotStoredFilter() {
            super(R.string.cache_status_not_stored);
        }

        protected StateNotStoredFilter(final Parcel in) {
            super(in);
        }

        @Override
        public boolean accepts(@NonNull final Geocache cache) {
            return !cache.isOffline();
        }
    }

    static class StateStoredFilter extends AbstractFilter {

        public static final Creator<StateStoredFilter> CREATOR
                = new Parcelable.Creator<StateStoredFilter>() {

            @Override
            public StateStoredFilter createFromParcel(final Parcel in) {
                return new StateStoredFilter(in);
            }

            @Override
            public StateStoredFilter[] newArray(final int size) {
                return new StateStoredFilter[size];
            }
        };

        StateStoredFilter() {
            super(R.string.cache_status_stored);
        }

        protected StateStoredFilter(final Parcel in) {
            super(in);
        }

        @Override
        public boolean accepts(@NonNull final Geocache cache) {
            return cache.isOffline();
        }
    }

    static class StateStoredWithinLastXFilter extends AbstractFilter {
        protected long timeCompare = 0;
        protected boolean within = true;

        public static final Creator<StateStoredWithinLastXFilter> CREATOR
                = new Parcelable.Creator<StateStoredWithinLastXFilter>() {

            @Override
            public StateStoredWithinLastXFilter createFromParcel(final Parcel in) {
                return new StateStoredWithinLastXFilter(in);
            }

            @Override
            public StateStoredWithinLastXFilter[] newArray(final int size) {
                return new StateStoredWithinLastXFilter[size];
            }
        };

        StateStoredWithinLastXFilter(final int resId, final boolean within, final long timeCompare) {
            super(resId);
            this.timeCompare = timeCompare;
            this.within = within;
        }

        protected StateStoredWithinLastXFilter(final Parcel in) {
            super(in);
            timeCompare = in.readLong();
            within = in.readInt() != 0;
        }

        @Override
        public void writeToParcel(final Parcel dest, final int flags) {
            super.writeToParcel(dest, flags);
            dest.writeLong(timeCompare);
            dest.writeInt(within ? 1 : 0);
        }

        @Override
        public boolean accepts(@NonNull final Geocache cache) {
            if (within) {
                return cache.isOffline() && cache.getUpdated() >= timeCompare;
            }
            return cache.isOffline() && cache.getUpdated() <= timeCompare;
        }
    }

    static class StateStoredWithinLastEightHoursFilter extends StateStoredWithinLastXFilter {
        StateStoredWithinLastEightHoursFilter() {
            super(R.string.cache_status_stored_lasteighthours, true, System.currentTimeMillis() - 8 * 60 * 60 * 1000);
        }
    }

    static class StateStoredWithinLastTwentyFourHoursFilter extends StateStoredWithinLastXFilter {
        StateStoredWithinLastTwentyFourHoursFilter() {
            super(R.string.cache_status_stored_lasttwentyfourhours, true, System.currentTimeMillis() - 24 * 60 * 60 * 1000);
        }
    }

    static class StateStoredWithinLastSevenDaysFilter extends StateStoredWithinLastXFilter {
        StateStoredWithinLastSevenDaysFilter() {
            super(R.string.cache_status_stored_lastsevendays, true, System.currentTimeMillis() - 7 * 24 * 60 * 60 * 1000);
        }
    }

    static class StateStoredWithinLastThirtyDaysFilter extends StateStoredWithinLastXFilter {
        StateStoredWithinLastThirtyDaysFilter() {
            super(R.string.cache_status_stored_lastthirtydays, true, System.currentTimeMillis() - 30 * 24 * 60 * 60 * 1000);
        }
    }

    static class StateStoredBeforeLastEightHoursFilter extends StateStoredWithinLastXFilter {
        StateStoredBeforeLastEightHoursFilter() {
            super(R.string.cache_status_stored_morethaneighthours, false, System.currentTimeMillis() - 8 * 60 * 60 * 1000);
        }
    }

    static class StateStoredBeforeLastTwentyFourHoursFilter extends StateStoredWithinLastXFilter {
        StateStoredBeforeLastTwentyFourHoursFilter() {
            super(R.string.cache_status_stored_morethantwentyfourhours, false, System.currentTimeMillis() - 24 * 60 * 60 * 1000);
        }
    }

    static class StateStoredBeforeLastSevenDaysFilter extends StateStoredWithinLastXFilter {
        StateStoredBeforeLastSevenDaysFilter() {
            super(R.string.cache_status_stored_morethansevendays, false, System.currentTimeMillis() - 7 * 24 * 60 * 60 * 1000);
        }
    }

    static class StateStoredBeforeLastThirtyDaysFilter extends StateStoredWithinLastXFilter {
        StateStoredBeforeLastThirtyDaysFilter() {
            super(R.string.cache_status_stored_morethanthirtydays, false, System.currentTimeMillis() - 30 * 24 * 60 * 60 * 1000);
        }
    }

    @Override
    @NonNull
    public List<IFilter> getFilters() {
        final List<IFilter> filters = new ArrayList<>(10);
        filters.add(new StateNotStoredFilter());
        filters.add(new StateStoredFilter());
        filters.add(new StateStoredWithinLastEightHoursFilter());
        filters.add(new StateStoredWithinLastTwentyFourHoursFilter());
        filters.add(new StateStoredWithinLastSevenDaysFilter());
        filters.add(new StateStoredWithinLastThirtyDaysFilter());
        filters.add(new StateStoredBeforeLastEightHoursFilter());
        filters.add(new StateStoredBeforeLastTwentyFourHoursFilter());
        filters.add(new StateStoredBeforeLastSevenDaysFilter());
        filters.add(new StateStoredBeforeLastThirtyDaysFilter());
        return filters;
    }

}
