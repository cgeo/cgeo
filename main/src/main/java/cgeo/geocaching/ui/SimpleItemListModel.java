package cgeo.geocaching.ui;

import cgeo.geocaching.R;
import cgeo.geocaching.utils.CommonUtils;
import cgeo.geocaching.utils.functions.Action1;
import cgeo.geocaching.utils.functions.Func1;
import cgeo.geocaching.utils.functions.Func4;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Model class to work with {@link cgeo.geocaching.ui.SimpleItemListView} */
public class SimpleItemListModel<T> {

    private final List<T> items = new ArrayList<>();
    private final List<T> itemsReadOnly = Collections.unmodifiableList(items);

    private Func4<T, Context, View, ViewGroup, View> displayViewMapper = (item, context, view, parent) -> null;
    private Func1<T, String> textFilterMapper = null;
    private Func1<T, ImageParam> displayIconMapper = (o) -> null;

    private Func1<T, ImageParam> actionIconMapper = (o) -> null;

    private ChoiceMode choiceMode = ChoiceMode.SINGLE_PLAIN;

    private int minimumItemCountForFilterDisplay = 5;

    private int[] itemPaddingInDp = new int[]{10, 4, 10, 4};

    private int plainItemPaddingLeftInDp = 10;

    private final GroupingOptions<Object> groupingOptions = new GroupingOptions<>();


    private final Set<T> selectedItems = new HashSet<>();

    private final List<Action1<ChangeType>> changeListeners = new ArrayList<>();

    private Action1<T> actionListener = null;


    /** Supported display modes for choosing items from a list */
    public enum ChoiceMode { SINGLE_PLAIN, SINGLE_RADIO, MULTI_CHECKBOX }

    /** Types of model changes for which events are fired */
    public enum ChangeType { COMPLETE, SELECTION, SELECTION_BY_USER }

    /** Specifies options for grouping items */
    public class GroupingOptions<G> {

        private Func1<T, G> groupMapper = null;

        private Func4<G, Context, View, ViewGroup, View> groupDisplayViewMapper = (item, context, view, parent) -> null;
        private Func1<G, ImageParam> groupDisplayIconMapper = o -> null;
        private Comparator<G> groupComparator = null;

        private int minActivationGroupCount = 2;

        private GroupingOptions() {
            //no instances outside of this model class
        }

        public Func1<T, G> getGroupMapper() {
            return groupMapper;
        }

        /** Sets a mapper specifying how list items (of type T) are sorted into groups (of type G) */
        public GroupingOptions<G> setGroupMapper(final Func1<T, G> groupMapper) {
            this.groupMapper = groupMapper;
            triggerChange(ChangeType.COMPLETE);
            return this;
        }

        public Func4<G, Context, View, ViewGroup, View> getGroupDisplayViewMapper() {
            return groupDisplayViewMapper;
        }

        /** Sets a mapper providing the text visualization for a group item */
        public GroupingOptions<G> setGroupDisplayMapper(final Func1<G, TextParam> groupDisplayMapper) {
            if (groupDisplayMapper != null) {
                setGroupDisplayViewMapper(constructDisplayViewMapper(groupDisplayMapper));
                triggerChange(ChangeType.COMPLETE);
            }
            return this;
        }

        public GroupingOptions<G> setGroupDisplayViewMapper(final Func4<G, Context, View, ViewGroup, View> groupDisplayViewMapper) {
            if (groupDisplayViewMapper != null) {
                this.groupDisplayViewMapper = groupDisplayViewMapper;
                triggerChange(ChangeType.COMPLETE);
            }
            return this;
        }

        public Func1<G, ImageParam> getGroupDisplayIconMapper() {
            return groupDisplayIconMapper;
        }

        /** Sets a mapper providing an optional display icon per group */
        public GroupingOptions<G> setGroupDisplayIconMapper(final Func1<G, ImageParam> groupDisplayIconMapper) {
            if (groupDisplayIconMapper != null) {
                this.groupDisplayIconMapper = groupDisplayIconMapper;
                triggerChange(ChangeType.COMPLETE);
            }
            return this;
        }

