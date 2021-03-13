package cgeo.geocaching.filters.gui;

import cgeo.geocaching.filters.core.OneOfManyGeocacheFilter;
import cgeo.geocaching.models.Geocache;

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

    public OneOfManyFilterViewHolder(final T[] values) {
        this.values = values;
        this.valueCheckboxes = new CheckBox[values.length];
    }


    public View createView() {

        //get statistics
        final Map<T, Integer> stats = new HashMap<>();
        final boolean isFilled = FilterViewHolderUtils.isListInfoFilled();
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

        final LinearLayout ll = new LinearLayout(getContext());
        ll.setOrientation(LinearLayout.VERTICAL);
        int idx = 0;
        for (T value : values) {
            final CheckBox c = new CheckBox(getContext());
            final String vText = value.toString() + (isFilled ? " (" + (stats.containsKey(value) ? "" + stats.get(value) : "0") + ")" : "");
            c.setText(vText);
            ll.addView(c);
            this.valueCheckboxes[idx++] = c;
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
