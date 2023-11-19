package cgeo.geocaching.ui;

import cgeo.geocaching.R;
import cgeo.geocaching.databinding.SimpleitemlistItemViewBinding;
import cgeo.geocaching.databinding.SimpleitemlistViewBinding;
import cgeo.geocaching.ui.recyclerview.ManagedListAdapter;
import cgeo.geocaching.utils.CommonUtils;
import cgeo.geocaching.utils.LocalizationUtils;

import android.content.Context;
import android.util.AttributeSet;
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

    private SimpleitemlistViewBinding binding;
    private ItemListAdapter listAdapter;

    private SimpleItemListModel<Object> model = new SimpleItemListModel<>();

    private final Set<Object> reducedGroups = new HashSet<>();
    private int currentlyVisible = 0;
    private int currentlyVisibleSelected = 0;
    private boolean itemsHaveIcons = false;
    private boolean groupsHaveIcons = false;

    private static class ListItem {

        public final Object value;

        public final TextParam text;
        public final ImageParam icon;
        public final ImageParam actionIcon;

        public final ListItemType type;

        public final int originalIndex; // only filled for items
        public final Object group; // only filled if isGroupHeader = false and item belongs to a group

        public final int groupFirstItemIndex; // only filled if isGroupHeader = true
        public final int groupCount; // only filled if isGroupHeader = true

        private ListItem(final Object value, final TextParam text, final ImageParam icon, final ImageParam actionIcon, final ListItemType type,
                        final int originalIndex, final Object group, final int groupFirstItemIndex, final int groupCount) {
            this.value = value;
            this.text = text;
            this.icon = icon;
            this.actionIcon = actionIcon;
            this.type = type;
            this.originalIndex = originalIndex;
            this.group = group;
            this.groupFirstItemIndex = groupFirstItemIndex;
            this.groupCount = groupCount;
        }

    }

    private ListItem createForItem(final Object value, final int originalIndex, final Object group) {
        return new ListItem(value,
                model.getDisplayMapper().call(value, originalIndex),
                model.getDisplayIconMapper().call(value, originalIndex),
                model.getActionIconMapper().call(value, originalIndex),
                ListItemType.ITEM, originalIndex, group, -1, -1);
    }

    private ListItem createForGroup(final Object group, final int groupFirstItemIndex, final int groupSize) {
        return new ListItem(group, model.getGroupDisplayMapper().call(group),
                model.getGroupDisplayIconMapper().call(group), null, ListItemType.GROUPHEADER,  -1, -1, groupFirstItemIndex, groupSize);
    }

    private ListItem createForType(final ListItemType type) {
        return new ListItem(null, TextParam.id(R.string.cache_filter_status_select_all), null, null, type,  -1, -1, -1, -1);
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
                    binding.itemText.setText(selectAllText);
                    binding.itemCheckbox.setChecked(model.getSelectedItems().size() == model.getItems().size());
                    binding.itemIcon.setVisibility(GONE);
                    break;
                case SELECTED_VISIBLE:
                    final String selectVisibleText = "<" + LocalizationUtils.getString(R.string.multiselect_selectvisible) + " (" + currentlyVisibleSelected + "/" + currentlyVisible + ")>";
                    binding.itemText.setText(selectVisibleText);
                    binding.itemCheckbox.setChecked(currentlyVisibleSelected == currentlyVisible);
                    binding.itemIcon.setVisibility(GONE);
                    break;
                default:
                    //handling of groups and items is identical
                    if (data.text == null) {
                        binding.itemText.setText("--");
                    } else {
                        data.text.applyTo(binding.itemText);
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

            binding.itemChecker.setVisibility(data.type != ListItemType.ITEM || model.getChoiceMode() != SimpleItemListModel.ChoiceMode.SINGLE_PLAIN ? VISIBLE : GONE);
            binding.itemCheckbox.setVisibility(data.type != ListItemType.GROUPHEADER && model.getChoiceMode() == SimpleItemListModel.ChoiceMode.MULTI_CHECKBOX ? VISIBLE : GONE);
            binding.itemRadiobutton.setVisibility(data.type == ListItemType.ITEM && model.getChoiceMode() == SimpleItemListModel.ChoiceMode.SINGLE_RADIO ? VISIBLE : GONE);
            binding.groupExpanded.setVisibility(data.type == ListItemType.GROUPHEADER && isGroupExpanded(data.value) ? VISIBLE : GONE);
            binding.groupReduced.setVisibility(data.type == ListItemType.GROUPHEADER && !isGroupExpanded(data.value) ? VISIBLE : GONE);
            binding.itemAction.setVisibility(data.type == ListItemType.ITEM && data.actionIcon != null ? VISIBLE : GONE);
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
            final SimpleitemlistItemViewBinding itemBinding = SimpleitemlistItemViewBinding.bind(view);

            final ItemListViewHolder vh = new ItemListViewHolder(itemBinding);
            itemBinding.itemRadiobutton.setClickable(false);
            itemBinding.itemText.setClickable(false);
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
            model.getActionListener().call(itemData.value);
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
                    triggerRefreshList();
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
                model.setSelectedItems(newSelection, true);
            }
        }

        @Override
        public void onBindViewHolder(@NonNull final ItemListViewHolder holder, final int position) {
            holder.binding.itemChecker.setOnClickListener(v -> handleClick(holder.getBindingAdapterPosition()));
            holder.binding.itemText.setOnClickListener(v -> handleClick(holder.getBindingAdapterPosition()));
            holder.binding.itemIcon.setOnClickListener(v -> handleClick(holder.getBindingAdapterPosition()));
            holder.binding.itemAction.setOnClickListener(v -> handleActionClick(holder.getBindingAdapterPosition()));
            holder.fillData(getItem(position));
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
                    for (int pos = item.groupFirstItemIndex; pos < item.groupFirstItemIndex + item.groupCount; pos++) {
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
        super(context);
        init();
    }

    public SimpleItemListView(final Context context, @Nullable final AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public SimpleItemListView(final Context context, @Nullable final AttributeSet attrs, final int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    public SimpleItemListView(final Context context, final AttributeSet attrs, final int defStyleAttr, final int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init();
    }

    private void init() {
        final Context ctw = ViewUtils.wrap(getContext(), R.style.cgeo);
        inflate(ctw, R.layout.simpleitemlist_view, this);
        binding = SimpleitemlistViewBinding.bind(this);
        setOrientation(VERTICAL);

        listAdapter = new ItemListAdapter(binding.list);
        binding.list.setAdapter(listAdapter);

        binding.listFilter.addTextChangedListener(ViewUtils.createSimpleWatcher(e -> triggerRefreshList()));
    }

    @SuppressWarnings("unchecked")
    public void setModel(final SimpleItemListModel<?> model) {
        if (model != null) {
            this.model = (SimpleItemListModel<Object>) model;
            this.model.addChangeListeners(this::handleModelChange);
            handleModelChange(SimpleItemListModel.ChangeType.COMPLETE);
        }
    }

    private void handleModelChange(final SimpleItemListModel.ChangeType changeType) {
        switch (changeType) {
            case COMPLETE:
                recreateList();
                break;
            case SELECTION:
            case SELECTION_BY_USER:
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
        return !reducedGroups.contains(group);
    }

    private void toggleGroupExpanded(final Object group) {
        if (reducedGroups.contains(group)) {
            reducedGroups.remove(group);
        } else {
            reducedGroups.add(group);
        }
    }

    private boolean isDisplayedSimpleItem(final ListItem simpleItem, final boolean checkGroupExpanded) {
        if (simpleItem == null || simpleItem.text == null) {
            return true;
        }
        if (checkGroupExpanded && !isGroupExpanded(simpleItem.group)) {
            return false;
        }
        final String filter = binding.listFilter.getText().toString();
        if (StringUtils.isBlank(filter)) {
            return true;
        }
        final String rawText = simpleItem.text.toString().toLowerCase(Locale.US);
        return rawText.contains(filter.trim().toLowerCase(Locale.US));
    }

    private void recreateList() {

        final List<ListItem> list = new ArrayList<>();
        list.add(createForType(ListItemType.SELECT_ALL));
        list.add(createForType(ListItemType.SELECTED_VISIBLE));

        CommonUtils.groupList(model.getItems(), model.getGroupMapper(), model.getGroupComparator(),
                (group, firstItemIdx, size) -> list.add(createForGroup(group, firstItemIdx + 2, size)),
                (value, originalIdx, group, groupIdx) -> list.add(createForItem(value, originalIdx, group)));

        if (model.getItems().size() >= model.getMinimumItemCountForFilterDisplay()) {
            //show
            binding.listFilterContainer.setVisibility(VISIBLE);
        } else {
            //hide
            binding.listFilter.setText("");
            binding.listFilterContainer.setVisibility(GONE);
        }

        listAdapter.setItems(list);

        triggerRefreshList();
    }


}