        public Comparator<G> getGroupComparator() {
            return groupComparator;
        }

        /** Sets a comparator specifying how groups are sorted */
        public GroupingOptions<G> setGroupComparator(final Comparator<G> groupComparator) {
            this.groupComparator = groupComparator;
            triggerChange(ChangeType.COMPLETE);
            return this;
        }

        public int getMinActivationGroupCount() {
            return minActivationGroupCount;
        }

        /** Sets the  minimum number of groups on which grouping is really activated */
        public GroupingOptions<G> setMinActivationGroupCount(final int minActivationGroupCount) {
            this.minActivationGroupCount = minActivationGroupCount;
            triggerChange(ChangeType.COMPLETE);
            return this;
        }
    }

    public List<T> getItems() {
        return itemsReadOnly;
    }

    /** Sets the items to show */
    public SimpleItemListModel<T> setItems(final Iterable<T> items) {
        if (items != null) {
            this.items.clear();
            for (T item : items) {
                this.items.add(item);
            }
            triggerChange(ChangeType.COMPLETE);
        }
        return this;
    }

    public Func4<T, Context, View, ViewGroup, View> getDisplayViewMapper() {
        return displayViewMapper;
    }

    public Func1<T, String> getTextFilterMapper() {
        return textFilterMapper;
    }

    /** Sets a providing the text visualization for an item */
    public SimpleItemListModel<T> setDisplayMapper(final Func1<T, TextParam> displayMapper) {
        if (displayMapper != null) {
            setDisplayViewMapper(constructDisplayViewMapper(displayMapper), constructFilterTextExtractor(displayMapper));
        }
        return this;
    }

    public static <TT> Func4<TT, Context, View, ViewGroup, View> constructDisplayViewMapper(final Func1<TT, TextParam> displayTextMapper) {
        return (item, context, view, parent) -> {
            final TextView tv = view instanceof TextView ? (TextView) view : ViewUtils.createTextItem(context, R.style.text_default, TextParam.text(""));
            final TextParam tp = displayTextMapper == null ? TextParam.text(String.valueOf(item)) : displayTextMapper.call(item);
            if (tp == null) {
                tv.setText("--");
            } else {
                tp.applyTo(tv);
            }
            return tv;
        };
    }

    public static <T> Func1<T, String> constructFilterTextExtractor(final Func1<T, TextParam> displayTextMapper) {
        return (item) -> {

            final TextParam tp = displayTextMapper == null ? null : displayTextMapper.call(item);
            if (tp == null) {
                return String.valueOf(item);
            }
            return String.valueOf(tp.getText(null));
        };
    }

    /** Sets a view provider for the items */
    public SimpleItemListModel<T> setDisplayViewMapper(final Func4<T, Context, View, ViewGroup, View> displayViewMapper, final Func1<T, String> textFilterMapper) {
        if (displayViewMapper != null) {
            this.displayViewMapper = displayViewMapper;
            this.textFilterMapper = textFilterMapper;
            triggerChange(ChangeType.COMPLETE);
        }
        return this;
    }

    public Func1<T, ImageParam> getDisplayIconMapper() {
        return displayIconMapper;
    }

    /** Sets a apper providing an optional icon to visualize per item */
    public SimpleItemListModel<T> setDisplayIconMapper(final Func1<T, ImageParam> displayIconMapper) {
        if (displayIconMapper != null) {
            this.displayIconMapper = displayIconMapper;
            triggerChange(ChangeType.COMPLETE);
        }
        return this;
    }

    public Func1<T, ImageParam> getActionIconMapper() {
        return actionIconMapper;
    }

    /** Sets a mapper providing an optional action icon per item. For each item where the action item
     *  is non-null, it is displayed and the listener provided via {@link #setItemActionListener(Action1)} is fired */
    public SimpleItemListModel<T> setItemActionIconMapper(final Func1<T, ImageParam> actionIconMapper) {
        if (actionIconMapper != null) {
            this.actionIconMapper = actionIconMapper;
            triggerChange(ChangeType.COMPLETE);
        }
        return this;
    }

