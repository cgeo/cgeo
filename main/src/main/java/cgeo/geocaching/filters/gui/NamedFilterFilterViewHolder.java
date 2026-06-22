package cgeo.geocaching.filters.gui;

import cgeo.geocaching.R;
import cgeo.geocaching.filters.FilterUtils;
import cgeo.geocaching.filters.NamedFilter;
import cgeo.geocaching.filters.core.IGeocacheFilter;
import cgeo.geocaching.ui.TextParam;

import android.view.View;

import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;


public class NamedFilterFilterViewHolder<T, F extends IGeocacheFilter> extends CheckboxFilterViewHolder<T, F> {

        public NamedFilterFilterViewHolder(final ValueGroupFilterAccessor<T, F> filterAccessor) {
            super(filterAccessor, 1, Collections.emptySet(), false);
        }

        @Override
        protected View.OnClickListener getAddItemButtonCallback() {
            return v -> FilterUtils.openDialogMultiselectNamedFilters(getActivity(), TextParam.id(R.string.named_filter_select_title), null, nfs -> {
                final Set<Integer> nfIds = nfs.stream().map(NamedFilter::getId).collect(Collectors.toSet());
                final Set<T> selectedListsSet = filterAccessor.getSelectableValues().stream().filter(nf -> nfIds.contains(((NamedFilter) nf).getId())).collect(Collectors.toSet());
                visibleValues.addAll(selectedListsSet);
                for (T value : selectedListsSet) {
                    getValueCheckbox(value).right.setChecked(true);
                }
                relayout();
            });
    }
}
