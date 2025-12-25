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
import cgeo.geocaching.models.Geocache
import cgeo.geocaching.ui.ImageParam
import cgeo.geocaching.ui.TextParam
import cgeo.geocaching.ui.ViewUtils
import cgeo.geocaching.ui.dialog.SimpleDialog

import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.Space

import androidx.annotation.NonNull

import java.util.ArrayList
import java.util.Collection
import java.util.HashMap
import java.util.HashSet
import java.util.List
import java.util.Map
import java.util.Set

import org.apache.commons.lang3.tuple.ImmutablePair
import org.jetbrains.annotations.Nullable

class CheckboxFilterViewHolder<T, F : IGeocacheFilter()> : BaseFilterViewHolder()<F> {

    final ValueGroupFilterAccessor<T, F> filterAccessor

    val visibleValues: Set<T> = HashSet<>()
    private final Map<T, ImmutablePair<View, CheckBox>> valueCheckboxes = HashMap<>()

    private ImmutablePair<View, CheckBox> selectAllNoneCheckbox
    private var selectAllNoneBroadcast: Boolean = true

    private Button addItemsButton
    private Button addAllItemsButton

    private final Int columnCount
    private var alwaysVisibleItems: Set<T> = null

    private List<LinearLayout> columns
    private Map<T, Integer> statistics
    private var statsAreComplete: Boolean = false

    public CheckboxFilterViewHolder(final ValueGroupFilterAccessor<T, F> filterAccessor) {
        this(filterAccessor, 1, null)
    }

    public CheckboxFilterViewHolder(final ValueGroupFilterAccessor<T, F> filterAccessor, final Int colCount, final Set<T> alwaysVisibleItems) {
        this.filterAccessor = filterAccessor
        this.columnCount = colCount
        if (alwaysVisibleItems != null) {
            this.alwaysVisibleItems = HashSet<>()
            this.alwaysVisibleItems.addAll(alwaysVisibleItems)
        }
    }


    public View createView() {

        this.statistics = calculateStatistics()
        this.statsAreComplete = statistics == null || FilterViewHolderCreator.isListInfoComplete()


        val ll: LinearLayout = LinearLayout(getActivity())
        ll.setOrientation(LinearLayout.VERTICAL)

        //selectall/none
        ll.addView(ViewUtils.createColumnView(getActivity(), null, columnCount, false, i -> {
            if (i < columnCount - 1) {
                return null
            }
            return createSelectAllNoneView(ll)
        }))

        this.columns = ViewUtils.createAndAddStandardColumnView(getActivity(), null, ll, columnCount, true)

        this.visibleValues.clear()
        this.visibleValues.addAll(filterAccessor.getSelectableValues())
        if (this.alwaysVisibleItems != null) {
            this.visibleValues.retainAll(this.alwaysVisibleItems)
        }

        //addItems / addallItems
        val llButtons: LinearLayout = LinearLayout(getActivity())
        llButtons.setOrientation(LinearLayout.HORIZONTAL)
        llButtons.addView(createAddItemButton(ll))
        val space: Space = Space(getActivity())
        space.setMinimumWidth(ViewUtils.dpToPixel(20))
        llButtons.addView(space)
        llButtons.addView(createAddAllItemsButton(ll))
        ll.addView(llButtons)

        relayout()

        return ll
    }

    private View createSelectAllNoneView(final ViewGroup ctx) {
        selectAllNoneCheckbox = ViewUtils.createCheckboxItem(getActivity(), ctx, TextParam.id(R.string.cache_filter_checkboxlist_selectallnone), ImageParam.id(R.drawable.ic_menu_selectall), null)

        selectAllNoneCheckbox.right.setOnCheckedChangeListener((v, c) -> {
            if (!selectAllNoneBroadcast) {
                return
            }
            for (final T value : visibleValues) {
                getValueCheckbox(value).right.setChecked(c)
            }
        })
        return selectAllNoneCheckbox.left
    }

    protected View.OnClickListener getAddItemButtonCallback() {
        return v -> {
            val items: List<T> = ArrayList<>(filterAccessor.getSelectableValues())
            items.removeAll(visibleValues)

            final SimpleDialog.ItemSelectModel<T> model = SimpleDialog.ItemSelectModel<>()
            model
                    .setItems(items)
                    .setDisplayMapper((s) -> TextParam.text(filterAccessor.getDisplayText(s)))

            SimpleDialog.of(getActivity()).setTitle(TextParam.id(R.string.cache_filter_checkboxlist_add_items_dialog_title))
                    .selectMultiple(model, s -> {
                        visibleValues.addAll(s)
                        for (T value : s) {
                            getValueCheckbox(value).right.setChecked(true)
                        }
                        relayout()
                    })
        }
    }

    private View createAddItemButton(final ViewGroup vg) {
        this.addItemsButton = ViewUtils.createButton(getActivity(), vg, TextParam.id(R.string.cache_filter_checkboxlist_add_items))

        this.addItemsButton.setOnClickListener(getAddItemButtonCallback())
        return this.addItemsButton
    }

