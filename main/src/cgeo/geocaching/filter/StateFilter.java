package cgeo.geocaching.filter;

import cgeo.geocaching.R;
import cgeo.geocaching.cgCache;
import cgeo.geocaching.cgeoapplication;

import android.content.res.Resources;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

abstract class StateFilter extends AbstractFilter {

    final static Resources res = cgeoapplication.getInstance().getResources();

    protected StateFilter(String name) {
        super(name);
    }

    static class StateFoundFilter extends StateFilter {

        public StateFoundFilter() {
            super(res.getString(R.string.cache_status_found));
        }

        @Override
        public boolean accepts(cgCache cache) {
            return cache.isFound();
        }

    }

    static class StateArchivedFilter extends StateFilter {
        public StateArchivedFilter() {
            super(res.getString(R.string.cache_status_archived));
        }

        @Override
        public boolean accepts(cgCache cache) {
            return cache.isArchived();
        }
    }

    static class StateDisabledFilter extends StateFilter {
        public StateDisabledFilter() {
            super(res.getString(R.string.cache_status_disabled));
        }

        @Override
        public boolean accepts(cgCache cache) {
            return cache.isDisabled();
        }
    }

    static class StatePremiumFilter extends StateFilter {
        public StatePremiumFilter() {
            super(res.getString(R.string.cache_status_premium));
        }

        @Override
        public boolean accepts(cgCache cache) {
            return cache.isPremiumMembersOnly();
        }
    }

    private static class StateOfflineLogFilter extends StateFilter {
        public StateOfflineLogFilter() {
            super(res.getString(R.string.cache_status_offline_log));
        }

        @Override
        public boolean accepts(cgCache cache) {
            return cache.isLogOffline();
        }
    }

    public static class Factory implements IFilterFactory {

        @Override
        public IFilter[] getFilters() {
            final ArrayList<StateFilter> filters = new ArrayList<StateFilter>();
            filters.add(new StateFoundFilter());
            filters.add(new StateArchivedFilter());
            filters.add(new StateDisabledFilter());
            filters.add(new StatePremiumFilter());
            filters.add(new StateOfflineLogFilter());

            Collections.sort(filters, new Comparator<StateFilter>() {

                @Override
                public int compare(StateFilter filter1, StateFilter filter2) {
                    return filter1.getName().compareToIgnoreCase(filter2.getName());
                }
            });

            return filters.toArray(new StateFilter[filters.size()]);
        }

    }

}
