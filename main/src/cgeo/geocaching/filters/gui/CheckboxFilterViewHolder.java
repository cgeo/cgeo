package cgeo.geocaching.filters.gui;

import cgeo.geocaching.R;
import cgeo.geocaching.filters.core.IGeocacheFilter;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.ui.ImageParam;
import cgeo.geocaching.ui.TextParam;
import cgeo.geocaching.ui.ViewUtils;
import cgeo.geocaching.ui.dialog.SimpleDialog;

import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.Space;

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
    private Button addAllItemsButton;

    private final int columnCount;
    private Set<T> alwaysVisibleItems = null;

    private List<LinearLayout> columns;
    private Map<T, Integer> statistics;
    private boolean statsAreComplete = false;

    public CheckboxFilterViewHolder(final ValueGroupFilterAccessor<T, F> filterAccessor) {
        this(filterAccessor, 1, null);
    }

    public CheckboxFilterViewHolder(final ValueGroupFilterAccessor<T, F> filterAccessor, final int colCount, final Set<T> alwaysVisibleItems) {
        this.filterAccessor = filterAccessor;
        this.columnCount = colCount;
        if (alwaysVisibleItems != null) {
            this.alwaysVisibleItems = new HashSet<>();
            this.alwaysVisibleItems.addAll(alwaysVisibleItems);
        }
    }


    public View createView() {

        this.statistics = calculateStatistics();
        this.statsAreComplete = statistics == null || FilterViewHolderCreator.isListInfoComplete();


        final LinearLayout ll = new LinearLayout(getActivity());
        ll.setOrientation(LinearLayout.VERTICAL);

        //selectall/none
        ll.addView(ViewUtils.createColumnView(getActivity(), null, columnCount, false, i -> {
            if (i < columnCount - 1) {
                return null;
            }
            return createSelectAllNoneView(ll);
        }));

        this.columns = ViewUtils.createAndAddStandardColumnView(getActivity(), null, ll, columnCount, true);

        this.visibleValues.clear();
        this.visibleValues.addAll(filterAccessor.getSelectableValues());
        if (this.alwaysVisibleItems != null) {
            this.visibleValues.retainAll(this.alwaysVisibleItems);
        }

        //addItems / addallItems
        final LinearLayout llButtons = new LinearLayout(getActivity());
        llButtons.setOrientation(LinearLayout.HORIZONTAL);
        llButtons.addView(createAddItemButton(ll));
        final Space space = new Space(getActivity());
        space.setMinimumWidth(ViewUtils.dpToPixel(20));
        llButtons.addView(space);
        llButtons.addView(createAddAllItemsButton(ll));
        ll.addView(llButtons);

        relayout();

        return ll;
    }

    private View createSelectAllNoneView(final ViewGroup ctx) {
        selectAllNoneCheckbox = ViewUtils.createCheckboxItem(getActivity(), ctx, TextParam.id(R.string.cache_filter_checkboxlist_selectallnone), ImageParam.id(R.drawable.ic_menu_selectall), null);

        selectAllNoneCheckbox.right.setOnCheckedChangeListener((v, c) -> {
            if (!selectAllNoneBroadcast) {
                return;
            }
            for (final T value : visibleValues) {
                getValueCheckbox(value).right.setChecked(c);
            }
        });
        return selectAllNoneCheckbox.left;
    }

    private View createAddItemButton(final ViewGroup vg) {
        this.addItemsButton = ViewUtils.createButton(getActivity(), vg, TextParam.id(R.string.cache_filter_checkboxlist_add_items));

        this.addItemsButton.setOnClickListener(v -> {

            final List<T> items = new ArrayList<>(filterAccessor.getSelectableValues());
            items.removeAll(visibleValues);
            SimpleDialog.of(getActivity()).setTitle(TextParam.id(R.string.cache_filter_checkboxlist_add_items_dialog_title)).selectMultiple(items, (s, i) -> TextParam.text(filterAccessor.getDisplayText(s)), null, s -> {
                visibleValues.addAll(s);
                for (T value : s) {
                    getValueCheckbox(value).right.setChecked(true);
                }
                relayout();
            });
        });

        return this.addItemsButton;
    }

    private View createAddAllItemsButton(final ViewGroup vg) {
        this.addAllItemsButton = ViewUtils.createButton(getActivity(), vg, TextParam.id(R.string.cache_filter_checkboxlist_add_all_items));

        this.addAllItemsButton.setOnClickListener(v -> {
            visibleValues.addAll(filterAccessor.getSelectableValues());
            relayout();
        });

        return this.addAllItemsButton;
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
        addItemsButton.setVisibility(this.visibleValues.size() < filterAccessor.getSelectableValues().size() ? View.VISIBLE : View.GONE);
        addAllItemsButton.setVisibility(this.visibleValues.size() < filterAccessor.getSelectableValues().size() ? View.VISIBLE : View.GONE);
        checkAndSetAllNoneValue();
    }

    @NonNull
    private ImmutablePair<View, CheckBox> getValueCheckbox(final T value) {
        ImmutablePair<View, CheckBox> cb = this.valueCheckboxes.get(value);
        if (cb == null) {
            final String vText = this.filterAccessor.getDisplayText(value) +
                    (statistics != null ? " (" + (statistics.containsKey(value) ? "" + statistics.get(value) : "0") + (statsAreComplete ? "" : "+") + ")" : "");

            cb = ViewUtils.createCheckboxItem(getActivity(), columns.get(0), TextParam.text(vText), this.filterAccessor.getIconFor(value), null);
            cb.right.setChecked(this.alwaysVisibleItems == null);
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
                        final Integer cnt = stats.get(cValue);
                        stats.put(cValue, cnt == null ? 1 : cnt + 1);
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
        for (final T value : visibleValues) {
            final ImmutablePair<View, CheckBox> cb = getValueCheckbox(value);
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
        this.visibleValues.clear();
        final Collection<T> set = filterAccessor.getValues(filter);
        final boolean setCheckedAll = set.isEmpty() && this.alwaysVisibleItems == null;
        for (T value : filterAccessor.getSelectableValuesAsArray()) {
            if (set.contains(value) || this.alwaysVisibleItems == null || this.alwaysVisibleItems.contains(value)) {
                this.visibleValues.add(value);
            }
            final ImmutablePair<View, CheckBox> cb = getValueCheckbox(value);
            cb.right.setChecked(setCheckedAll || set.contains(value));
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
