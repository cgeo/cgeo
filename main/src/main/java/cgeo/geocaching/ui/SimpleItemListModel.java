package cgeo.geocaching.ui;

import cgeo.geocaching.utils.CommonUtils;
import cgeo.geocaching.utils.functions.Action1;
import cgeo.geocaching.utils.functions.Func1;
import cgeo.geocaching.utils.functions.Func2;

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
    private Func2<T, Integer, TextParam> displayMapper = (o, i) -> TextParam.text(o == null ? "--" : o.toString());
    private Func2<T, Integer, ImageParam> displayIconMapper = (o, i) -> null;

    private Func2<T, Integer, ImageParam> actionIconMapper = (o, i) -> null;

    private ChoiceMode choiceMode = ChoiceMode.SINGLE_PLAIN;

    private int minimumItemCountForFilterDisplay = 5;

    private GroupingOptions<Object> groupingOptions = new GroupingOptions<>();


    private final Set<T> selectedItems = new HashSet<>();

    private final List<Action1<ChangeType>> changeListeners = new ArrayList<>();

    private Action1<T> actionListener = null;


    /** Supported display modes for choosing items from a list */
    public enum ChoiceMode { SINGLE_PLAIN, SINGLE_RADIO, MULTI_CHECKBOX }

    /** Types of model changes for which events are fired */
    public enum ChangeType { COMPLETE, SELECTION, SELECTION_BY_USER }

    /** Specifies options for grouping items */
    public class GroupingOptions<G> {

        private Func2<T, Integer, G> groupMapper = null;
        private Func1<G, TextParam> groupDisplayMapper = o -> TextParam.text("# " + (o == null ? "--" : o.toString())).setMarkdown(true);
        private Func1<G, ImageParam> groupDisplayIconMapper = o -> null;
        private Comparator<G> groupComparator = null;

        private GroupingOptions() {
            //no instances outside of this model class
        }

        public Func2<T, Integer, G> getGroupMapper() {
            return groupMapper;
        }

        /** Sets a mapper specifying how list items (of type T) are sorted into groups (of type G) */
        public GroupingOptions<G> setGroupMapper(final Func2<T, Integer, G> groupMapper) {
            this.groupMapper = groupMapper;
            triggerChange(ChangeType.COMPLETE);
            return this;
        }

        public Func1<G, TextParam> getGroupDisplayMapper() {
            return groupDisplayMapper;
        }

        /** Sets a mapper providing the text visualization for a group item */
        public GroupingOptions<G> setGroupDisplayMapper(final Func1<G, TextParam> groupDisplayMapper) {
            if (groupDisplayMapper != null) {
                this.groupDisplayMapper = groupDisplayMapper;
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

    public Func2<T, Integer, TextParam> getDisplayMapper() {
        return displayMapper;
    }

    /** Sets a providing the text visualization for an item */
    public SimpleItemListModel<T> setDisplayMapper(final Func2<T, Integer, TextParam> displayMapper) {
        if (displayMapper != null) {
            this.displayMapper = displayMapper;
            triggerChange(ChangeType.COMPLETE);
        }
        return this;
    }

    public Func2<T, Integer, ImageParam> getDisplayIconMapper() {
        return displayIconMapper;
    }

    /** Sets a apper providing an optional icon to visualize per item */
    public SimpleItemListModel<T> setDisplayIconMapper(final Func2<T, Integer, ImageParam> displayIconMapper) {
        if (displayIconMapper != null) {
            this.displayIconMapper = displayIconMapper;
            triggerChange(ChangeType.COMPLETE);
        }
        return this;
    }

    public Func2<T, Integer, ImageParam> getActionIconMapper() {
        return actionIconMapper;
    }

    /** Sets a mapper providing an optional action icon per item. For each item where the action item
     *  is non-null, it is displayed and the listener provided via {@link #setItemActionListener(Action1)} is fired */
    public SimpleItemListModel<T> setItemActionIconMapper(final Func2<T, Integer, ImageParam> actionIconMapper) {
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

    /** Sets Grouping options. Modify the passed GroupingOptions object in the Action. */
    @SuppressWarnings("unchecked")
    public <G> SimpleItemListModel<T> setGrouping(final Action1<GroupingOptions<G>> groupingOptions) {
        if (groupingOptions != null) {
            final GroupingOptions<G> go = this.new GroupingOptions<>();
            groupingOptions.call(go);
            this.groupingOptions = (GroupingOptions<Object>) go;
            triggerChange(ChangeType.COMPLETE);
        }
        return this;
    }

    public Func2<T, Integer, Object> getGroupMapper() {
        return this.groupingOptions.getGroupMapper();
    }

    public Func1<Object, TextParam> getGroupDisplayMapper() {
        return this.groupingOptions.getGroupDisplayMapper();
    }

    public Func1<Object, ImageParam> getGroupDisplayIconMapper() {
        return this.groupingOptions.getGroupDisplayIconMapper();
    }

    public Comparator<Object> getGroupComparator() {
        return this.groupingOptions.getGroupComparator();
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
