package cgeo.geocaching.filters.gui;

import cgeo.geocaching.R;
import cgeo.geocaching.filters.core.IGeocacheFilter;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.ui.ImageParam;
import cgeo.geocaching.ui.ViewUtils;
import cgeo.geocaching.ui.dialog.Dialogs;

import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;

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

    private final ValueGroupFilterAccessor<T, F> filterAccessor;

    private final Set<T> visibleValues = new HashSet<>();
    private final Map<T, ImmutablePair<View, CheckBox>> valueCheckboxes = new HashMap<>();

    private ImmutablePair<View, CheckBox> selectAllNoneCheckbox;
    private boolean selectAllNoneBroadcast = true;
    private Button addItemsButton;

    private final int columnCount;
    private final boolean reduceToSelected;

    private List<LinearLayout> columns;
    private Map<T, Integer> statistics;
    private boolean statsAreComplete = false;

    public CheckboxFilterViewHolder(final ValueGroupFilterAccessor<T, F> filterAccessor) {
        this(filterAccessor, 1, false);
    }

    public CheckboxFilterViewHolder(final ValueGroupFilterAccessor<T, F> filterAccessor, final int colCount, final boolean reduceToSelected) {
        this.filterAccessor = filterAccessor;
        this.columnCount = colCount;
        this.reduceToSelected = reduceToSelected;
    }


    public View createView() {

        this.statistics = calculateStatistics();
        this.statsAreComplete = statistics == null || FilterViewHolderCreator.isListInfoComplete();


        final LinearLayout ll = new LinearLayout(getActivity());
        ll.setOrientation(LinearLayout.VERTICAL);

        //selectall/none
        ll.addView(ViewUtils.createColumnView(getActivity(), null, columnCount, false,  i -> {
            if (i < columnCount - 1) {
                return null;
            }
            return createSelectAllNoneView(ll);
        }));

        this.columns = ViewUtils.createAndAddStandardColumnView(getActivity(), null, ll, columnCount, true);
        if (!reduceToSelected) {
            visibleValues.addAll(filterAccessor.getSelectableValues());
        }

        //addItems
        ll.addView(createAddItemButton(ll));

        relayout();

        return ll;
    }

    private View createSelectAllNoneView(final ViewGroup ctx) {
        selectAllNoneCheckbox = ViewUtils.createCheckboxItem(getActivity(), ctx, getActivity().getString(R.string.cache_filter_checkboxlist_selectallnone), ImageParam.id(R.drawable.ic_menu_selectall), 0);

        selectAllNoneCheckbox.right.setOnCheckedChangeListener((v, c) -> {
            if (!selectAllNoneBroadcast) {
                return;
            }
            for (final ImmutablePair<View, CheckBox> cb : this.valueCheckboxes.values()) {
                cb.right.setChecked(c);
            }
        });
        return selectAllNoneCheckbox.left;
    }

    private View createAddItemButton(final ViewGroup vg) {
        this.addItemsButton = ViewUtils.createButton(getActivity(), vg, R.string.cache_filter_checkboxlist_add_items);

        this.addItemsButton.setOnClickListener(v -> {

            final List<T> items = new ArrayList<>(filterAccessor.getSelectableValues());
            items.removeAll(visibleValues);
            Dialogs.selectMultiple(getActivity(), items, filterAccessor::getDisplayText, null, R.string.cache_filter_checkboxlist_add_items_dialog_title, s -> {
                visibleValues.addAll(s);
                relayout();
            });
        });
        return this.addItemsButton;
    }

    private void relayout() {

        for (ViewGroup column : this.columns) {
            column.removeAllViews();
        }

        int idx = 0;
        for (T value : filterAccessor.getSelectableValuesAsArray()) {

            if (!this.visibleValues.contains(value)) {
                continue;
            }

            final ImmutablePair<View, CheckBox> cb = getValueCheckbox(value);
            columns.get(idx % columns.size()).addView(cb.left);

            idx++;
        }
        selectAllNoneCheckbox.left.setVisibility(this.visibleValues.size() > 1 ? View.VISIBLE : View.GONE);
        addItemsButton.setVisibility(this.reduceToSelected && this.visibleValues.size() < filterAccessor.getSelectableValues().size() ? View.VISIBLE : View.GONE);
        checkAndSetAllNoneValue();
    }

    @NonNull
    private ImmutablePair<View, CheckBox> getValueCheckbox(final T value) {
        ImmutablePair<View, CheckBox> cb = this.valueCheckboxes.get(value);
        if (cb == null) {
            final String vText = this.filterAccessor.getDisplayText(value) +
                (statistics != null ? " (" + (statistics.containsKey(value) ? "" + statistics.get(value) : "0") + (statsAreComplete ? "" : "+") + ")" : "");

            cb = ViewUtils.createCheckboxItem(getActivity(), columns.get(0), vText, this.filterAccessor.getIconFor(value), 0);
            cb.right.setChecked(true);
            this.valueCheckboxes.put(value, cb);
            cb.right.setOnCheckedChangeListener((v, c) -> checkAndSetAllNoneValue());
        }
        return cb;
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
        for (final ImmutablePair<View, CheckBox> cb : this.valueCheckboxes.values()) {
            if (!cb.right.isChecked()) {
                allChecked = false;
                break;
            }
        }
        //avoid that setting all/none-checkbox leads to setting other checkbox values here
        selectAllNoneBroadcast = false;
        selectAllNoneCheckbox.right.setChecked(allChecked);
        selectAllNoneBroadcast = true;
    }

    @Override
    public void setViewFromFilter(final F filter) {
        if (this.reduceToSelected) {
            this.visibleValues.clear();
        }
        final Collection<T> set = filterAccessor.getValues(filter);
        for (T value : filterAccessor.getSelectableValuesAsArray()) {
            if (!this.reduceToSelected || set.contains(value)) {
                final ImmutablePair<View, CheckBox> cb = getValueCheckbox(value);
                cb.right.setChecked(set.isEmpty() || set.contains(value));
                this.visibleValues.add(value);
            }
        }

        relayout();
    }

    @Override
    public F createFilterFromView() {
        final F filter = createFilter();
        final Set<T> set = new HashSet<>();
        if (getAllNoneSelected() == null) {
            for (T value : filterAccessor.getSelectableValuesAsArray()) {
                if (this.visibleValues.contains(value) && getValueCheckbox(value).right.isChecked()) {
                    set.add(value);
                }
            }
        }
        filterAccessor.setValues(filter, set);
        return filter;
    }

    private Boolean getAllNoneSelected() {
        boolean foundNonSelected = false;
        boolean foundSelected = false;
        for (T value : filterAccessor.getSelectableValuesAsArray()) {
            final boolean selected = this.visibleValues.contains(value) && getValueCheckbox(value).right.isChecked();
            foundNonSelected |= !selected;
            foundSelected |= selected;
        }
        if (foundNonSelected && !foundSelected) {
            return false;
        }
        if (!foundNonSelected && foundSelected) {
            return true;
        }
        return null;
    }
}
