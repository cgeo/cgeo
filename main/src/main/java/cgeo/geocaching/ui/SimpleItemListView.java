package cgeo.geocaching.ui;

import cgeo.geocaching.R;
import cgeo.geocaching.databinding.SimpleitemlistItemViewBinding;
import cgeo.geocaching.databinding.SimpleitemlistViewBinding;
import cgeo.geocaching.ui.recyclerview.ManagedListAdapter;
import cgeo.geocaching.utils.ItemGroup;
import cgeo.geocaching.utils.LocalizationUtils;
import cgeo.geocaching.utils.Log;
import cgeo.geocaching.utils.functions.Func5;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiFunction;

import com.google.android.material.button.MaterialButton;
import org.apache.commons.lang3.StringUtils;

/**
 * This view displays and maintains a list of simple view items, mainly for (single or multiple)
 * item selection lists. It provides special support for grouping, filtering and "select all" for such
 * lists.
 */
public class SimpleItemListView extends LinearLayout {

    private enum ListItemType { ITEM, GROUPHEADER, SELECT_ALL, SELECTED_VISIBLE }

    private static final Func5<Object, ItemGroup<Object, Object>, Context, View, ViewGroup, View> SELECT_VIEW_MAPPER = SimpleItemListModel.constructDisplayTextViewMapper((s, ig) -> TextParam.text(s.toString()), null);

    private ItemListAdapter listAdapter;
    private SimpleitemlistViewBinding binding;

    private SimpleItemListModel<Object> model = new SimpleItemListModel<>();

    private int currentlyVisible = 0;
    private int currentlyVisibleSelected = 0;
    private boolean itemsHaveIcons = false;
    private boolean groupsHaveIcons = false;

    private static class ListItem {

        public final Object value; //only filled for items
        public final ItemGroup<Object, Object> itemGroup; // for groups: the group item. For items: the parent group

        public final ListItemType type;

        public final ImageParam icon;
        public final ImageParam actionIcon;

        //public Boolean isVisible = null; // "null" means: "need to be refreshed"
        public boolean isVisibleByFilter = true; // will be set on text-filter-reevaluation
        public boolean isVisible = true; //will be set on item visibility reevaluation

        private ListItem(final Object value, final ItemGroup<Object, Object> itemGroup, final ImageParam icon, final ImageParam actionIcon, final ListItemType type) { // }, final int groupFirstItemIndex) {
            this.value = value;
            this.icon = icon;
            this.actionIcon = actionIcon;
            this.type = type;
            this.itemGroup = itemGroup;
        }

        @NonNull
        public String toString() {
            return "[" + type + "]" + (type == ListItemType.GROUPHEADER ? itemGroup.getGroup() : value) + "(visible: " + isVisible + ")";
        }

    }

    private ListItem createForItem(final Object value, final ItemGroup<Object, Object> group) {
        return new ListItem(value, group,
            model.getDisplayIconMapper().apply(value),
            model.getActionIconMapper().apply(value),
            ListItemType.ITEM);
    }

    private ListItem createForGroup(final ItemGroup<Object, Object> group) {
        return new ListItem(null, group,
                model.getGroupingOptions().getGroupDisplayIconMapper().apply(group),
    null, ListItemType.GROUPHEADER);
    }

    private ListItem createForType(final ListItemType type) {
        return new ListItem(null, null, null, null, type);
    }


    private class ItemListViewHolder extends RecyclerView.ViewHolder {

        private final SimpleitemlistItemViewBinding binding;

        ItemListViewHolder(final SimpleitemlistItemViewBinding itemBinding) {
            super(itemBinding.getRoot());
            this.binding = itemBinding;
        }