    public ChoiceMode getChoiceMode() {
        return choiceMode;
    }

    /** Sets the (visual) choice mode to use. For single-choice modes, the model ensures that max.
     *  one item is selected at any time.
     */
    public SimpleItemListModel<T> setChoiceMode(final ChoiceMode choiceMode) {
        if (choiceMode != null) {
            this.choiceMode = choiceMode;
            pruneSelected();
            triggerChange(ChangeType.COMPLETE);
        }
        return this;
    }

    private void pruneSelected() {
        if (choiceMode != ChoiceMode.MULTI_CHECKBOX && selectedItems.size() > 1) {
            final T remaining = CommonUtils.first(selectedItems);
            selectedItems.clear();
            selectedItems.add(remaining);
        }
    }

    public int getMinimumItemCountForFilterDisplay() {
        return minimumItemCountForFilterDisplay;
    }

    /** Sets the minumum count of items to display a textual filter */
    public SimpleItemListModel<T> setMinimumItemCountForFilterDisplay(final int minimumItemCountForFilterDisplay) {
        if (minimumItemCountForFilterDisplay != this.minimumItemCountForFilterDisplay) {
            this.minimumItemCountForFilterDisplay = minimumItemCountForFilterDisplay;
            triggerChange(ChangeType.COMPLETE);
        }
        return this;
    }

    /** Sets padding (in DP) to apply to each list item. Usage is described in {@link ViewUtils#applyPadding(View, int[])}*/
    public SimpleItemListModel<T> setItemPadding(final int ... itemPaddingsInDp) {
        this.itemPaddingInDp = itemPaddingsInDp;
        triggerChange(ChangeType.COMPLETE);
        return this;
    }

    public int[] getItemPaddingInDp() {
        return this.itemPaddingInDp;
    }

    public int getPlainItemPaddingLeftInDp() {
        return plainItemPaddingLeftInDp;
    }

    public SimpleItemListModel<T> setPlainItemPaddingLeftInDp(final int plainItemPaddingLeftInDp) {
        this.plainItemPaddingLeftInDp = plainItemPaddingLeftInDp;
        triggerChange(ChangeType.COMPLETE);
        return this;
    }

    /** Activates Grouping for this model with given groupMapper. Returns a GroupingOptions instance to set further grouping settings. */
    @SuppressWarnings("unchecked")
    public <G> GroupingOptions<G> activateGrouping(final Func1<T, G> groupMapper) {
        this.groupingOptions.setGroupMapper((Func1<T, Object>) groupMapper);
        return (GroupingOptions<G>) this.groupingOptions;
    }

    @NonNull
    public GroupingOptions<Object> getGroupingOptions() {
        return this.groupingOptions;
    }

    /** Sets the currently selected items */
    public SimpleItemListModel<T> setSelectedItems(final Iterable<T> selected) {
        return setSelectedItems(selected, false);
    }

    public SimpleItemListModel<T> setSelectedItems(final Iterable<T> selected, final boolean byUser) {
        if (selected != null) {
            selectedItems.clear();
            for (T sel : selected) {
                selectedItems.add(sel);
            }
            pruneSelected();
            triggerChange(byUser ? ChangeType.SELECTION_BY_USER : ChangeType.SELECTION);
        }
        return this;
    }

    /** Gets currently selected items. If ChoiceMode is a SINGLE mode then the returned
     *  collection will always have 0 or 1 element only. */
    public Set<T> getSelectedItems() {
        return selectedItems;
    }

    /** Adds a model change listener */
    public void addChangeListeners(final Action1<ChangeType> changeListeners) {
        this.changeListeners.add(changeListeners);
    }

    /** The listener provided here will be fired for items where an action icon is set if user clicks this icon */
    public SimpleItemListModel<T> setItemActionListener(final Action1<T> actionListener) {
        this.actionListener = actionListener;
        return this;
    }

    public Action1<T> getActionListener() {
        return this.actionListener;
    }

    private void triggerChange(final ChangeType mode) {
        for (Action1<ChangeType> changeListener : this.changeListeners) {
            changeListener.call(mode);
        }
    }
}
