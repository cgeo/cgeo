package cgeo.geocaching.filters.gui;

import cgeo.geocaching.filters.core.IGeocacheFilter;
import cgeo.geocaching.ui.ChipChoiceGroup;
import cgeo.geocaching.ui.TextParam;
import cgeo.geocaching.ui.ViewUtils;
import cgeo.geocaching.utils.CollectionStream;
import static cgeo.geocaching.ui.ViewUtils.dpToPixel;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class ChipChoiceFilterViewHolder<T, F extends IGeocacheFilter> extends BaseFilterViewHolder<F> {

    private final ValueGroupFilterAccessor<T, F> filterAccessor;
    private ChipChoiceGroup chipChoiceGroup;

    public ChipChoiceFilterViewHolder(final ValueGroupFilterAccessor<T, F> filterAccessor) {
        this.filterAccessor = filterAccessor;

    }

    public View createView() {

        final Context ctx = ViewUtils.wrap(getActivity());
        this.chipChoiceGroup = new ChipChoiceGroup(ctx);
        this.chipChoiceGroup.setChipSpacing(dpToPixel(10));
        this.chipChoiceGroup.addChips(CollectionStream.of(filterAccessor.getSelectableValues()).map(v -> TextParam.text(filterAccessor.getDisplayText(v))).toList());

        final LinearLayout ll = new LinearLayout(getActivity());
        ll.setOrientation(LinearLayout.VERTICAL);
        final LinearLayout.LayoutParams llp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        llp.setMargins(0, dpToPixel(20), 0, dpToPixel(20));
        ll.addView(this.chipChoiceGroup, llp);

        return ll;
    }

    @Override
    public void setViewFromFilter(final F filter) {
        final Collection<T> set = filterAccessor.getValues(filter);
        for (int i = 0; i < filterAccessor.getSelectableValues().size(); i++) {
            this.chipChoiceGroup.setCheckedButtonByIndex(set.isEmpty() || set.contains(filterAccessor.getSelectableValuesAsArray()[i]), i);
        }
    }

    @Override
    public F createFilterFromView() {
        final F filter = createFilter();
        final Set<T> set = new HashSet<>();
        if (!chipChoiceGroup.allChecked() && !chipChoiceGroup.noneChecked()) {
            for (int checked : this.chipChoiceGroup.getCheckedButtonIndexes()) {
                set.add(filterAccessor.getSelectableValuesAsArray()[checked]);
            }
        }
        filterAccessor.setValues(filter, set);
        return filter;
    }

}
