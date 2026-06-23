package cgeo.geocaching.filters.gui;

import cgeo.geocaching.filters.FilterUtils;
import cgeo.geocaching.filters.core.IGeocacheFilter;

import android.view.View;

import java.util.Collections;


public class NamedFilterFilterViewHolder<T, F extends IGeocacheFilter> extends CheckboxFilterViewHolder<T, F> {

        public NamedFilterFilterViewHolder(final ValueGroupFilterAccessor<T, F> filterAccessor) {
            super(filterAccessor, 1, Collections.emptySet(), false);
        }

        @Override
        protected View.OnClickListener getAddItemButtonCallback() {
            return v -> FilterUtils.openDialogActivateMarkers(getActivity());
    }
}
