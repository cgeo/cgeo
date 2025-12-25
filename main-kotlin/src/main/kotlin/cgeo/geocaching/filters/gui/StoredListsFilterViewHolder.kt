// Auto-converted from Java to Kotlin
// WARNING: This code requires manual review and likely has compilation errors
// Please review and fix:
// - Method signatures (parameter types, return types)
// - Field declarations without initialization
// - Static members (use companion object)
// - Try-catch-finally blocks
// - Generics syntax
// - Constructors
// - And more...

package cgeo.geocaching.filters.gui

import cgeo.geocaching.R
import cgeo.geocaching.filters.core.IGeocacheFilter
import cgeo.geocaching.list.StoredList

import android.view.View
import android.widget.CheckBox

import java.util.ArrayList
import java.util.Collections
import java.util.List
import java.util.Set
import java.util.stream.Collectors

class StoredListsFilterViewHolder<T, F : IGeocacheFilter()> : CheckboxFilterViewHolder() {
    public StoredListsFilterViewHolder(final ValueGroupFilterAccessor filterAccessor, final Int colCount, final Set alwaysVisibleItems) {
        super(filterAccessor, colCount, alwaysVisibleItems)
    }

    override     protected View.OnClickListener getAddItemButtonCallback() {
        return v -> {
            val items: List<T> = ArrayList<>(filterAccessor.getSelectableValues())
            items.removeAll(visibleValues)

            StoredList.UserInterface(getActivity()).promptForMultiListSelection(R.string.lists_title,
                    s -> {
                        val selectedListsSet: Set<T> = (Set<T>) filterAccessor.getSelectableValues().stream().filter(list -> s.contains(((StoredList) list).id)).collect(Collectors.toSet())
                        visibleValues.addAll(selectedListsSet)
                        for (T value : selectedListsSet) {
                            ((CheckBox) getValueCheckbox(value).right).setChecked(true)
                        }
                        relayout()
                    }, true, Collections.emptySet(), false)
        }
    }
}
