package cgeo.geocaching.ui;

import cgeo.geocaching.R;
import cgeo.geocaching.databinding.SimpleitemlistItemViewBinding;
import cgeo.geocaching.databinding.SimpleitemlistViewBinding;
import cgeo.geocaching.ui.recyclerview.ManagedListAdapter;
import cgeo.geocaching.utils.CommonUtils;
import cgeo.geocaching.utils.LocalizationUtils;
import cgeo.geocaching.utils.functions.Func4;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

import com.google.android.material.button.MaterialButton;
import org.apache.commons.lang3.StringUtils;

/**
 * This view displays and maintains a list of simple view items, mainly for (single or multiple)
 * item selection lists. It provides special support for grouping, filtering and "select all" for such
 * lists.
 */
public class SimpleItemListView extends LinearLayout {

    private enum ListItemType { ITEM, GROUPHEADER, SELECT_ALL, SELECTED_VISIBLE }

    private static final Func4<Object, Context, View, ViewGroup, View> SELECT_VIEW_MAPPER = SimpleItemListModel.constructDisplayViewMapper(s -> TextParam.text(s.toString()), null);

    private ItemListAdapter listAdapter;
    private SimpleitemlistViewBinding binding;

    private SimpleItemListModel<Object> model = new SimpleItemListModel<>();

    private int currentlyVisible = 0;
    private int currentlyVisibleSelected = 0;
    private boolean itemsHaveIcons = false;
    private boolean groupsHaveIcons = false;

    private static class ListItem {

        public final Object value;

        //public final TextParam text;
        public final ImageParam icon;
        public final ImageParam actionIcon;

        public final ListItemType type;

        public final int originalIndex; // only filled for items
        public final Object group; // only filled if isGroupHeader = false and item belongs to a group
        public final boolean hasGroupHeader; // true for items which belong to a group with a header

        public final int groupFirstItemIndex; // only filled if isGroupHeader = true
        public final List<Object> groupElements; // only filled if isGroupHeader = true

        private ListItem(final Object value, final ImageParam icon, final ImageParam actionIcon, final ListItemType type,
                        final int originalIndex, final Object group, final boolean hasGroupHeader, final int groupFirstItemIndex, final List<Object> groupElements) {
            this.value = value;
            this.icon = icon;
            this.actionIcon = actionIcon;
            this.type = type;
            this.originalIndex = originalIndex;
            this.group = group;
            this.hasGroupHeader = hasGroupHeader;
            this.groupFirstItemIndex = groupFirstItemIndex;
            this.groupElements = groupElements;
        }

    }

    private ListItem createForItem(final Object value, final int originalIndex, final Object group, final boolean hasGroupHeader) {
        return new ListItem(value,
            model.getDisplayIconMapper().apply(value),
            model.getActionIconMapper().apply(value),
            ListItemType.ITEM, originalIndex, group, hasGroupHeader, -1, null);
    }

    private ListItem createForGroup(final Object group, final int groupFirstItemIndex, final List<Object> groupElements) {
        return new ListItem(group, model.getGroupingOptions().getGroupDisplayIconMapper().call(group, groupElements),
    null, ListItemType.GROUPHEADER,  -1, -1, false, groupFirstItemIndex, groupElements);
    }

