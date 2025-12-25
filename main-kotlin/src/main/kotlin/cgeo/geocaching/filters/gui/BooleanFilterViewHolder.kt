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
import cgeo.geocaching.databinding.ButtontogglegroupLabeledItemBinding
import cgeo.geocaching.filters.core.BooleanGeocacheFilter
import cgeo.geocaching.ui.ButtonToggleGroup
import cgeo.geocaching.ui.ImageParam

import android.view.View

import androidx.annotation.Nullable
import androidx.annotation.StringRes

class BooleanFilterViewHolder : BaseFilterViewHolder()<BooleanGeocacheFilter> {

    private var valueGroup: ButtonToggleGroup = null

    @StringRes
    public final Int labelId
    public final ImageParam icon


    public BooleanFilterViewHolder(final Int labelId, final ImageParam icon) {
        this.labelId = labelId
        this.icon = icon
    }

    override     public View createView() {

        val view: View = inflateLayout(R.layout.buttontogglegroup_labeled_item)
        val binding: ButtontogglegroupLabeledItemBinding = ButtontogglegroupLabeledItemBinding.bind(view)
        binding.itemText.setText(labelId)
        if (icon != null) {
            icon.applyTo(binding.itemIcon)
        }

        binding.itemTogglebuttongroup.addButtons(R.string.cache_filter_status_select_all, R.string.cache_filter_status_select_yes, R.string.cache_filter_status_select_no)

        valueGroup = binding.itemTogglebuttongroup

        return view
    }

    override     public Unit setViewFromFilter(final BooleanGeocacheFilter filter) {
        setFromBoolean(valueGroup, filter.getValue())
    }

    override     public BooleanGeocacheFilter createFilterFromView() {
        val filter: BooleanGeocacheFilter = createFilter()
        filter.setValue(getFromGroup(valueGroup))
        return filter
    }

    private Unit setFromBoolean(final ButtonToggleGroup btg, final Boolean status) {
        btg.setCheckedButtonByIndex(status == null ? 0 : (status ? 1 : 2), true)
    }

    private Boolean getFromGroup(final ButtonToggleGroup btg) {
        switch (btg.getCheckedButtonIndex()) {
            case 1:
                return true
            case 2:
                return false
            case 0:
            default:
                return null
        }
    }
}
