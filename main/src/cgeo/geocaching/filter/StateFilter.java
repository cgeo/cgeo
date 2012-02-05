package cgeo.geocaching.filter;

import cgeo.geocaching.R;
import cgeo.geocaching.cgCache;
import cgeo.geocaching.cgeoapplication;

import android.content.res.Resources;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

public abstract class StateFilter extends AbstractFilter {

    public StateFilter(String name) {
        super(name);
    }

    private static class StateFoundFilter extends StateFilter {

        public StateFoundFilter(String name) {
            super(name);
        }

        @Override
        public boolean accepts(cgCache cache) {
            return cache.isFound();
        }

    }

    private static class StateArchivedFilter extends StateFilter {
        public StateArchivedFilter(String name) {
            super(name);
        }

        @Override
        public boolean accepts(cgCache cache) {
            return cache.isArchived();
        }
    }

    private static class StateDisabledFilter extends StateFilter {
        public StateDisabledFilter(String name) {
            super(name);
        }

        @Override
        public boolean accepts(cgCache cache) {
            return cache.isDisabled();
        }
    }

    private static class StatePremiumFilter extends StateFilter {
        public StatePremiumFilter(String name) {
            super(name);
        }

        @Override
        public boolean accepts(cgCache cache) {
            return cache.isPremiumMembersOnly();
        }
    }

    private static class StateOfflineLogFilter extends StateFilter {
        public StateOfflineLogFilter(String name) {
            super(name);
        }

        @Override
        public boolean accepts(cgCache cache) {
            return cache.isLogOffline();
        }
    }

    public static AbstractFilter[] getAllFilters() {
        final Resources res = cgeoapplication.getInstance().getResources();
        final ArrayList<StateFilter> filters = new ArrayList<StateFilter>();
        filters.add(new StateFoundFilter(res.getString(R.string.cache_status_found)));
        filters.add(new StateArchivedFilter(res.getString(R.string.cache_status_archived)));
        filters.add(new StateDisabledFilter(res.getString(R.string.cache_status_disabled)));
        filters.add(new StatePremiumFilter(res.getString(R.string.cache_status_premium)));
        filters.add(new StateOfflineLogFilter(res.getString(R.string.cache_status_offline_log)));

        Collections.sort(filters, new Comparator<StateFilter>() {

            @Override
            public int compare(StateFilter filter1, StateFilter filter2) {
                return filter1.getName().compareToIgnoreCase(filter2.getName());
            }
        });

        return filters.toArray(new StateFilter[filters.size()]);
    }

}
