package cgeo.geocaching.filters.gui;

import cgeo.geocaching.R;
import cgeo.geocaching.filters.core.IGeocacheFilter;
import cgeo.geocaching.list.StoredList;

import android.view.View;

import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

public class StoredListsFilterViewHolder<T, F extends IGeocacheFilter> extends CheckboxFilterViewHolder<T, F> {
    public StoredListsFilterViewHolder(final ValueGroupFilterAccessor<T, F> filterAccessor) {
        super(filterAccessor, 1, Collections.emptySet(), false);
    }

    @Override
    protected View.OnClickListener getAddItemButtonCallback() {
        return v -> new StoredList.UserInterface(getActivity()).promptForMultiListSelection(R.string.lists_title,
        s -> {
            final Set<T> selectedListsSet = filterAccessor.getSelectableValues().stream().filter(list -> s.contains(((StoredList) list).id)).collect(Collectors.toSet());
            visibleValues.addAll(selectedListsSet);
            for (T value : selectedListsSet) {
                getValueCheckbox(value).right.setChecked(true);
            }
            relayout();
        }, true, Collections.emptySet(), false);
    }
}
