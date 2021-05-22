package cgeo.geocaching.filters.gui;

import cgeo.geocaching.R;
import cgeo.geocaching.filters.core.IGeocacheFilter;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.ui.ViewUtils;

import android.view.View;
import android.widget.CheckBox;
import android.widget.LinearLayout;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.jetbrains.annotations.Nullable;

public class CheckboxFilterViewHolder<T, F extends IGeocacheFilter> extends BaseFilterViewHolder<F> {

    private final ValueGroupFilterAccessor<T, ?, F> filterAccessor;
    private final CheckBox[] valueCheckboxes;

    private CheckBox selectAllNoneCheckbox;
    private boolean selectAllNoneBroadcast = true;

    private final int columnCount;

    public CheckboxFilterViewHolder(final ValueGroupFilterAccessor<T, ?, F> filterAccessor) {
        this(filterAccessor, 1);
    }

    public CheckboxFilterViewHolder(final ValueGroupFilterAccessor<T, ?, F> filterAccessor, final int colCount) {
        this.filterAccessor = filterAccessor;
        this.valueCheckboxes = new CheckBox[filterAccessor.getSelectableValuesAsArray().length];
        this.columnCount = colCount;
    }


    public View createView() {

        final Map<T, Integer> stats = calculateStatistics();
        final boolean showStatistics = stats != null;
        final boolean statsAreComplete = !showStatistics || FilterViewHolderCreator.isListInfoComplete();


        final LinearLayout ll = new LinearLayout(getActivity());
        ll.setOrientation(LinearLayout.VERTICAL);

        final List<Float> columnWidths = new ArrayList<>();
        for (int c = 0 ; c < columnCount * 2 - 1; c++) {
            columnWidths.add(c % 2 == 0 ? 1f : 0.1f);
        }

        //selectall/none
        if (filterAccessor.getSelectableValuesAsArray().length > 1) {

            ll.addView(ViewUtils.createHorizontallyDistributedViews(getActivity(), null, columnWidths, (i, f) -> {
                if (i < columnWidths.size() - 1) {
                    return null;
                }
                final ImmutablePair<View, CheckBox> ip = ViewUtils.createCheckboxItem(getActivity(), ll, getActivity().getString(R.string.cache_filter_checkboxlist_selectallnone), R.drawable.ic_menu_selectall, 0);
                selectAllNoneCheckbox = ip.right;
                selectAllNoneCheckbox.setOnCheckedChangeListener((v, c) -> {
                    if (!selectAllNoneBroadcast) {
                        return;
                    }
                    for (final CheckBox cb : this.valueCheckboxes) {
                        cb.setChecked(c);
                    }
                });
                return ip.left;
            }, (i, f) -> f));
        }

        final List<LinearLayout> columns = new ArrayList<>();
        ll.addView(ViewUtils.createHorizontallyDistributedViews(getActivity(), null, columnWidths, (i, f) -> {
            if (i % 2 == 1) {
                //column separator
                return ViewUtils.createVerticalSeparator(getActivity());
            }
            final LinearLayout colLl = new LinearLayout(getActivity());
            columns.add(colLl);
            colLl.setOrientation(LinearLayout.VERTICAL);
            return colLl;
        }, (i, f) -> f));


        int idx = 0;
        for (T value : filterAccessor.getSelectableValuesAsArray()) {

            final String vText = this.filterAccessor.getDisplayText(value) + (showStatistics ? " (" + (stats.containsKey(value) ? "" + stats.get(value) : "0") + (statsAreComplete ? "" : "+") + ")" : "");
            this.valueCheckboxes[idx] = ViewUtils.addCheckboxItem(getActivity(), columns.get(idx % columns.size()), vText, this.filterAccessor.getIconFor(value), 0);
            this.valueCheckboxes[idx].setChecked(true);
            if (selectAllNoneCheckbox != null) {
                this.valueCheckboxes[idx].setOnCheckedChangeListener((v, c) -> {
                    checkAndSetAllNoneValue();
                });
            }
            idx++;
        }
        checkAndSetAllNoneValue();
        return ll;
    }

    @Nullable
    private Map<T, Integer> calculateStatistics() {
        Map<T, Integer> stats = null;
        if (FilterViewHolderCreator.isListInfoFilled() && filterAccessor.hasCacheValueGetter()) {
            stats = new HashMap<>();
            final F filter = createFilter();
            for (Geocache cache : FilterViewHolderCreator.getListInfoFilteredList()) {
                final Set<T> cValues = filterAccessor.getCacheValues(filter, cache);
                for (T cValue : cValues) {
                    if (stats.containsKey(cValue)) {
                        stats.put(cValue, stats.get(cValue) + 1);
                    } else {
                        stats.put(cValue, 1);
                    }
                }
            }
        }
        return stats;
    }

    private void checkAndSetAllNoneValue() {
        if (selectAllNoneCheckbox == null) {
            return;
        }

        boolean allChecked = true;
        for (final CheckBox cb : this.valueCheckboxes) {
            if (!cb.isChecked()) {
                allChecked = false;
                break;
            }
        }
        //avoid that setting all/none-checkbox leads to setting other checkbox values here
        selectAllNoneBroadcast = false;
        selectAllNoneCheckbox.setChecked(allChecked);
        selectAllNoneBroadcast = true;
    }

    @Override
    public void setViewFromFilter(final F filter) {
        final Collection<T> set = filterAccessor.getValues(filter);
        for (int i = 0; i < filterAccessor.getSelectableValuesAsArray().length; i++) {
            this.valueCheckboxes[i].setChecked(set.contains(filterAccessor.getSelectableValuesAsArray()[i]));
        }
    }

    @Override
    public F createFilterFromView() {
        final F filter = createFilter();
        final Set<T> set = new HashSet<>();
        for (int i = 0; i < filterAccessor.getSelectableValuesAsArray().length; i++) {
            if (this.valueCheckboxes[i].isChecked()) {
                set.add(filterAccessor.getSelectableValuesAsArray()[i]);
            }
        }
        filterAccessor.setValues(filter, set);
        return filter;
    }
}
