package cgeo.geocaching.filters.gui;

import cgeo.geocaching.R;
import cgeo.geocaching.databinding.ButtontogglegroupLabeledItemBinding;
import cgeo.geocaching.filters.core.BooleanGeocacheFilter;
import cgeo.geocaching.ui.ButtonToggleGroup;
import cgeo.geocaching.ui.ImageParam;

import android.view.View;

import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

public class BooleanFilterViewHolder extends BaseFilterViewHolder<BooleanGeocacheFilter> {

    private ButtonToggleGroup valueGroup = null;

    @StringRes
    public final int labelId;
    public final ImageParam icon;


    public BooleanFilterViewHolder(final int labelId, @Nullable final ImageParam icon) {
        this.labelId = labelId;
        this.icon = icon;
    }

    @Override
    public View createView() {

        final View view = inflateLayout(R.layout.buttontogglegroup_labeled_item);
        final ButtontogglegroupLabeledItemBinding binding = ButtontogglegroupLabeledItemBinding.bind(view);
        binding.itemText.setText(labelId);
        if (icon != null) {
            icon.applyTo(binding.itemIcon);
        }

        binding.itemTogglebuttongroup.addButtons(R.string.cache_filter_status_select_all, R.string.cache_filter_status_select_yes, R.string.cache_filter_status_select_no);

        valueGroup = binding.itemTogglebuttongroup;

        return view;
    }

    @Override
    public void setViewFromFilter(final BooleanGeocacheFilter filter) {
        setFromBoolean(valueGroup, filter.getValue());
    }

    @Override
    public BooleanGeocacheFilter createFilterFromView() {
        final BooleanGeocacheFilter filter = createFilter();
        filter.setValue(getFromGroup(valueGroup));
        return filter;
    }

    private void setFromBoolean(final ButtonToggleGroup btg, final Boolean status) {
        btg.setCheckedButtonByIndex(status == null ? 0 : (status ? 1 : 2), true);
    }

    private Boolean getFromGroup(final ButtonToggleGroup btg) {
        switch (btg.getCheckedButtonIndex()) {
            case 1:
                return true;
            case 2:
                return false;
            case 0:
            default:
                return null;
        }
    }
}
