package cgeo.geocaching.filters.gui;

import cgeo.geocaching.R;
import cgeo.geocaching.filters.core.IGeocacheFilter;
import cgeo.geocaching.list.ListNameMemento;
import cgeo.geocaching.list.StoredList;

import android.view.View;

import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

public class StoredListsFilterViewHolder<F extends IGeocacheFilter> extends CheckboxFilterViewHolder<StoredList, F> {
    public StoredListsFilterViewHolder(final ValueGroupFilterAccessor<StoredList, F> filterAccessor) {
        super(filterAccessor, 1, Collections.emptySet(), false);
    }

    @Override
    protected View.OnClickListener getAddItemButtonCallback() {
        return v -> {
            final Set<Integer> exceptListIds = visibleValues.stream()
                    .map(item -> item.id).collect(Collectors.toSet());

            new StoredList.UserInterface(getActivity()).promptForMultiListSelection(R.string.lists_title,
        s -> {
            final Set<StoredList> selectableValues = filterAccessor.getSelectableValues().stream()
                    .filter(list -> s.contains(list.id)).collect(Collectors.toSet());
            addItems(selectableValues);
        }, true, exceptListIds, Collections.emptySet(), ListNameMemento.EMPTY, false);
        };
    }
}