    private ListItem createForType(final ListItemType type) {
        return new ListItem(null, null, null, type,  -1, -1, false, -1, null);
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
                    applyItemView(binding, selectAllText, SELECT_VIEW_MAPPER);
                    binding.itemCheckbox.setChecked(model.getSelectedItems().size() == model.getItems().size());
                    binding.itemIcon.setVisibility(GONE);
                    break;
                case SELECTED_VISIBLE:
                    final String selectVisibleText = "<" + LocalizationUtils.getString(R.string.multiselect_selectvisible) + " (" + currentlyVisibleSelected + "/" + currentlyVisible + ")>";
                    applyItemView(binding, selectVisibleText, SELECT_VIEW_MAPPER);
                    binding.itemCheckbox.setChecked(currentlyVisibleSelected == currentlyVisible);
                    binding.itemIcon.setVisibility(GONE);
                    break;
                default:
                    //handling of groups and items
                    if (data.type == ListItemType.GROUPHEADER) {
                        applyItemView(binding, new Pair<>(data.value, data.groupElements), model.getGroupingOptions().getGroupDisplayViewMapper());
                    } else {
                        applyItemView(binding, data.value, model.getDisplayViewMapper());
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

            final boolean isGroupedItem = data.type == ListItemType.ITEM && data.hasGroupHeader;
            binding.itemChecker.setVisibility(model.getChoiceMode() != SimpleItemListModel.ChoiceMode.SINGLE_PLAIN || isGroupedItem ? VISIBLE : GONE);
            binding.itemCheckbox.setVisibility(data.type != ListItemType.GROUPHEADER && model.getChoiceMode() == SimpleItemListModel.ChoiceMode.MULTI_CHECKBOX ? VISIBLE : GONE);
            binding.itemRadiobutton.setVisibility(data.type == ListItemType.ITEM && model.getChoiceMode() == SimpleItemListModel.ChoiceMode.SINGLE_RADIO ? VISIBLE : GONE);
            binding.itemGroupPadding.setVisibility(isGroupedItem ? VISIBLE : GONE);
            binding.itemGroupToggle.setVisibility(data.type == ListItemType.GROUPHEADER ? VISIBLE : GONE);
            binding.groupExpanded.setVisibility(data.type == ListItemType.GROUPHEADER && isGroupExpanded(data.value) ? VISIBLE : GONE);
            binding.groupReduced.setVisibility(data.type == ListItemType.GROUPHEADER && !isGroupExpanded(data.value) ? VISIBLE : GONE);
            binding.itemAction.setVisibility(data.type == ListItemType.ITEM && data.actionIcon != null ? VISIBLE : GONE);
        }

        private <T> void applyItemView(final SimpleitemlistItemViewBinding itemBinding, final T value, final Func4<T, Context, View, ViewGroup, View> viewMapper) {
            final View currentView = itemBinding.itemViewAnchor.getChildAt(0);
            final View newView = viewMapper.call(value, getContext(), currentView, itemBinding.itemViewAnchor);

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
                        if (item.type == ListItemType.ITEM && isDisplayedSimpleItem(item, true)) {
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
                    toggleGroupExpanded(itemData.value);
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
                    for (int pos = item.groupFirstItemIndex; pos < item.groupFirstItemIndex + item.groupElements.size(); pos++) {
                        if (isDisplayedSimpleItem(getOriginalItems().get(pos), false)) {
                            return true;
                        }
                    }
                    return false;
                case ITEM:
                default:
                    return isDisplayedSimpleItem(item, true);
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
            case SELECTION:
            case FILTER:
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
                    if (isDisplayedSimpleItem(item, true)) {
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

    private boolean isDisplayedSimpleItem(final ListItem simpleItem, final boolean checkGroupExpanded) {
        if (simpleItem == null) {
            return true;
        }
        if (checkGroupExpanded && !isGroupExpanded(simpleItem.group)) {
            return false;
        }
        final String filter = model.getFilterTerm();
        if (StringUtils.isBlank(filter)) {
            return true;
        }
        final String rawText = model.getTextFilterMapper() == null ? null : model.getTextFilterMapper().apply(simpleItem.value);

        return rawText != null && rawText.toLowerCase(Locale.US).contains(filter.trim().toLowerCase(Locale.US));
    }

    private void recreateList() {

        final List<ListItem> list = new ArrayList<>();
        list.add(createForType(ListItemType.SELECT_ALL));
        list.add(createForType(ListItemType.SELECTED_VISIBLE));

        CommonUtils.groupList(model.getItems(),
                model.getGroupingOptions().getGroupMapper(), model.getGroupingOptions().getGroupComparator(),
                model.getGroupingOptions().getHasGroupHeaderMapper(),
                (group, firstItemIdx, elements) -> list.add(createForGroup(group, firstItemIdx + 2, elements)),
                (value, originalIdx, group, groupHeaderIdx) -> list.add(createForItem(value, originalIdx, group, groupHeaderIdx >= 0)));

        listAdapter.setItems(list);

        triggerRefreshList();
    }


}
