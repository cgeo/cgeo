package cgeo.geocaching.ui;

import cgeo.geocaching.R;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.utils.CommonUtils;
import cgeo.geocaching.utils.JsonUtils;
import cgeo.geocaching.utils.functions.Func2;
import cgeo.geocaching.utils.functions.Func4;

import android.content.Context;
import android.util.Pair;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/** Model class to work with {@link cgeo.geocaching.ui.SimpleItemListView} */
public class SimpleItemListModel<T> {

    private final Function<T, TextParam> defaultDisplayMapper = obj -> TextParam.text(obj == null ? "-" : obj.toString());

    private final List<T> items = new ArrayList<>();
    private final List<T> itemsReadOnly = Collections.unmodifiableList(items);

    private Func4<T, Context, View, ViewGroup, View> displayViewMapper = constructDisplayViewMapper(defaultDisplayMapper, null);
    private Function<T, String> textFilterMapper = constructFilterTextExtractor(defaultDisplayMapper);
    private Function<T, ImageParam> displayIconMapper = (o) -> null;

    private Function<T, ImageParam> actionIconMapper = (o) -> null;

    private ChoiceMode choiceMode = ChoiceMode.SINGLE_PLAIN;

    private int[] itemPaddingInDp = new int[]{10, 4, 10, 4};

    private final GroupingOptions<Object> groupingOptions = new GroupingOptions<>();

    private String filterTerm = null;

    private T scrollAnchorOnOpen = null;


    private final Set<T> selectedItems = new HashSet<>();

    private final List<Consumer<ChangeType>> changeListeners = new ArrayList<>();

    private Consumer<T> actionListener = null;


    /** Supported display modes for choosing items from a list */
    public enum ChoiceMode { SINGLE_PLAIN, SINGLE_RADIO, MULTI_CHECKBOX }

    /** Types of model changes for which events are fired */
    public enum ChangeType { COMPLETE, SELECTION, FILTER, GROUP_HEADER }

    /** Specifies options for grouping items */
    public class GroupingOptions<G> {

        private Function<T, G> groupMapper = null;

        private Func4<Pair<G, List<T>>, Context, View, ViewGroup, View> groupDisplayViewMapper = (item, context, view, parent) -> null;
        private Func2<G, List<T>, ImageParam> groupDisplayIconMapper = (o, e) -> null;
        private Comparator<G> groupComparator = null;

        private final Set<G> reducedGroups = new HashSet<>();

        private String reducedGroupSaveId = null;
        private Function<G, String> reducedGroupIdMapper = null;
        private Function<String, G> reducedGroupIdBackMapper = null;

        private BiPredicate<G, List<T>> hasGroupHeaderMapper = null;

        private GroupingOptions() {
            //no instances outside of this model class
        }

        public Function<T, G> getGroupMapper() {
            return groupMapper;
        }

        /** Sets a mapper specifying how list items (of type T) are sorted into groups (of type G) */
        public GroupingOptions<G> setGroupMapper(final Function<T, G> groupMapper) {
            this.groupMapper = groupMapper;
            triggerChange(ChangeType.COMPLETE);
            return this;
        }

        public Func4<Pair<G, List<T>>, Context, View, ViewGroup, View> getGroupDisplayViewMapper() {
            return groupDisplayViewMapper;
        }

        /** Sets a mapper providing the text visualization for a group item */
        public GroupingOptions<G> setGroupDisplayMapper(final Func2<G, List<T>, TextParam> groupDisplayMapper) {
            if (groupDisplayMapper != null) {
                setGroupDisplayViewMapper(constructGroupDisplayViewMapper(groupDisplayMapper));
                triggerChange(ChangeType.COMPLETE);
            }
            return this;
        }

        public GroupingOptions<G> setGroupDisplayViewMapper(final Func4<Pair<G, List<T>>, Context, View, ViewGroup, View> groupDisplayViewMapper) {
            if (groupDisplayViewMapper != null) {
                this.groupDisplayViewMapper = groupDisplayViewMapper;
                triggerChange(ChangeType.COMPLETE);
            }
            return this;
        }

        public Func2<G, List<T>, ImageParam> getGroupDisplayIconMapper() {
            return groupDisplayIconMapper;
        }

