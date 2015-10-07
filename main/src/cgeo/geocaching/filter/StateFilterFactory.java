package cgeo.geocaching.filter;

import cgeo.geocaching.Geocache;
import cgeo.geocaching.LogEntry;
import cgeo.geocaching.R;

import org.eclipse.jdt.annotation.NonNull;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

class StateFilterFactory implements IFilterFactory {

    @Override
    @NonNull
    public List<? extends IFilter> getFilters() {
        final List<AbstractFilter> filters = new ArrayList<>(6);
        filters.add(new StateFoundFilter());
        filters.add(new StateNotFoundFilter());
        filters.add(new StateNeverFoundFilter());
        filters.add(new StateArchivedFilter());
        filters.add(new StateDisabledFilter());
        filters.add(new StatePremiumFilter());
        filters.add(new StateNonPremiumFilter());
        filters.add(new StateStoredFilter());
        filters.add(new StateNotStoredFilter());
        filters.add(new RatingFilter());
        filters.add(new TrackablesFilter());

        Collections.sort(filters, new Comparator<AbstractFilter>() {

            @Override
            public int compare(final AbstractFilter filter1, final AbstractFilter filter2) {
                return String.CASE_INSENSITIVE_ORDER.compare(filter1.getName(), filter2.getName());
            }
        });

        return filters;
    }

    static class StateFoundFilter extends AbstractFilter {

        public StateFoundFilter() {
            super(R.string.cache_status_found);
        }

        protected StateFoundFilter(final Parcel in) {
            super(in);
        }

        @Override
        public boolean accepts(@NonNull final Geocache cache) {
            return cache.isFound();
        }

        public static final Creator<StateFoundFilter> CREATOR
                = new Parcelable.Creator<StateFoundFilter>() {

            @Override
            public StateFoundFilter createFromParcel(final Parcel in) {
                return new StateFoundFilter(in);
            }

            @Override
            public StateFoundFilter[] newArray(final int size) {
                return new StateFoundFilter[size];
            }
        };
    }

    static class StateNotFoundFilter extends AbstractFilter {

        public StateNotFoundFilter() {
            super(R.string.cache_not_status_found);
        }

        protected StateNotFoundFilter(final Parcel in) {
            super(in);
        }

        @Override
        public boolean accepts(@NonNull final Geocache cache) {
            return !cache.isFound();
        }

        public static final Creator<StateNotFoundFilter> CREATOR
                = new Parcelable.Creator<StateNotFoundFilter>() {

            @Override
            public StateNotFoundFilter createFromParcel(final Parcel in) {
                return new StateNotFoundFilter(in);
            }

            @Override
            public StateNotFoundFilter[] newArray(final int size) {
                return new StateNotFoundFilter[size];
            }
        };
    }

    static class StateNeverFoundFilter extends AbstractFilter {

        public StateNeverFoundFilter() {
            super(R.string.cache_never_found);
        }

        protected StateNeverFoundFilter(final Parcel in) {
            super(in);
        }

        @Override
        public boolean accepts(@NonNull final Geocache cache) {
            if (cache.getFindsCount() > 0) {
                return false;
            }
            // find counts don't exist for every connector, so we also check the logs
            for (final LogEntry log : cache.getLogs()) {
                if (log.type.isFoundLog()) {
                    return false;
                }
            }
            return true;
        }

        public static final Creator<StateNeverFoundFilter> CREATOR = new Parcelable.Creator<StateNeverFoundFilter>() {

            @Override
            public StateNeverFoundFilter createFromParcel(final Parcel in) {
                return new StateNeverFoundFilter(in);
            }

            @Override
            public StateNeverFoundFilter[] newArray(final int size) {
                return new StateNeverFoundFilter[size];
            }
        };
    }

    static class StateArchivedFilter extends AbstractFilter {

        public StateArchivedFilter() {
            super(R.string.cache_status_archived);
        }

        protected StateArchivedFilter(final Parcel in) {
            super(in);
        }

        @Override
        public boolean accepts(@NonNull final Geocache cache) {
            return cache.isArchived();
        }

        public static final Creator<StateArchivedFilter> CREATOR
                = new Parcelable.Creator<StateArchivedFilter>() {

            @Override
            public StateArchivedFilter createFromParcel(final Parcel in) {
                return new StateArchivedFilter(in);
            }

            @Override
            public StateArchivedFilter[] newArray(final int size) {
                return new StateArchivedFilter[size];
            }
        };
    }

    static class StateDisabledFilter extends AbstractFilter {

        public StateDisabledFilter() {
            super(R.string.cache_status_disabled);
        }

        protected StateDisabledFilter(final Parcel in) {
            super(in);
        }

        @Override
        public boolean accepts(@NonNull final Geocache cache) {
            return cache.isDisabled();
        }

        public static final Creator<StateDisabledFilter> CREATOR
                = new Parcelable.Creator<StateDisabledFilter>() {

            @Override
            public StateDisabledFilter createFromParcel(final Parcel in) {
                return new StateDisabledFilter(in);
            }

            @Override
            public StateDisabledFilter[] newArray(final int size) {
                return new StateDisabledFilter[size];
            }
        };
    }

    static class StatePremiumFilter extends AbstractFilter {

        public StatePremiumFilter() {
            super(R.string.cache_status_premium);
        }

        protected StatePremiumFilter(final Parcel in) {
            super(in);
        }

        @Override
        public boolean accepts(@NonNull final Geocache cache) {
            return cache.isPremiumMembersOnly();
        }

        public static final Creator<StatePremiumFilter> CREATOR
                = new Parcelable.Creator<StatePremiumFilter>() {

            @Override
            public StatePremiumFilter createFromParcel(final Parcel in) {
                return new StatePremiumFilter(in);
            }

            @Override
            public StatePremiumFilter[] newArray(final int size) {
                return new StatePremiumFilter[size];
            }
        };
    }

    static class StateNonPremiumFilter extends AbstractFilter {

        public StateNonPremiumFilter() {
            super(R.string.cache_status_not_premium);
        }

        protected StateNonPremiumFilter(final Parcel in) {
            super(in);
        }

        @Override
        public boolean accepts(@NonNull final Geocache cache) {
            return !cache.isPremiumMembersOnly();
        }

        public static final Creator<StateNonPremiumFilter> CREATOR
                = new Parcelable.Creator<StateNonPremiumFilter>() {

            @Override
            public StateNonPremiumFilter createFromParcel(final Parcel in) {
                return new StateNonPremiumFilter(in);
            }

            @Override
            public StateNonPremiumFilter[] newArray(final int size) {
                return new StateNonPremiumFilter[size];
            }
        };
    }

    static class StateStoredFilter extends AbstractFilter {

        public StateStoredFilter() {
            super(R.string.cache_status_stored);
        }

        protected StateStoredFilter(final Parcel in) {
            super(in);
        }

        @Override
        public boolean accepts(@NonNull final Geocache cache) {
            return cache.isOffline();
        }

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
    }

    static class StateNotStoredFilter extends AbstractFilter {

        public StateNotStoredFilter() {
            super(R.string.cache_status_not_stored);
        }

        protected StateNotStoredFilter(final Parcel in) {
            super(in);
        }

        @Override
        public boolean accepts(@NonNull final Geocache cache) {
            return !cache.isOffline();
        }

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
    }

}
