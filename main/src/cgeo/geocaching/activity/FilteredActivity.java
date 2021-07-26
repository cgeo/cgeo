package cgeo.geocaching.activity;

import cgeo.geocaching.filters.core.GeocacheFilter;

import android.view.View;


public interface FilteredActivity {
    /**
     * called from the filter bar view
     */
    void showFilterMenu(View view);

    boolean showFilterList(View view);

    void refreshWithFilter(GeocacheFilter filter);
}