        @SuppressWarnings({"PMD.NPathComplexity"})
        void fillData(final ListItem data) {
            if (data == null) {
                return;
            }

            switch (data.type) {
                case SELECT_ALL:
                    final String selectAllText = "<" + LocalizationUtils.getString(R.string.multiselect_selectall) + " (" + model.getSelectedItems().size() + "/" + model.getItems().size() + ")>";
                    applyItemView(binding, selectAllText, null, SELECT_VIEW_MAPPER);
                    binding.itemCheckbox.setChecked(model.getSelectedItems().size() == model.getItems().size());
                    binding.itemIcon.setVisibility(GONE);
                    break;
                case SELECTED_VISIBLE:
                    final String selectVisibleText = "<" + LocalizationUtils.getString(R.string.multiselect_selectvisible) + " (" + currentlyVisibleSelected + "/" + currentlyVisible + ")>";
                    applyItemView(binding, selectVisibleText, null, SELECT_VIEW_MAPPER);
                    binding.itemCheckbox.setChecked(currentlyVisibleSelected == currentlyVisible);
                    binding.itemIcon.setVisibility(GONE);
                    break;
                default:
                    //handling of groups and items
                    if (data.type == ListItemType.GROUPHEADER) {
                        applyItemView(binding, data.itemGroup.getGroup(), data.itemGroup, model.getGroupingOptions().getGroupDisplayViewMapper());
                    } else {
                        applyItemView(binding, data.value, data.itemGroup, model.getDisplayViewMapper());
                    }

                    final boolean showIcon = data.icon != null || (data.type == ListItemType.GROUPHEADER ? groupsHaveIcons : itemsHaveIcons);
                    if (showIcon) {
                        (data.icon == null ? ImageParam.TRANSPARENT : data.icon).applyTo(binding.itemIcon);
                        binding.itemIcon.setVisibility(VISIBLE);
                    } else {
                        binding.itemIcon.setVisibility(GONE);
                    }
                    if (data.actionIcon != null) {
                        final MaterialButton b = (MaterialButton) binding.itemAction;
                        data.actionIcon.applyToIcon(b);
                    }
                    binding.itemRadiobutton.setChecked(model.getSelectedItems().contains(data.value));
                    binding.itemCheckbox.setChecked(model.getSelectedItems().contains(data.value));
                    break;
            }

            final int groupPadding = Math.max(0, data.itemGroup == null ? 0 : data.itemGroup.getLevel() - (data.type == ListItemType.GROUPHEADER ? 1 : 0));
            binding.itemChecker.setPadding(ViewUtils.dpToPixel(34 * groupPadding), 0, 0, 0);

            binding.itemCheckbox.setVisibility(data.type != ListItemType.GROUPHEADER && model.getChoiceMode() == SimpleItemListModel.ChoiceMode.MULTI_CHECKBOX ? VISIBLE : GONE);
            binding.itemRadiobutton.setVisibility(data.type == ListItemType.ITEM && model.getChoiceMode() == SimpleItemListModel.ChoiceMode.SINGLE_RADIO ? VISIBLE : GONE);
            binding.itemGroupToggle.setVisibility(data.type == ListItemType.GROUPHEADER ? VISIBLE : GONE);
            binding.groupExpanded.setVisibility(data.type == ListItemType.GROUPHEADER && isGroupExpanded(data.itemGroup.getGroup()) ? VISIBLE : GONE);
            binding.groupReduced.setVisibility(data.type == ListItemType.GROUPHEADER && !isGroupExpanded(data.itemGroup.getGroup()) ? VISIBLE : GONE);
            binding.itemAction.setVisibility(data.type == ListItemType.ITEM && data.actionIcon != null ? VISIBLE : GONE);
        }

        private <T> void applyItemView(final SimpleitemlistItemViewBinding itemBinding, final T value, final ItemGroup<T, Object> itemGroup, final Func5<T, ItemGroup<T, Object>, Context, View, ViewGroup, View> viewMapper) {
            final View currentView = itemBinding.itemViewAnchor.getChildAt(0);
            final View newView = viewMapper.call(value, itemGroup, getContext(), currentView, itemBinding.itemViewAnchor);

            if (currentView != newView) {
                itemBinding.itemViewAnchor.removeAllViews();
                itemBinding.itemViewAnchor.addView(newView);
            }
        }
    }


    private class ItemListAdapter extends ManagedListAdapter<ListItem, ItemListViewHolder> {

        protected ItemListAdapter(final RecyclerView recyclerView) {
            super(new Config(recyclerView));
        }

        @NonNull
        @Override
        public ItemListViewHolder onCreateViewHolder(@NonNull final ViewGroup parent, final int viewType) {
            final View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.simpleitemlist_item_view, parent, false);

            ViewUtils.applyPadding(view, model.getItemPaddingInDp());
            final SimpleitemlistItemViewBinding itemBinding = SimpleitemlistItemViewBinding.bind(view);