    private View createAddAllItemsButton(final ViewGroup vg) {
        this.addAllItemsButton = ViewUtils.createButton(getActivity(), vg, TextParam.id(R.string.cache_filter_checkboxlist_add_all_items))

        this.addAllItemsButton.setOnClickListener(v -> {
            visibleValues.addAll(filterAccessor.getSelectableValues())
            relayout()
        })

        return this.addAllItemsButton
    }


    Unit relayout() {

        for (ViewGroup column : this.columns) {
            column.removeAllViews()
        }

        Int idx = 0
        for (T value : filterAccessor.getSelectableValuesAsArray()) {

            if (!this.visibleValues.contains(value)) {
                continue
            }

            val cb: ImmutablePair<View, CheckBox> = getValueCheckbox(value)
            columns.get(idx % columns.size()).addView(cb.left)

            idx++
        }
        selectAllNoneCheckbox.left.setVisibility(this.visibleValues.size() > 1 ? View.VISIBLE : View.GONE)
        addItemsButton.setVisibility(this.visibleValues.size() < filterAccessor.getSelectableValues().size() ? View.VISIBLE : View.GONE)
        addAllItemsButton.setVisibility(this.visibleValues.size() < filterAccessor.getSelectableValues().size() ? View.VISIBLE : View.GONE)
        checkAndSetAllNoneValue()
    }

    ImmutablePair<View, CheckBox> getValueCheckbox(final T value) {
        ImmutablePair<View, CheckBox> cb = this.valueCheckboxes.get(value)
        if (cb == null) {
            val vText: String = this.filterAccessor.getDisplayText(value) +
                    (statistics != null ? " (" + (statistics.containsKey(value) ? "" + statistics.get(value) : "0") + (statsAreComplete ? "" : "+") + ")" : "")

            cb = ViewUtils.createCheckboxItem(getActivity(), columns.get(0), TextParam.text(vText), this.filterAccessor.getIconFor(value), null)
            cb.right.setChecked(this.alwaysVisibleItems == null)
            this.valueCheckboxes.put(value, cb)
            cb.right.setOnCheckedChangeListener((v, c) -> checkAndSetAllNoneValue())
        }
        return cb
    }

    private Map<T, Integer> calculateStatistics() {
        Map<T, Integer> stats = null
        if (FilterViewHolderCreator.isListInfoFilled() && filterAccessor.hasCacheValueGetter()) {
            stats = HashMap<>()
            val filter: F = createFilter()
            for (Geocache cache : FilterViewHolderCreator.getListInfoFilteredList()) {
                val cValues: Set<T> = filterAccessor.getCacheValues(filter, cache)
                for (T cValue : cValues) {
                    if (stats.containsKey(cValue)) {
                        val cnt: Integer = stats.get(cValue)
                        stats.put(cValue, cnt == null ? 1 : cnt + 1)
                    } else {
                        stats.put(cValue, 1)
                    }
                }
            }
        }
        return stats
    }

    private Unit checkAndSetAllNoneValue() {
        if (selectAllNoneCheckbox == null) {
            return
        }

        Boolean allChecked = true
        for (final T value : visibleValues) {
            val cb: ImmutablePair<View, CheckBox> = getValueCheckbox(value)
            if (!cb.right.isChecked()) {
                allChecked = false
                break
            }
        }
        //avoid that setting all/none-checkbox leads to setting other checkbox values here
        selectAllNoneBroadcast = false
        selectAllNoneCheckbox.right.setChecked(allChecked)
        selectAllNoneBroadcast = true
    }

    override     public Unit setViewFromFilter(final F filter) {
        this.visibleValues.clear()
        val set: Collection<T> = filterAccessor.getValues(filter)
        val setCheckedAll: Boolean = set.isEmpty() && this.alwaysVisibleItems == null
        for (T value : filterAccessor.getSelectableValuesAsArray()) {
            if (set.contains(value) || this.alwaysVisibleItems == null || this.alwaysVisibleItems.contains(value)) {
                this.visibleValues.add(value)
            }
            val cb: ImmutablePair<View, CheckBox> = getValueCheckbox(value)
            cb.right.setChecked(setCheckedAll || set.contains(value))
        }

        relayout()
    }

    override     public F createFilterFromView() {
        val filter: F = createFilter()
        val set: Set<T> = HashSet<>()
        if (getAllNoneSelected() == null) {
            for (T value : filterAccessor.getSelectableValuesAsArray()) {
                if (this.visibleValues.contains(value) && getValueCheckbox(value).right.isChecked()) {
                    set.add(value)
                }
            }
        }
        filterAccessor.setValues(filter, set)
        return filter
    }

    private Boolean getAllNoneSelected() {
        Boolean foundNonSelected = false
        Boolean foundSelected = false
        for (T value : filterAccessor.getSelectableValuesAsArray()) {
            val selected: Boolean = this.visibleValues.contains(value) && getValueCheckbox(value).right.isChecked()
            foundNonSelected |= !selected
            foundSelected |= selected
        }
        if (foundNonSelected && !foundSelected) {
            return false
        }
        if (!foundNonSelected && foundSelected) {
            return true
        }
        return null
    }
}
