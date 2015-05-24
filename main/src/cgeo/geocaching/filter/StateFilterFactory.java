package cgeo.geocaching.filter;

import cgeo.geocaching.Geocache;
import cgeo.geocaching.R;

import org.eclipse.jdt.annotation.NonNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

class StateFilterFactory implements IFilterFactory {

    private static final long serialVersionUID = -4915854201507009125L;

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

        private static final long serialVersionUID = -3767242360632349788L;

        public StateFoundFilter() {
            super(R.string.cache_status_found);
        }

        @Override
        public boolean accepts(@NonNull final Geocache cache) {
            return cache.isFound();
        }

    }

    static class StateNotFoundFilter extends AbstractFilter {

        private static final long serialVersionUID = 19776079495156351L;

        public StateNotFoundFilter() {
            super(R.string.cache_not_status_found);
        }

        @Override
        public boolean accepts(@NonNull final Geocache cache) {
            return !cache.isFound();
        }

    }

    static class StateArchivedFilter extends AbstractFilter {
        private static final long serialVersionUID = -4272399405273124686L;

        public StateArchivedFilter() {
            super(R.string.cache_status_archived);
        }

        @Override
        public boolean accepts(@NonNull final Geocache cache) {
            return cache.isArchived();
        }
    }

    static class StateDisabledFilter extends AbstractFilter {
        private static final long serialVersionUID = -3027505042498459672L;

        public StateDisabledFilter() {
            super(R.string.cache_status_disabled);
        }

        @Override
        public boolean accepts(@NonNull final Geocache cache) {
            return cache.isDisabled();
        }
    }

    static class StatePremiumFilter extends AbstractFilter {
        private static final long serialVersionUID = -4086779915486623739L;

        public StatePremiumFilter() {
            super(R.string.cache_status_premium);
        }

        @Override
        public boolean accepts(@NonNull final Geocache cache) {
            return cache.isPremiumMembersOnly();
        }
    }

    static class StateNonPremiumFilter extends AbstractFilter {
        private static final long serialVersionUID = 6427819310603779646L;

        public StateNonPremiumFilter() {
            super(R.string.cache_status_not_premium);
        }

        @Override
        public boolean accepts(@NonNull final Geocache cache) {
            return !cache.isPremiumMembersOnly();
        }
    }

    private static class StateOfflineLogFilter extends AbstractFilter {
        private static final long serialVersionUID = -6076510706828408970L;

        public StateOfflineLogFilter() {
            super(R.string.cache_status_offline_log);
        }

        @Override
        public boolean accepts(@NonNull final Geocache cache) {
            return cache.isLogOffline();
        }
    }

    static class StateStoredFilter extends AbstractFilter {
        private static final long serialVersionUID = -2455064686291969386L;

        public StateStoredFilter() {
            super(R.string.cache_status_stored);
        }

        @Override
        public boolean accepts(@NonNull final Geocache cache) {
            return cache.isOffline();
        }
    }

    static class StateNotStoredFilter extends AbstractFilter {
        private static final long serialVersionUID = 1774243798304092972L;

        public StateNotStoredFilter() {
            super(R.string.cache_status_not_stored);
        }

        @Override
        public boolean accepts(@NonNull final Geocache cache) {
            return !cache.isOffline();
        }
    }

}
