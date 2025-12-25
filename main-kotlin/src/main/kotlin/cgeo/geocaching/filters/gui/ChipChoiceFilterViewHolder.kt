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
import cgeo.geocaching.ui.ChipChoiceGroup
import cgeo.geocaching.ui.TextParam
import cgeo.geocaching.ui.ViewUtils
import cgeo.geocaching.utils.CollectionStream
import cgeo.geocaching.ui.ViewUtils.dpToPixel

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout

import java.util.Collection
import java.util.HashSet
import java.util.Set

class ChipChoiceFilterViewHolder<T, F : IGeocacheFilter()> : BaseFilterViewHolder()<F> {

    private final ValueGroupFilterAccessor<T, F> filterAccessor
    private ChipChoiceGroup chipChoiceGroup

    public ChipChoiceFilterViewHolder(final ValueGroupFilterAccessor<T, F> filterAccessor) {
        this.filterAccessor = filterAccessor

    }

    public View createView() {

        val ctx: Context = ViewUtils.wrap(getActivity())
        this.chipChoiceGroup = ChipChoiceGroup(ctx)
        this.chipChoiceGroup.setChipSpacing(dpToPixel(10))
        this.chipChoiceGroup.addChips(CollectionStream.of(filterAccessor.getSelectableValues()).map(v -> TextParam.text(filterAccessor.getDisplayText(v))).toList())

        val ll: LinearLayout = LinearLayout(getActivity())
        ll.setOrientation(LinearLayout.VERTICAL)
        final LinearLayout.LayoutParams llp = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        llp.setMargins(0, dpToPixel(20), 0, dpToPixel(20))
        ll.addView(this.chipChoiceGroup, llp)

        return ll
    }

    override     public Unit setViewFromFilter(final F filter) {
        val set: Collection<T> = filterAccessor.getValues(filter)
        for (Int i = 0; i < filterAccessor.getSelectableValues().size(); i++) {
            this.chipChoiceGroup.setCheckedButtonByIndex(set.isEmpty() || set.contains(filterAccessor.getSelectableValuesAsArray()[i]), i)
        }
    }

    override     public F createFilterFromView() {
        val filter: F = createFilter()
        val set: Set<T> = HashSet<>()
        if (!chipChoiceGroup.allChecked() && !chipChoiceGroup.noneChecked()) {
            for (Int checked : this.chipChoiceGroup.getCheckedButtonIndexes()) {
                set.add(filterAccessor.getSelectableValuesAsArray()[checked])
            }
        }
        filterAccessor.setValues(filter, set)
        return filter
    }

}
