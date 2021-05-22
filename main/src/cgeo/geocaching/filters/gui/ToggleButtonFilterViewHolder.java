package cgeo.geocaching.filters.gui;

import cgeo.geocaching.R;
import cgeo.geocaching.filters.core.IGeocacheFilter;
import cgeo.geocaching.ui.ToggleButton;
import static cgeo.geocaching.ui.ViewUtils.dpToPixel;

import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import com.google.android.material.chip.ChipGroup;

public class ToggleButtonFilterViewHolder<T, F extends IGeocacheFilter> extends BaseFilterViewHolder<F> {

    private final ValueGroupFilterAccessor<T, F> filterAccessor;
    private final ToggleButton[] valueButtons;

    private ToggleButton selectAllNoneButton;
    private boolean selectAllNoneChecked = true;

    public ToggleButtonFilterViewHolder(final ValueGroupFilterAccessor<T, F> filterAccessor) {
        this.filterAccessor = filterAccessor;
        this.valueButtons = new ToggleButton[filterAccessor.getSelectableValuesAsArray().length];
    }

   private ToggleButton createAddButton(final ChipGroup viewGroup, final String text) {
        final ToggleButton button = new ToggleButton(getActivity());
        button.setText(text);
        button.setPadding(dpToPixel(10), dpToPixel(10), dpToPixel(10), dpToPixel(10));
        final ChipGroup.LayoutParams clp = new ChipGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        //lp.setMargins(dpToPixel(10), dpToPixel(10), dpToPixel(10), dpToPixel(10));
        viewGroup.addView(button, clp);
        return button;
    }


    public View createView() {

        final ChipGroup cg = new ChipGroup(getActivity());
        cg.setChipSpacing(dpToPixel(10));
        //ll.setOrientation(LinearLayout.HORIZONTAL);

        if (filterAccessor.getSelectableValues().size() > 1) {
            selectAllNoneButton = createAddButton(cg, getActivity().getString(R.string.cache_filter_checkboxlist_selectallnone));
            selectAllNoneButton.setOnClickListener(v -> {
                selectAllNoneChecked = !selectAllNoneChecked;
                selectAllNoneButton.setChecked(selectAllNoneChecked);
                for (ToggleButton tb : valueButtons) {
                    tb.setChecked(selectAllNoneChecked);
                }
            });
        }

        int idx = 0;
        for (T value : filterAccessor.getSelectableValues()) {
            this.valueButtons[idx] = new ToggleButton(getActivity());
            this.valueButtons[idx].setText(filterAccessor.getDisplayText(value));
            this.valueButtons[idx].setChecked(true);
            cg.addView(this.valueButtons[idx]);
            final int bIdx = idx;
            this.valueButtons[idx].setOnClickListener(v -> {
                this.valueButtons[bIdx].toggle();
                checkAndSetAllNoneValue();
            });
            idx++;
        }
        checkAndSetAllNoneValue();

        final LinearLayout ll = new LinearLayout(getActivity());
        ll.setOrientation(LinearLayout.VERTICAL);
        final LinearLayout.LayoutParams llp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        llp.setMargins(0, dpToPixel(20), 0, dpToPixel(20));
        ll.addView(cg, llp);

        return ll;
    }

    @Override
    public void setViewFromFilter(final F filter) {
        final Collection<T> set = filterAccessor.getValues(filter);
        for (int i = 0; i < filterAccessor.getSelectableValues().size(); i++) {
            valueButtons[i].setChecked(set.isEmpty() || set.contains(filterAccessor.getSelectableValuesAsArray()[i]));
        }
        checkAndSetAllNoneValue();
    }

    @Override
    public F createFilterFromView() {
        final F filter = createFilter();
        final Set<T> set = new HashSet<>();
        if (!selectAllNoneChecked) {
            for (int i = 0; i < filterAccessor.getSelectableValues().size(); i++) {
                if (valueButtons[i].isChecked()) {
                    set.add(filterAccessor.getSelectableValuesAsArray()[i]);
                }
            }
        }
        filterAccessor.setValues(filter, set);
        return filter;
    }

    private void checkAndSetAllNoneValue() {
        if (selectAllNoneButton == null) {
            return;
        }

        boolean allChecked = true;
        for (int idx = 0; idx < this.valueButtons.length; idx++) {
            if (!valueButtons[idx].isChecked()) {
                allChecked = false;
                break;
            }
        }
        selectAllNoneChecked = allChecked;
        selectAllNoneButton.setChecked(selectAllNoneChecked);
    }

}
