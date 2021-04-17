package cgeo.geocaching.filters.gui;

import cgeo.geocaching.R;
import cgeo.geocaching.databinding.CacheFilterCheckboxItemBinding;
import cgeo.geocaching.filters.core.OneOfManyGeocacheFilter;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.utils.functions.Func1;

import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.LinearLayout;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class OneOfManyFilterViewHolder<T, F extends OneOfManyGeocacheFilter<T>> extends BaseFilterViewHolder<F> {
    private final T[] values;
    private final CheckBox[] valueCheckboxes;
    private final Func1<T, Integer> drawableMapper;


    public OneOfManyFilterViewHolder(final T[] values) {
        this(values, null);
    }

    public OneOfManyFilterViewHolder(final T[] values, final Func1<T, Integer> drawableMapper) {
        this.values = values;
        this.valueCheckboxes = new CheckBox[values.length];
        this.drawableMapper = drawableMapper;
    }


    public View createView() {

        //get statistics
        final Map<T, Integer> stats = new HashMap<>();
        final boolean isFilled = FilterViewHolderUtils.isListInfoFilled();
        final boolean isCompete = !isFilled || FilterViewHolderUtils.isListInfoComplete();
        if (isFilled) {
            final F filter = (F) getType().create();
            for (Geocache cache : FilterViewHolderUtils.getListInfoFilteredList()) {
                final T cValue = filter.getValue(cache);
                if (stats.containsKey(cValue)) {
                    stats.put(cValue, stats.get(cValue) + 1);
                } else {
                    stats.put(cValue, 1);
                }
            }
        }

        final LinearLayout ll = new LinearLayout(getActivity());
        ll.setOrientation(LinearLayout.VERTICAL);
        int idx = 0;
        for (T value : values) {

            final String vText = value.toString() + (isFilled ? " (" + (stats.containsKey(value) ? "" + stats.get(value) : "0") + (isCompete ? "" : "+") + ")" : "");

            final View itemView = LayoutInflater.from(getActivity()).inflate(R.layout.cache_filter_checkbox_item, null);
            final CacheFilterCheckboxItemBinding itemBinding = CacheFilterCheckboxItemBinding.bind(itemView);
            itemBinding.itemText.setText(vText);
            final int iconId = this.drawableMapper == null ? -1 : this.drawableMapper.call(value);
            if (iconId > -1) {
                itemBinding.itemIcon.setImageResource(iconId);
            }
            ll.addView(itemView);
            this.valueCheckboxes[idx++] = itemBinding.itemCheckbox;
        }
        return ll;
    }

    @Override
    public void setViewFromFilter(final F filter) {
        final Set<T> set = filter.getValues();
        for (int i = 0; i < values.length; i++) {
            this.valueCheckboxes[i].setChecked(set.contains(this.values[i]));
        }
    }

    @Override
    public F createFilterFromView() {
        final F filter = createFilter();
        final Set<T> set = new HashSet<>();
        for (int i = 0; i < values.length; i++) {
            if (this.valueCheckboxes[i].isChecked()) {
                set.add(values[i]);
            }
        }
        filter.setValues(set);
        return filter;
    }
}
