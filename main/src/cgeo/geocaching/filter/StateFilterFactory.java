package cgeo.geocaching.filter;

import cgeo.geocaching.Geocache;
import cgeo.geocaching.R;

import org.eclipse.jdt.annotation.NonNull;

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
        filters.add(new StateArchivedFilter());
        filters.add(new StateDisabledFilter());
        filters.add(new StatePremiumFilter());
        filters.add(new StateNonPremiumFilter());
        filters.add(new StateOfflineLogFilter());
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

        @Override
        public boolean accepts(@NonNull final Geocache cache) {
            return cache.isFound();
        }

    }

    static class StateNotFoundFilter extends AbstractFilter {

        public StateNotFoundFilter() {
            super(R.string.cache_not_status_found);
        }

        @Override
        public boolean accepts(@NonNull final Geocache cache) {
            return !cache.isFound();
        }

    }

    static class StateArchivedFilter extends AbstractFilter {
        public StateArchivedFilter() {
            super(R.string.cache_status_archived);
        }

        @Override
        public boolean accepts(@NonNull final Geocache cache) {
            return cache.isArchived();
        }
    }

    static class StateDisabledFilter extends AbstractFilter {
        public StateDisabledFilter() {
            super(R.string.cache_status_disabled);
        }

        @Override
        public boolean accepts(@NonNull final Geocache cache) {
            return cache.isDisabled();
        }
    }

    static class StatePremiumFilter extends AbstractFilter {
        public StatePremiumFilter() {
            super(R.string.cache_status_premium);
        }

        @Override
        public boolean accepts(@NonNull final Geocache cache) {
            return cache.isPremiumMembersOnly();
        }
    }

    static class StateNonPremiumFilter extends AbstractFilter {
        public StateNonPremiumFilter() {
            super(R.string.cache_status_not_premium);
        }

        @Override
        public boolean accepts(@NonNull final Geocache cache) {
            return !cache.isPremiumMembersOnly();
        }
    }

    private static class StateOfflineLogFilter extends AbstractFilter {
        public StateOfflineLogFilter() {
            super(R.string.cache_status_offline_log);
        }

        @Override
        public boolean accepts(@NonNull final Geocache cache) {
            return cache.isLogOffline();
        }
    }

    static class StateStoredFilter extends AbstractFilter {
        public StateStoredFilter() {
            super(R.string.cache_status_stored);
        }

        @Override
        public boolean accepts(@NonNull final Geocache cache) {
            return cache.isOffline();
        }
    }

    static class StateNotStoredFilter extends AbstractFilter {
        public StateNotStoredFilter() {
            super(R.string.cache_status_not_stored);
        }

        @Override
        public boolean accepts(@NonNull final Geocache cache) {
            return !cache.isOffline();
        }
    }

}
