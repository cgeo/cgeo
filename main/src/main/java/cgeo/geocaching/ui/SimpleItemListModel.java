package cgeo.geocaching.ui;

import cgeo.geocaching.R;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.utils.CommonUtils;
import cgeo.geocaching.utils.ItemGroup;
import cgeo.geocaching.utils.JsonUtils;
import cgeo.geocaching.utils.functions.Action3;
import cgeo.geocaching.utils.functions.Func5;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/** Model class to work with {@link cgeo.geocaching.ui.SimpleItemListView} */
public class SimpleItemListModel<T> {

    private final BiFunction<T, ItemGroup<T, Object>, TextParam> defaultDisplayMapper = (i, gi) -> TextParam.text(i == null ? "-" : i.toString());

    private final List<T> items = new ArrayList<>();
    private final List<T> itemsReadOnly = Collections.unmodifiableList(items);

    private Func5<T, ItemGroup<T, Object>, Context, View, ViewGroup, View> displayViewMapper = constructDisplayTextViewMapper(defaultDisplayMapper, null);
    private BiFunction<T, ItemGroup<T, Object>, String> textFilterMapper = constructFilterTextExtractor(defaultDisplayMapper);
    private Function<T, ImageParam> displayIconMapper = (o) -> null;

    private Function<T, ImageParam> actionIconMapper = (o) -> null;

    private ChoiceMode choiceMode = ChoiceMode.SINGLE_PLAIN;

    private int[] itemPaddingInDp = new int[]{10, 4, 10, 4};

    private final GroupingOptions<Object> groupingOptions = new GroupingOptions<>();

    private String filterTerm = null;

    private final Set<T> selectedItems = new HashSet<>();

    private final List<Consumer<ChangeType>> changeListeners = new ArrayList<>();

    private Consumer<T> actionListener = null;

    private int columnCount = 1;
    private Function<T, Integer> columnSpanMapper = null;


    /** Supported display modes for choosing items from a list */
    public enum ChoiceMode { SINGLE_PLAIN, SINGLE_RADIO, MULTI_CHECKBOX }

    /** Types of model changes for which events are fired */
    public enum ChangeType { COMPLETE, SELECTION, FILTER, GROUP_HEADER }

    /** Specifies options for grouping items */
    public class GroupingOptions<G> {

        private Function<T, G> groupMapper = null;
        private Function<G, G> groupGroupMapper = null;

        private Func5<G, ItemGroup<T, G>, Context, View, ViewGroup, View> groupDisplayViewMapper = (group, itemGroup, context, view, parent) -> null;
        private Function<ItemGroup<T, G>, ImageParam> groupDisplayIconMapper = (ig) -> null;
        private Comparator<Object> itemGroupComparator = null;
        private Comparator<G> groupComparator = null;
        private boolean sortGroupsBeforeItems = false;
        private Predicate<ItemGroup<T, G>> groupPruner = null;

        private final Set<G> reducedGroups = new HashSet<>();

        private String reducedGroupSaveId = null;
        private Function<G, String> reducedGroupIdMapper = null;
        private Function<String, G> reducedGroupIdBackMapper = null;



        private GroupingOptions() {
            //no instances outside of this model class
        }

        public Function<T, G> getGroupMapper() {
            return groupMapper;
        }

        public Function<G, G> getGroupGroupMapper() {
            return groupGroupMapper;
        }

        /** Sets a mapper specifying how list items (of type T) are sorted into groups (of type G) */
        public GroupingOptions<G> setGroupMapper(final Function<T, G> groupMapper) {
            this.groupMapper = groupMapper;
            triggerChange(ChangeType.COMPLETE);
            return this;
        }

        /** Sets a mapper specifying how group items (of type G) are sorted into other groups (of type G) */
        public GroupingOptions<G> setGroupGroupMapper(final Function<G, G> groupGroupMapper) {
            this.groupGroupMapper = groupGroupMapper;
            triggerChange(ChangeType.COMPLETE);
            return this;
        }

        public Func5<G, ItemGroup<T, G>, Context, View, ViewGroup, View> getGroupDisplayViewMapper() {
            return groupDisplayViewMapper;
        }

        /** Sets a mapper providing the text visualization for a group item */
        public GroupingOptions<G> setGroupDisplayMapper(final Function<ItemGroup<T, G>, TextParam> groupDisplayMapper) {
            if (groupDisplayMapper != null) {
                setGroupDisplayViewMapper(constructGroupDisplayViewMapper(groupDisplayMapper));
                triggerChange(ChangeType.COMPLETE);
            }
            return this;
        }

