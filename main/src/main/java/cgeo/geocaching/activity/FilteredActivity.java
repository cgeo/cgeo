package cgeo.geocaching.activity;

import cgeo.geocaching.filters.core.GeocacheFilter;


public interface FilteredActivity {
    /**
     * called from the filter bar view
     */
    void showFilterMenu();

    boolean showSavedFilterList();

    void refreshWithFilter(GeocacheFilter filter);
}
