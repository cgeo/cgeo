package cgeo.geocaching.filters.gui;

import cgeo.geocaching.R;
import cgeo.geocaching.filters.FilterUtils;
import cgeo.geocaching.filters.NamedFilter;
import cgeo.geocaching.filters.core.IGeocacheFilter;
import cgeo.geocaching.ui.TextParam;

import android.view.View;

import java.util.Collections;


public class NamedFilterFilterViewHolder<F extends IGeocacheFilter> extends CheckboxFilterViewHolder<NamedFilter, F> {

    public NamedFilterFilterViewHolder(final ValueGroupFilterAccessor<NamedFilter, F> filterAccessor) {
            super(filterAccessor, 1, Collections.emptySet(), false);
        }

        @Override
        protected View.OnClickListener getAddItemButtonCallback() {
            return v -> FilterUtils.openDialogMultiSelectNamedFilter(getActivity(),
                    TextParam.id(R.string.cache_filter_storage_select_title), this::addItems, visibleValues);
        }
}
