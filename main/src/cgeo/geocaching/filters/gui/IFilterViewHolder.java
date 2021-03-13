package cgeo.geocaching.filters.gui;

import cgeo.geocaching.filters.core.GeocacheFilterType;
import cgeo.geocaching.filters.core.IGeocacheFilter;

import android.content.Context;
import android.view.View;

public interface IFilterViewHolder<T extends IGeocacheFilter> {

    void init(GeocacheFilterType type, Context context);

    GeocacheFilterType getType();

    Context getContext();

    void setViewFromFilter(T filter);

    View getView();

    T createFilterFromView();
}
