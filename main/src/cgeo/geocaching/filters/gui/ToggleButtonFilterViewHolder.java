package cgeo.geocaching.filters.gui;

import cgeo.geocaching.R;
import cgeo.geocaching.filters.core.IGeocacheFilter;
import cgeo.geocaching.settings.Settings;

import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import com.google.android.material.chip.ChipGroup;

public class ToggleButtonFilterViewHolder<T, F extends IGeocacheFilter> extends BaseFilterViewHolder<F> {

    private final ValueGroupFilterAccessor<T, F> filterAccessor;
    private final View[] valueButtons;
    private final boolean[] valueChecked;

    private View selectAllNoneButton;
    private boolean selectAllNoneChecked = true;

    public ToggleButtonFilterViewHolder(final ValueGroupFilterAccessor<T, F> filterAccessor) {
        this.filterAccessor = filterAccessor;
        this.valueButtons = new Button[filterAccessor.getSelectableValuesAsArray().length];
        this.valueChecked = new boolean[filterAccessor.getSelectableValuesAsArray().length];
    }

    private void toggleButtonState(final int idx) {
        setButtonState(idx, !getButtonState(idx));
    }

    private boolean getButtonState(final int idx) {
        return valueChecked[idx];
    }

    private void setButtonState(final int idx, final boolean checked) {
        valueChecked[idx] = checked;
        setButtonVisual(valueButtons[idx], checked);
    }

    private void setButtonVisual(final View button, final boolean checked) {
        if (checked) {
            button.setBackgroundColor(button.getResources().getColor(R.color.colorAccent));
        } else {
            button.setBackgroundColor(0x00000000);
            button.setBackgroundResource(Settings.isLightSkin() ? R.drawable.action_button_light : R.drawable.action_button_dark);
        }
    }

    private View createAddButton(final ChipGroup viewGroup, final String text) {
        final TextView button = new Button(getActivity(), null, R.style.button_small);
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
                setButtonVisual(selectAllNoneButton, selectAllNoneChecked);
                for (int idx = 0; idx < valueButtons.length; idx++) {
                    setButtonState(idx, selectAllNoneChecked);
                }
            });
        }

        int idx = 0;
        for (T value : filterAccessor.getSelectableValues()) {
            this.valueButtons[idx] = createAddButton(cg, filterAccessor.getDisplayText(value));
            setButtonState(idx, true);
            final int bIdx = idx;
            this.valueButtons[idx].setOnClickListener(v -> {
                toggleButtonState(bIdx);
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

    private int dpToPixel(final float dp)  {
        return (int) (dp * ((float) getActivity().getResources().getDisplayMetrics().density));
    }

    @Override
    public void setViewFromFilter(final F filter) {
        final Collection<T> set = filterAccessor.getValues(filter);
        for (int i = 0; i < filterAccessor.getSelectableValues().size(); i++) {
            setButtonState(i, set.contains(filterAccessor.getSelectableValuesAsArray()[i]));
        }
        checkAndSetAllNoneValue();
    }

    @Override
    public F createFilterFromView() {
        final F filter = createFilter();
        final Set<T> set = new HashSet<>();
        for (int i = 0; i < filterAccessor.getSelectableValues().size(); i++) {
            if (getButtonState(i)) {
                set.add(filterAccessor.getSelectableValuesAsArray()[i]);
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
            if (!getButtonState(idx)) {
                allChecked = false;
                break;
            }
        }
        selectAllNoneChecked = allChecked;
        setButtonVisual(selectAllNoneButton, selectAllNoneChecked);
    }

}
