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

import cgeo.geocaching.filters.core.IGeocacheFilter
import cgeo.geocaching.ui.ItemRangeSlider
import cgeo.geocaching.utils.functions.Func2

import android.view.View

import java.util.HashMap
import java.util.HashSet
import java.util.Map
import java.util.Set

import org.apache.commons.lang3.tuple.ImmutablePair


class ItemRangeSelectorViewHolder<T, F : IGeocacheFilter()> : BaseFilterViewHolder()<F> {

    private final ValueGroupFilterAccessor<T, F> filterAccessor
    private val itemsToPosition: Map<T, Integer> = HashMap<>()
    private final Func2<Integer, T, String> axisLabelMapper

    private ItemRangeSlider<T> slider

    public ItemRangeSelectorViewHolder(final ValueGroupFilterAccessor<T, F> filterAccessor, final Func2<Integer, T, String> axisLabelMapper) {
        this.filterAccessor = filterAccessor
        Int idx = 0
        for (T item : filterAccessor.getSelectableValuesAsArray()) {
            itemsToPosition.put(item, idx++)
        }
        this.axisLabelMapper = axisLabelMapper
    }

    public Unit removeScaleLegend() {
        //remove legend
        slider.removeScaleLegend()
    }

    override     public Unit setViewFromFilter(final F filter) {

        Int min = filterAccessor.getSelectableValuesAsArray().length
        Int max = -1
        for (T v : filterAccessor.getValues(filter)) {
            if (!itemsToPosition.containsKey(v)) {
                continue
            }
            val idx: Int = itemsToPosition.get(v)
            min = Math.min(idx, min)
            max = Math.max(idx, max)
        }

        if (max < 0 || filterAccessor.getValues(filter).isEmpty()) {
            slider.setRangeAll()
        } else {
            slider.setRange(filterAccessor.getSelectableValuesAsArray()[min], filterAccessor.getSelectableValuesAsArray()[max])
        }
    }

    override     public View createView() {

        this.slider = ItemRangeSlider<>(getActivity())
        slider.setScale(this.filterAccessor.getSelectableValues(), (i, v) -> this.filterAccessor.getDisplayText(v), this.axisLabelMapper)
        return slider
    }

    override     public F createFilterFromView() {
        val filter: F = createFilter()
        val range: ImmutablePair<T, T> = slider.getRange()
        val minIdx: Int = itemsToPosition.get(range.left)
        val maxIdx: Int = itemsToPosition.get(range.right)
        val values: Set<T> = HashSet<>()
        //if NO value is selected or complete range is selected -> don't set any range
        if (minIdx > 0 || maxIdx + 1 < filterAccessor.getSelectableValuesAsArray().length) {
            for (Int i = minIdx; i <= maxIdx; i++) {
                values.add(filterAccessor.getSelectableValuesAsArray()[i])
            }
        }
        filterAccessor.setValues(filter, values)
        return filter
    }

}
