package cgeo.geocaching.filters.gui;

import cgeo.geocaching.filters.core.DateRangeGeocacheFilter;
import cgeo.geocaching.ui.DateRangeSelector;

import android.view.View;


public class DateRangeFilterViewHolder<F extends DateRangeGeocacheFilter> extends BaseFilterViewHolder<F> {


    private DateRangeSelector dateRangeSelector;

    @Override
    public View createView() {

        dateRangeSelector = new DateRangeSelector(getActivity());
        return dateRangeSelector;
    }

    @Override
    public void setViewFromFilter(final DateRangeGeocacheFilter filter) {
        dateRangeSelector.setMinMaxDate(filter.getMinDate(), filter.getMaxDate());
    }

    @Override
    public F createFilterFromView() {
        final F filter = createFilter();
        filter.setMinMaxDate(dateRangeSelector.getMinDate(), dateRangeSelector.getMaxDate());
        return filter;
    }


}