        public GroupingOptions<G> setGroupDisplayViewMapper(final Func5<G, ItemGroup<T, G>, Context, View, ViewGroup, View> groupDisplayViewMapper) {
            if (groupDisplayViewMapper != null) {
                this.groupDisplayViewMapper = groupDisplayViewMapper;
                triggerChange(ChangeType.COMPLETE);
            }
            return this;
        }

        public Function<ItemGroup<T, G>, ImageParam> getGroupDisplayIconMapper() {
            return groupDisplayIconMapper;
        }

        /** Sets a mapper providing an optional display icon per group */
        public GroupingOptions<G> setGroupDisplayIconMapper(final Function<ItemGroup<T, G>, ImageParam> groupDisplayIconMapper) {
            if (groupDisplayIconMapper != null) {
                this.groupDisplayIconMapper = groupDisplayIconMapper;
                triggerChange(ChangeType.COMPLETE);
            }
            return this;
        }

        @NonNull
        public Comparator<Object> getItemGroupComparator() {
            final boolean hasItemGroupComparator = itemGroupComparator != null;

            final Comparator<G> gComparator = hasItemGroupComparator ? null : this.groupComparator != null ? this.groupComparator : CommonUtils.getTextSortingComparator(Objects::toString);
            final Comparator<T> tComparator = hasItemGroupComparator ? null : CommonUtils.getListSortingComparator(null, true, getItems());

            return CommonUtils.getNullHandlingComparator((o1, o2) -> {

                if (hasItemGroupComparator) {
                    return itemGroupComparator.compare(o1, o2);
                }

                final boolean o1IsGroup = o1 instanceof ItemGroup;
                final boolean o2IsGroup = o2 instanceof ItemGroup;
                if (o1IsGroup != o2IsGroup) {
                    return this.sortGroupsBeforeItems ^ o1IsGroup ? -1 : 1;
                }
                if (o1IsGroup) {
                    return gComparator.compare(((ItemGroup<T, G>) o1).getGroup(), ((ItemGroup<T, G>) o2).getGroup());
                }
                return tComparator.compare((T) o1, (T) o2);
            }, true);
        }

        /** Sets a cgroups are sorted and where items are sorted in reference to groups */
        public GroupingOptions<G> setGroupComparator(final Comparator<G> groupComparator, final boolean sortGroupsBeforeItems) {
            this.groupComparator = groupComparator;
            this.sortGroupsBeforeItems = sortGroupsBeforeItems;
            triggerChange(ChangeType.COMPLETE);
            return this;
        }

        /** Sets a comparator specifying how groups and items are sorted. Comparator must be able to handle lists of items of T and G intermixed */
        public GroupingOptions<G> setItemGroupComparator(final Comparator<Object> groupComparator) {
            this.itemGroupComparator = groupComparator;
            triggerChange(ChangeType.COMPLETE);
            return this;
        }

        /** Sets the  minimum number of groups on which grouping is really activated */
        public GroupingOptions<G> setGroupPruner(final Predicate<ItemGroup<T, G>> groupPruner) {
            this.groupPruner = groupPruner;
            triggerChange(ChangeType.COMPLETE);
            return this;
        }

