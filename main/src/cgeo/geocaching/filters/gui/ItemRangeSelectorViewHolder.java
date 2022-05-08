package cgeo.geocaching.filters.gui;

import cgeo.geocaching.filters.core.IGeocacheFilter;
import cgeo.geocaching.ui.ItemRangeSlider;
import cgeo.geocaching.utils.functions.Func2;

import android.view.View;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.tuple.ImmutablePair;


public class ItemRangeSelectorViewHolder<T, F extends IGeocacheFilter> extends BaseFilterViewHolder<F> {

    private final ValueGroupFilterAccessor<T, F> filterAccessor;
    private final Map<T, Integer> itemsToPosition = new HashMap<>();
    private final Func2<Integer, T, String> axisLabelMapper;

    private ItemRangeSlider<T> slider;

    public ItemRangeSelectorViewHolder(final ValueGroupFilterAccessor<T, F> filterAccessor, final Func2<Integer, T, String> axisLabelMapper) {
        this.filterAccessor = filterAccessor;
        int idx = 0;
        for (T item : filterAccessor.getSelectableValuesAsArray()) {
            itemsToPosition.put(item, idx++);
        }
        this.axisLabelMapper = axisLabelMapper;
    }

    public void removeScaleLegend() {
        //remove legend
        slider.removeScaleLegend();
    }

    @Override
    public void setViewFromFilter(final F filter) {

        int min = filterAccessor.getSelectableValuesAsArray().length;
        int max = -1;
        for (T v : filterAccessor.getValues(filter)) {
            if (!itemsToPosition.containsKey(v)) {
                continue;
            }
            final int idx = itemsToPosition.get(v);
            min = Math.min(idx, min);
            max = Math.max(idx, max);
        }

        if (max < 0 || filterAccessor.getValues(filter).isEmpty()) {
            slider.setRangeAll();
        } else {
            slider.setRange(filterAccessor.getSelectableValuesAsArray()[min], filterAccessor.getSelectableValuesAsArray()[max]);
        }
    }

    @Override
    public View createView() {

        this.slider = new ItemRangeSlider<>(getActivity());
        slider.setScale(this.filterAccessor.getSelectableValues(), (i, v) -> this.filterAccessor.getDisplayText(v), this.axisLabelMapper);
        return slider;
    }

    @Override
    public F createFilterFromView() {
        final F filter = createFilter();
        final ImmutablePair<T, T> range = slider.getRange();
        final int minIdx = itemsToPosition.get(range.left);
        final int maxIdx = itemsToPosition.get(range.right);
        final Set<T> values = new HashSet<>();
        //if NO value is selected or complete range is selected -> don't set any range
        if (minIdx > 0 || maxIdx + 1 < filterAccessor.getSelectableValuesAsArray().length) {
            for (int i = minIdx; i <= maxIdx; i++) {
                values.add(filterAccessor.getSelectableValuesAsArray()[i]);
            }
        }
        filterAccessor.setValues(filter, values);
        return filter;
    }

}