        /** Sets a mapper providing an optional display icon per group */
        public GroupingOptions<G> setGroupDisplayIconMapper(final Func2<G, List<T>, ImageParam> groupDisplayIconMapper) {
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

        /** Sets the  minimum number of groups on which grouping is really activated */
        public GroupingOptions<G> setHasGroupHeaderMapper(final BiPredicate<G, List<T>> hasGroupHeaderMapper) {
            this.hasGroupHeaderMapper = hasGroupHeaderMapper;
            triggerChange(ChangeType.COMPLETE);
            return this;
        }

        public BiPredicate<G, List<T>> getHasGroupHeaderMapper() {
            return this.hasGroupHeaderMapper;
        }

        public Set<G> getReducedGroups() {
            return reducedGroups;
        }

        public GroupingOptions<G> toggleGroup(final G group) {
            if (reducedGroups.contains(group)) {
                reducedGroups.remove(group);
            } else {
                reducedGroups.add(group);
            }
            saveReducedGroups();
            triggerChange(ChangeType.GROUP_HEADER);
            return this;
        }

        public GroupingOptions<G> setReducedGroups(final Iterable<G> reducedGroups) {
            this.reducedGroups.clear();
            if (reducedGroups != null) {
                for (G group : reducedGroups) {
                    this.reducedGroups.add(group);
                }
            }
            saveReducedGroups();
            triggerChange(ChangeType.GROUP_HEADER);
            return this;
        }

        public GroupingOptions<G> setReducedGroupSaver(final String saveId, final Function<G, String> saveGroupMapper, final Function<String, G> saveGroupBackMapper) {
            this.reducedGroupSaveId = saveId;
            this.reducedGroupIdMapper = saveGroupMapper;
            this.reducedGroupIdBackMapper = saveGroupBackMapper;
            loadReducedGroups();
            return this;
        }

        private void loadReducedGroups() {
            if (this.reducedGroupSaveId != null && this.reducedGroupIdBackMapper != null) {
                final JsonNode node = JsonUtils.stringToNode(Settings.getSimpleListModelConfig(this.reducedGroupSaveId));
                if (node != null) {
                    final List<String> groupStrings = JsonUtils.getTextList(node, "groups");
                    setReducedGroups(groupStrings.stream().map(s -> this.reducedGroupIdBackMapper.apply(s)).collect(Collectors.toList()));
                }
            }
        }

        private void saveReducedGroups() {
            if (this.reducedGroupSaveId != null && this.reducedGroupIdMapper != null) {
                final ObjectNode node = JsonUtils.createObjectNode();
                JsonUtils.setCollection(node, "groups", reducedGroups, g -> JsonUtils.fromText(this.reducedGroupIdMapper.apply(g)));
                Settings.setSimpleListModelConfig(this.reducedGroupSaveId, JsonUtils.nodeToString(node));
            }
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

    public Function<T, String> getTextFilterMapper() {
        return textFilterMapper;
    }

    /** Sets a display providing the text visualization for an item */
    public SimpleItemListModel<T> setDisplayMapper(final Function<T, TextParam> displayMapper) {
        return setDisplayMapper(displayMapper, null, null);
    }

    public SimpleItemListModel<T> setDisplayMapper(final Function<T, TextParam> displayMapper, @Nullable final Function<T, String> textFilterMapper, @Nullable final BiFunction<Context, ViewGroup, TextView> textViewCreator) {
        if (displayMapper != null) {
            setDisplayViewMapper(constructDisplayViewMapper(displayMapper, textViewCreator), textFilterMapper != null ? textFilterMapper : constructFilterTextExtractor(displayMapper));
        }
        return this;
    }

    public String getFilterTerm() {
        return filterTerm;
    }

    public SimpleItemListModel<T> setFilterTerm(final String filterTerm) {
        this.filterTerm = filterTerm;
        triggerChange(ChangeType.FILTER);
        return this;
    }

    public static <TT> Func4<TT, Context, View, ViewGroup, View> constructDisplayViewMapper(final Function<TT, TextParam> displayTextMapper, @Nullable final BiFunction<Context, ViewGroup, TextView> textViewCreator) {
        final BiFunction<Context, ViewGroup, TextView> tvCreator = textViewCreator != null ? textViewCreator :
            (ctx, parent) -> ViewUtils.createTextItem(ctx, R.style.text_default, TextParam.text(""));
        return (item, context, view, parent) -> {
            final TextView tv = view instanceof TextView ? (TextView) view : tvCreator.apply(context, parent);
            final TextParam tp = displayTextMapper == null ? TextParam.text(String.valueOf(item)) : displayTextMapper.apply(item);
            if (tp == null) {
                tv.setText("--");
            } else {
                tp.applyTo(tv);
            }
            return tv;
        };
    }

    public static <GT, TT> Func4<Pair<GT, List<TT>>, Context, View, ViewGroup, View> constructGroupDisplayViewMapper(final Func2<GT, List<TT>, TextParam> displayGroupTextMapper) {
        return (item, context, view, parent) -> {
            final TextView tv = view instanceof TextView ? (TextView) view : ViewUtils.createTextItem(context, R.style.text_default, TextParam.text(""));
            final TextParam tp = displayGroupTextMapper == null || item == null ? TextParam.text(String.valueOf(item)) : displayGroupTextMapper.call(item.first, item.second);
            if (tp == null) {
                tv.setText("--");
            } else {
                tp.applyTo(tv);
            }
            return tv;
        };
    }

    public static <T> Function<T, String> constructFilterTextExtractor(final Function<T, TextParam> displayTextMapper) {
        return (item) -> {

            final TextParam tp = displayTextMapper == null ? null : displayTextMapper.apply(item);
            if (tp == null) {
                return String.valueOf(item);
            }
            return String.valueOf(tp.getText(null));
        };
    }

    /** Sets a view provider for the items */
    public SimpleItemListModel<T> setDisplayViewMapper(final Func4<T, Context, View, ViewGroup, View> displayViewMapper, final Function<T, String> textFilterMapper) {
        if (displayViewMapper != null) {
            this.displayViewMapper = displayViewMapper;
            this.textFilterMapper = textFilterMapper;
            triggerChange(ChangeType.COMPLETE);
        }
        return this;
    }

    public Function<T, ImageParam> getDisplayIconMapper() {
        return displayIconMapper;
    }

    /** Sets a apper providing an optional icon to visualize per item */
    public SimpleItemListModel<T> setDisplayIconMapper(final Function<T, ImageParam> displayIconMapper) {
        if (displayIconMapper != null) {
            this.displayIconMapper = displayIconMapper;
            triggerChange(ChangeType.COMPLETE);
        }
        return this;
    }

    public Function<T, ImageParam> getActionIconMapper() {
        return actionIconMapper;
    }

    /** Sets a mapper providing an optional action icon per item. For each item where the action item
     *  is non-null, it is displayed and the listener provided via {@link #setItemActionListener(Consumer)} is fired */
    public SimpleItemListModel<T> setItemActionIconMapper(final Function<T, ImageParam> actionIconMapper) {
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

    /** Sets padding (in DP) to apply to each list item. Usage is described in {@link ViewUtils#applyPadding(View, int[]...)} */
    public SimpleItemListModel<T> setItemPadding(final int ... itemPaddingsInDp) {
        this.itemPaddingInDp = itemPaddingsInDp;
        triggerChange(ChangeType.COMPLETE);
        return this;
    }

    public int[] getItemPaddingInDp() {
        return this.itemPaddingInDp;
    }

    /** Activates Grouping for this model with given groupMapper. Returns a GroupingOptions instance to set further grouping settings. */
    @SuppressWarnings("unchecked")
    public <G> GroupingOptions<G> activateGrouping(final Function<T, G> groupMapper) {
        this.groupingOptions.setGroupMapper((Function<T, Object>) groupMapper);
        return (GroupingOptions<G>) this.groupingOptions;
    }

    @NonNull
    public GroupingOptions<Object> getGroupingOptions() {
        return this.groupingOptions;
    }

    /** Sets the currently selected items */
    public SimpleItemListModel<T> setSelectedItems(final Iterable<T> selected) {
        if (selected != null) {
            selectedItems.clear();
            for (T sel : selected) {
                selectedItems.add(sel);
            }
            pruneSelected();
            triggerChange(ChangeType.SELECTION);
        }
        return this;
    }

    /** Gets currently selected items. If ChoiceMode is a SINGLE mode then the returned
     *  collection will always have 0 or 1 element only. */
    public Set<T> getSelectedItems() {
        return selectedItems;
    }

    /** Adds a model change listener */
    public SimpleItemListModel<T> addChangeListeners(final Consumer<ChangeType> changeListeners) {
        this.changeListeners.add(changeListeners);
        return this;
    }

    /** Adds an item click listener. Use this for SINGLE select models */
    public SimpleItemListModel<T> addSingleSelectListener(final Consumer<T> selectListener) {
        return addChangeListeners(ct -> {
            if (ct == ChangeType.SELECTION && selectListener != null) {
                selectListener.accept(CommonUtils.first(getSelectedItems()));
            }
        });
    }

    /** Adds an item click listener. Use this for MULTI select models */
    public SimpleItemListModel<T> addMultiSelectListener(final Consumer<Set<T>> selectListener) {
        return addChangeListeners(ct -> {
            if (ct == ChangeType.SELECTION && selectListener != null) {
                selectListener.accept(getSelectedItems());
            }
        });
    }

    /** The listener provided here will be fired for items where an action icon is set if user clicks this icon */
    public SimpleItemListModel<T> setItemActionListener(final Consumer<T> actionListener) {
        this.actionListener = actionListener;
        return this;
    }

    public Consumer<T> getActionListener() {
        return this.actionListener;
    }

    private void triggerChange(final ChangeType mode) {
        for (Consumer<ChangeType> changeListener : this.changeListeners) {
            changeListener.accept(mode);
        }
    }
}