            final ItemListViewHolder vh = new ItemListViewHolder(itemBinding);
            itemBinding.itemRadiobutton.setClickable(false);
            itemBinding.itemViewAnchor.setClickable(false);
            itemBinding.itemCheckbox.setClickable(false);
            itemBinding.groupReduced.setClickable(false);
            itemBinding.groupExpanded.setClickable(false);
            return vh;
        }

        private void handleActionClick(final int adapterPos) {
            final ListItem itemData = getItems().get(adapterPos);
            if (itemData == null || itemData.type != ListItemType.ITEM || model.getActionListener() == null) {
                return;
            }
            model.getActionListener().accept(itemData.value);
        }

        private void handleClick(final int adapterPos) {
            if (adapterPos < 0 || adapterPos >= getItems().size()) {
                Log.e("adapterPos outside of list range: " + adapterPos + " >= " + getItems().size() + ": " + getItems());
                return;
            }
            final ListItem itemData = getItems().get(adapterPos);
            boolean selectionChanged = false;
            final Set<Object> newSelection = new HashSet<>(model.getSelectedItems());

            switch (itemData.type) {
                case SELECT_ALL:
                    if (model.getSelectedItems().size() == model.getItems().size()) {
                        newSelection.clear();
                    } else {
                        newSelection.addAll(model.getItems());
                    }
                    selectionChanged = true;
                    break;
                case SELECTED_VISIBLE:
                    final boolean remove = (currentlyVisible == currentlyVisibleSelected);
                    for (ListItem item : getOriginalItems()) {
                        if (item.type == ListItemType.ITEM && item.isVisible) {
                            if (remove) {
                                newSelection.remove(item.value);
                            } else {
                                newSelection.add(item.value);
                            }
                            selectionChanged = true;
                        }
                    }
                    break;
                case GROUPHEADER:
                    toggleGroupExpanded(itemData.itemGroup.getGroup());
                    break;
                case ITEM:
                default:
                    // data element
                    final Object itemValue = itemData.value;
                    final boolean itemIsSelected = newSelection.contains(itemValue);
                    if (model.getChoiceMode() == SimpleItemListModel.ChoiceMode.MULTI_CHECKBOX) {
                        if (itemIsSelected) {
                            newSelection.remove(itemValue);
                        } else {
                            newSelection.add(itemValue);
                        }
                    } else {
                        if (!newSelection.contains(itemValue)) {
                            newSelection.clear();
                            newSelection.add(itemValue);
                        }
                    }
                    selectionChanged = true;
            }
            if (selectionChanged) {
                model.setSelectedItems(newSelection);
            }
        }

        @Override
        public void onBindViewHolder(@NonNull final ItemListViewHolder holder, final int position) {
            holder.fillData(getItem(position));

            holder.binding.getRoot().setOnClickListener(v -> handleClick(holder.getBindingAdapterPosition()));
            holder.binding.itemChecker.setOnClickListener(v -> handleClick(holder.getBindingAdapterPosition()));
            ViewUtils.walkViewTree(holder.binding.itemViewAnchor, view -> {
                view.setOnClickListener(v -> handleClick(holder.getBindingAdapterPosition()));
                return true;
            }, null);
            holder.binding.itemIcon.setOnClickListener(v -> handleClick(holder.getBindingAdapterPosition()));
            holder.binding.itemAction.setOnClickListener(v -> handleActionClick(holder.getBindingAdapterPosition()));
        }

        @Override
        public int getItemViewType(final int position) {
            //For all types the same item is created.
            //However we have to make sure that items are reused only within their type
            //--> this is what this function takes care of by assigning unique view types to them
            final ListItem itemData = getItems().get(position);
            return itemData.type.ordinal();
        }