        public Predicate<ItemGroup<T, G>> getGroupPruner() {
            return this.groupPruner;
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

    public Func5<T, ItemGroup<T, Object>, Context, View, ViewGroup, View> getDisplayViewMapper() {
        return displayViewMapper;
    }

    public BiFunction<T, ItemGroup<T, Object>, String> getTextFilterMapper() {
        return textFilterMapper;
    }

    /** Sets a display providing the text visualization for an item */
    public SimpleItemListModel<T> setDisplayMapper(final Function<T, TextParam> displayMapper) {
        if (displayMapper != null) {
            setDisplayMapper((item, itemGroup) -> displayMapper.apply(item), null, null);
        }
        return this;    }

   public SimpleItemListModel<T> setDisplayMapper(final BiFunction<T, ItemGroup<T, Object>, TextParam> displayMapper, @Nullable final BiFunction<T, ItemGroup<T, Object>, String> textFilterMapper, @Nullable final BiFunction<Context, ViewGroup, TextView> textViewCreator) {
        if (displayMapper != null) {
            setDisplayViewMapper(constructDisplayTextViewMapper(displayMapper, textViewCreator), textFilterMapper != null ? textFilterMapper : constructFilterTextExtractor(displayMapper));
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

    public static <TT> Func5<TT, ItemGroup<TT, Object>, Context, View, ViewGroup, View> constructDisplayTextViewMapper(final BiFunction<TT, ItemGroup<TT, Object>, TextParam> displayTextMapper, @Nullable final BiFunction<Context, ViewGroup, TextView> textViewCreator) {
        final BiFunction<Context, ViewGroup, TextView> tvCreator = textViewCreator != null ? textViewCreator :
            (ctx, parent) -> ViewUtils.createTextItem(ctx, R.style.text_default, TextParam.text(""));
        return (item, itemGroup, context, view, parent) -> {
            final TextView tv = view instanceof TextView ? (TextView) view : tvCreator.apply(context, parent);
            final TextParam tp = displayTextMapper == null ? TextParam.text(String.valueOf(item)) : displayTextMapper.apply(item, itemGroup);
            if (tp == null) {
                tv.setText("--");
            } else {
                tp.applyTo(tv);
            }
            return tv;
        };
    }

    public static <GT, TT> Func5<GT, ItemGroup<TT, GT>, Context, View, ViewGroup, View> constructGroupDisplayViewMapper(final Function<ItemGroup<TT, GT>, TextParam> displayGroupTextMapper) {
        return (group, itemGroup, context, view, parent) -> {
            final TextView tv = view instanceof TextView ? (TextView) view : ViewUtils.createTextItem(context, R.style.text_default, TextParam.text(""));
            final TextParam tp = displayGroupTextMapper == null || itemGroup == null ? TextParam.text(String.valueOf(itemGroup)) : displayGroupTextMapper.apply(itemGroup);
            if (tp == null) {
                tv.setText("--");
            } else {
                tp.applyTo(tv);
            }
            return tv;
        };
    }

    public static <T> BiFunction<T, ItemGroup<T, Object>, String> constructFilterTextExtractor(final BiFunction<T, ItemGroup<T, Object>, TextParam> displayTextMapper) {
        return (item, itemGroup) -> {

            final TextParam tp = displayTextMapper == null ? null : displayTextMapper.apply(item, itemGroup);
            if (tp == null) {
                return String.valueOf(item);
            }
            return String.valueOf(tp.getText(null));
        };
    }

    /**
     * Sets a view provider for the items.
     * <br>
     * Use this method ONLY if you need full control over the view creation. In most cases this is not needed
     *
     * @param displayViewMapper needs to return a fully prepared view for a gien item, its group, the previous view (if any) and its viewgroup (for new view creation)
     * @param textFilterMapper needs to return for a given item and its group the text string against text filtering will be done
     * @return this
     */
    public SimpleItemListModel<T> setDisplayViewMapper(final Func5<T, ItemGroup<T, Object>, Context, View, ViewGroup, View> displayViewMapper, final BiFunction<T, ItemGroup<T, Object>, String> textFilterMapper) {
        if (displayViewMapper != null) {
            this.displayViewMapper = displayViewMapper;
            this.textFilterMapper = textFilterMapper;
            triggerChange(ChangeType.COMPLETE);
        }
        return this;
    }

    /** Sets a view provider for items based on a layout */
    public SimpleItemListModel<T> setDisplayViewMapper(@LayoutRes final int layoutResId, final Action3<T, ItemGroup<T, Object>, View> fillView, final BiFunction<T, ItemGroup<T, Object>, String> textFilterMapper) {
        if (layoutResId != 0 && fillView != null) {
            setDisplayViewMapper((item, itemGroup, ctx, view, viewGroup) -> {
                //get or create view
                final View realView = view == null ? LayoutInflater.from(ctx).inflate(layoutResId, viewGroup, false) : view;
                //fill view
                fillView.call(item, itemGroup, realView);
                //return view
                return realView;
            }, textFilterMapper);
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

    public SimpleItemListModel<T> setColumns(final int columnCount, final Function<T, Integer> columnSpanMapper) {
        this.columnCount = columnCount;
        this.columnSpanMapper = columnSpanMapper;
        triggerChange(ChangeType.COMPLETE);
        return this;
    }

    public int getColumnCount() {
        return this.columnCount;
    }

    public final Function<T, Integer> getColumnSpanMapper() {
        return this.columnSpanMapper;
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
