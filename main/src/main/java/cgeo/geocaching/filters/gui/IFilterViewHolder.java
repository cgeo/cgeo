package cgeo.geocaching.filters.gui;

import cgeo.geocaching.filters.core.GeocacheFilterType;
import cgeo.geocaching.filters.core.IGeocacheFilter;

import android.app.Activity;
import android.view.View;

public interface IFilterViewHolder<T extends IGeocacheFilter> {

    void init(GeocacheFilterType type, Activity activity);

    GeocacheFilterType getType();

    Activity getActivity();

    void setViewFromFilter(T filter);

    View getView();

    T createFilterFromView();

    void setAdvancedMode(boolean isAdvanced);

    boolean canBeSwitchedToBasicLossless();
}