        private boolean isDisplayed(final ListItem item) {
            if (item == null) {
                return true;
            }
            switch (item.type) {
                case SELECT_ALL:
                    return model.getChoiceMode() == SimpleItemListModel.ChoiceMode.MULTI_CHECKBOX && model.getItems().size() > 1;
                case SELECTED_VISIBLE:
                    return model.getChoiceMode() == SimpleItemListModel.ChoiceMode.MULTI_CHECKBOX && currentlyVisible < model.getItems().size();
                case GROUPHEADER:
                case ITEM:
                default:
                    return item.isVisible;
            }
        }
    }

    public SimpleItemListView(final Context context) {
        super(wrap(context));
        init();
    }

    public SimpleItemListView(final Context context, @Nullable final AttributeSet attrs) {
        super(wrap(context), attrs);
        init();
    }

    public SimpleItemListView(final Context context, @Nullable final AttributeSet attrs, final int defStyleAttr) {
        super(wrap(context), attrs, defStyleAttr);
        init();
    }

    public SimpleItemListView(final Context context, final AttributeSet attrs, final int defStyleAttr, final int defStyleRes) {
        super(wrap(context), attrs, defStyleAttr, defStyleRes);
        init();
    }

    private static Context wrap(final Context context) {
        return ViewUtils.wrap(context, R.style.cgeo);
    }

    private void init() {
        final Context ctw = getContext();
        inflate(ctw, R.layout.simpleitemlist_view, this);
        this.binding = SimpleitemlistViewBinding.bind(this);
        setOrientation(VERTICAL);

        listAdapter = new ItemListAdapter(binding.list);
        binding.list.setAdapter(listAdapter);
    }

    @SuppressWarnings("unchecked")
    public void setModel(final SimpleItemListModel<?> model) {
        if (model != null) {
            this.model = (SimpleItemListModel<Object>) model;
            this.model.addChangeListeners(this::handleModelChange);
            handleModelChange(SimpleItemListModel.ChangeType.COMPLETE);
        }
    }

    public void setColumnCount(final int columnCount) {
        if (columnCount <= 1) {
            binding.list.setLayoutManager(new LinearLayoutManager(this.getContext()));
        } else {
            binding.list.setLayoutManager(new GridLayoutManager(this.getContext(), columnCount));
        }
        handleModelChange(SimpleItemListModel.ChangeType.COMPLETE);
    }

    public void scrollTo(final Object value) {
        if (value == null || binding.list.getLayoutManager() == null) {
            return;
        }

        int pos = 0;
        for (ListItem item : listAdapter.getItems()) {
            if (Objects.equals(value, item.value) && item.type == ListItemType.ITEM) {
                binding.list.getLayoutManager().scrollToPosition(pos);
                break;
            }
            pos++;
        }

    }

    private void handleModelChange(final SimpleItemListModel.ChangeType changeType) {
        switch (changeType) {
            case COMPLETE:
                recreateList();
                break;
            case FILTER:
                reevaluateTextFilterVisibility();
                triggerRefreshList();
                break;
            case SELECTION:
            case GROUP_HEADER:
            default:
                triggerRefreshList();
                break;
        }
    }

    private void triggerRefreshList() {

        //prepare overall list data which is needed for display routines
        int visible = 0;
        int visibleSelected = 0;
        this.itemsHaveIcons = false;
        this.groupsHaveIcons = false;

        reevaluateItemVisibility();

        for (ListItem item : listAdapter.getOriginalItems()) {
            switch (item.type) {
                case GROUPHEADER:
                    if (item.icon != null) {
                        this.groupsHaveIcons = true;
                    }
                    break;
                case ITEM:
                    if (item.icon != null) {
                        this.itemsHaveIcons = true;
                    }
                    if (item.isVisible) {
                        visible++;
                        if (model.getSelectedItems().contains(item.value)) {
                            visibleSelected++;
                        }
                    }
                    break;
                default:
                    //do nothing
                    break;
            }
        }
        this.currentlyVisible = visible;
        this.currentlyVisibleSelected = visibleSelected;

        //trigger re-filtering and re-drawing of list items
        listAdapter.setFilter(t -> listAdapter.isDisplayed(t));
        listAdapter.notifyItemRangeChanged(0, listAdapter.getItemCount());
    }

    private boolean isGroupExpanded(final Object group) {
        return !model.getGroupingOptions().getReducedGroups().contains(group);
    }

    private void toggleGroupExpanded(final Object group) {
        model.getGroupingOptions().toggleGroup(group);
    }

    private void reevaluateTextFilterVisibility() {
        final String filter = model.getFilterTerm();
        final BiFunction<Object, ItemGroup<Object, Object>, String> textFilterMapper = model.getTextFilterMapper();
        final boolean allVisible = StringUtils.isBlank(filter) || textFilterMapper == null;
        for (ListItem item : listAdapter.getOriginalItems()) {
            if (item.type != ListItemType.ITEM || allVisible) {
                item.isVisibleByFilter = true;
            } else {
                final String rawText = textFilterMapper.apply(item.value, item.itemGroup);
                item.isVisibleByFilter = rawText != null && rawText.toLowerCase(Locale.US).contains(filter.trim().toLowerCase(Locale.US));
            }
        }
    }

    private void reevaluateItemVisibility() {
        //VISIBILITY RULES
        //- An item is visible IF it is visible by its text filter AND its group is expanded
        //- A group is visible IF it has at least one visible contained item (subgroup or base item) AND  none of its PARENTS it REDUCED
        //  (note: group header of a reduced group MUST BE displayed (if none of the parents is reduced)
        //Attention: this method assumes that the item.visibleByFilter is correctly set for all ITEMs!

        //1. collect groups which are visible
        final Map<Object, Boolean> visibleGroups = new HashMap<>();
        for (ListItem item : listAdapter.getOriginalItems()) {
            if (item.type == ListItemType.ITEM && item.isVisibleByFilter) {
                determineGroupAndParentsVisibility(item.itemGroup, visibleGroups);
            }
        }

        //2. set visible flag for items and groupheaders
        for (ListItem item : listAdapter.getOriginalItems()) {
            final Object group = item.itemGroup == null ? null : item.itemGroup.getGroup();
            item.isVisible = item.isVisibleByFilter && (group == null || Boolean.TRUE.equals(visibleGroups.get(group)))
              && (item.type != ListItemType.ITEM || isGroupExpanded(group));
        }
    }

    private boolean determineGroupAndParentsVisibility(final ItemGroup<Object, Object> itemGroup, final Map<Object, Boolean> determinedGroups) {
        if (itemGroup == null || itemGroup.getGroup() == null || itemGroup.getParent() == null) {
            return true;
        }
        if (determinedGroups.containsKey(itemGroup.getGroup())) {
            return Boolean.TRUE.equals(determinedGroups.get(itemGroup.getGroup()));
        }
        final boolean isParentExpanded = isGroupExpanded(itemGroup.getParent().getGroup());
        final boolean isParentVisible = determineGroupAndParentsVisibility(itemGroup.getParent(), determinedGroups);
        final boolean isVisible = isParentVisible && isParentExpanded;
        determinedGroups.put(itemGroup.getGroup(), isVisible);
        return isVisible;
    }

    private void recreateList() {

        //reset layout manager
        recreateLayoutManager();

        final List<ListItem> list = new ArrayList<>();
        list.add(createForType(ListItemType.SELECT_ALL));
        list.add(createForType(ListItemType.SELECTED_VISIBLE));

        final ItemGroup<Object, Object> root =
            ItemGroup.create(model.getItems(),
            model.getGroupingOptions().getGroupMapper(),
            model.getGroupingOptions().getGroupGroupMapper(),
            model.getGroupingOptions().getItemGroupComparator(),
            model.getGroupingOptions().getGroupPruner());

        recreateListItem(root, list);

        reevaluateTextFilterVisibility();

        listAdapter.setItems(list);

        triggerRefreshList();
    }

    private void recreateListItem(final ItemGroup<Object, Object> group, final List<ListItem> list) {
        for (Object child : group.getItemsAndGroups()) {
            if (child instanceof ItemGroup) {
                list.add(createForGroup((ItemGroup<Object, Object>) child));
                recreateListItem((ItemGroup<Object, Object>) child, list);
            } else {
                list.add(createForItem(child, group));
            }
        }
    }

    private void recreateLayoutManager() {
        final RecyclerView.LayoutManager currentLm = binding.list.getLayoutManager();
        final int columnCount = model.getColumnCount();
        if (columnCount <= 1) {
            if (!(currentLm instanceof LinearLayoutManager)) {
                binding.list.setLayoutManager(new LinearLayoutManager(getContext()));
            }
        } else {
            if (!(currentLm instanceof GridLayoutManager) || ((GridLayoutManager) currentLm).getSpanCount() != columnCount) {
                binding.list.setLayoutManager(new GridLayoutManager(getContext(), model.getColumnCount()));
            }
            final GridLayoutManager glm = (GridLayoutManager) binding.list.getLayoutManager();
            glm.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
               @Override
               public int getSpanSize(final int pos) {
                   final ListItem item = listAdapter.getItem(pos);
                   if (item.type == ListItemType.ITEM) {
                       final int span = model.getColumnSpanMapper() == null ? 1 : model.getColumnSpanMapper().apply(item.value);
                       return Math.max(1, Math.min(columnCount, span));
                   }
                   return columnCount;
               }
           });
        }
    }
}
