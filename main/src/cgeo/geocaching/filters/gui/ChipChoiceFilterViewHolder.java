package cgeo.geocaching.filters.gui;

import cgeo.geocaching.R;
import cgeo.geocaching.filters.core.IGeocacheFilter;
import cgeo.geocaching.ui.ViewUtils;
import static cgeo.geocaching.ui.ViewUtils.dpToPixel;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.LinearLayout;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import com.google.android.material.chip.ChipGroup;

public class ChipChoiceFilterViewHolder<T, F extends IGeocacheFilter> extends BaseFilterViewHolder<F> {

    private final ValueGroupFilterAccessor<T, F> filterAccessor;
    private final CompoundButton[] valueButtons;

    private CompoundButton selectAllNoneButton;
    private boolean userInteraction = true;

    public ChipChoiceFilterViewHolder(final ValueGroupFilterAccessor<T, F> filterAccessor) {
        this.filterAccessor = filterAccessor;
        this.valueButtons = new CompoundButton[filterAccessor.getSelectableValuesAsArray().length];
    }

    private CompoundButton createAddButton(final LayoutInflater inflater, final ChipGroup viewGroup, final String text) {

        final CompoundButton button = (CompoundButton) inflater.inflate(R.layout.chip_choice_view, viewGroup, false);
        button.setText(text);
        final ChipGroup.LayoutParams clp = new ChipGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        viewGroup.addView(button, clp);
        return button;
    }


    public View createView() {

        final Context ctx = ViewUtils.wrap(getActivity());
        final LayoutInflater inflater = LayoutInflater.from(ctx);

        final ChipGroup cg = new ChipGroup(ctx);
        cg.setChipSpacing(dpToPixel(10));

        if (filterAccessor.getSelectableValues().size() > 1) {
            selectAllNoneButton = createAddButton(inflater, cg, getActivity().getString(R.string.cache_filter_checkboxlist_selectallnone));
            selectAllNoneButton.setOnCheckedChangeListener((v, c) -> {
                if (!userInteraction) {
                    return;
                }
                for (CompoundButton tb : valueButtons) {
                    tb.setChecked(c);
                }
            });
        }

        int idx = 0;
        for (T value : filterAccessor.getSelectableValues()) {
            this.valueButtons[idx] = createAddButton(inflater, cg, filterAccessor.getDisplayText(value));
            this.valueButtons[idx].setChecked(true);
            this.valueButtons[idx].setOnCheckedChangeListener((v, c) -> checkAndSetAllNoneValue());
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
        if (selectAllNoneButton == null || !selectAllNoneButton.isChecked()) {
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
        for (CompoundButton valueButton : this.valueButtons) {
            if (!valueButton.isChecked()) {
                allChecked = false;
                break;
            }
        }

        userInteraction = false;
        selectAllNoneButton.setChecked(allChecked);
        userInteraction = true;
    }

}
