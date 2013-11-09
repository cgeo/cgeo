package cgeo.geocaching.filter;

import cgeo.geocaching.CgeoApplication;
import cgeo.geocaching.Geocache;
import cgeo.geocaching.R;

import android.content.res.Resources;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

abstract class StateFilter extends AbstractFilter {

    static final Resources res = CgeoApplication.getInstance().getResources();

    protected StateFilter(final String name) {
        super(name);
    }

    static class StateFoundFilter extends StateFilter {

        public StateFoundFilter() {
            super(res.getString(R.string.cache_status_found));
        }

        @Override
        public boolean accepts(final Geocache cache) {
            return cache.isFound();
        }

    }

    static class StateArchivedFilter extends StateFilter {
        public StateArchivedFilter() {
            super(res.getString(R.string.cache_status_archived));
        }

        @Override
        public boolean accepts(final Geocache cache) {
            return cache.isArchived();
        }
    }

    static class StateDisabledFilter extends StateFilter {
        public StateDisabledFilter() {
            super(res.getString(R.string.cache_status_disabled));
        }

        @Override
        public boolean accepts(final Geocache cache) {
            return cache.isDisabled();
        }
    }

    static class StatePremiumFilter extends StateFilter {
        public StatePremiumFilter() {
            super(res.getString(R.string.cache_status_premium));
        }

        @Override
        public boolean accepts(final Geocache cache) {
            return cache.isPremiumMembersOnly();
        }
    }

    static class StateNonPremiumFilter extends StateFilter {
        public StateNonPremiumFilter() {
            super(res.getString(R.string.cache_status_not_premium));
        }

        @Override
        public boolean accepts(final Geocache cache) {
            return !cache.isPremiumMembersOnly();
        }
    }

    private static class StateOfflineLogFilter extends StateFilter {
        public StateOfflineLogFilter() {
            super(res.getString(R.string.cache_status_offline_log));
        }

        @Override
        public boolean accepts(final Geocache cache) {
            return cache.isLogOffline();
        }
    }

    static class StateStoredFilter extends StateFilter {
        public StateStoredFilter() {
            super(res.getString(R.string.cache_status_stored));
        }

        @Override
        public boolean accepts(final Geocache cache) {
            return cache.isOffline();
        }
    }

    static class StateNotStoredFilter extends StateFilter {
        public StateNotStoredFilter() {
            super(res.getString(R.string.cache_status_not_stored));
        }

        @Override
        public boolean accepts(final Geocache cache) {
            return !cache.isOffline();
        }
    }

    public static class Factory implements IFilterFactory {

        @Override
        public List<StateFilter> getFilters() {
            final List<StateFilter> filters = new ArrayList<StateFilter>(6);
            filters.add(new StateFoundFilter());
            filters.add(new StateArchivedFilter());
            filters.add(new StateDisabledFilter());
            filters.add(new StatePremiumFilter());
            filters.add(new StateNonPremiumFilter());
            filters.add(new StateOfflineLogFilter());
            filters.add(new StateStoredFilter());
            filters.add(new StateNotStoredFilter());

            Collections.sort(filters, new Comparator<StateFilter>() {

                @Override
                public int compare(final StateFilter filter1, final StateFilter filter2) {
                    return String.CASE_INSENSITIVE_ORDER.compare(filter1.getName(), filter2.getName());
                }
            });

            return filters;
        }

    }

}
