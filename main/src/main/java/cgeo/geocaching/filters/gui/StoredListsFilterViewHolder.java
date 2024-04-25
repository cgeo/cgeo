package cgeo.geocaching.filters.gui;

import cgeo.geocaching.R;
import cgeo.geocaching.filters.core.IGeocacheFilter;
import cgeo.geocaching.list.StoredList;

import android.view.View;
import android.widget.CheckBox;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class StoredListsFilterViewHolder<T, F extends IGeocacheFilter> extends CheckboxFilterViewHolder {
    public StoredListsFilterViewHolder(final ValueGroupFilterAccessor filterAccessor, final int colCount, final Set alwaysVisibleItems) {
        super(filterAccessor, colCount, alwaysVisibleItems);
    }

    @Override
    protected View.OnClickListener getAddItemButtonCallback() {
        return v -> {
            final List<T> items = new ArrayList<>(filterAccessor.getSelectableValues());
            items.removeAll(visibleValues);

            new StoredList.UserInterface(getActivity()).promptForMultiListSelection(R.string.lists_title,
                    s -> {
                        final Set<T> selectedListsSet = (Set<T>) filterAccessor.getSelectableValues().stream().filter(list -> s.contains(((StoredList) list).id)).collect(Collectors.toSet());
                        visibleValues.addAll(selectedListsSet);
                        for (T value : selectedListsSet) {
                            ((CheckBox) getValueCheckbox(value).right).setChecked(true);
                        }
                        relayout();
                    }, true, Collections.emptySet(), false);
        };
    }
}
