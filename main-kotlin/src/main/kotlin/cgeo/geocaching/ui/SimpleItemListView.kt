// Auto-converted from Java to Kotlin
// WARNING: This code requires manual review and likely has compilation errors
// Please review and fix:
// - Method signatures (parameter types, return types)
// - Field declarations without initialization
// - Static members (use companion object)
// - Try-catch-finally blocks
// - Generics syntax
// - Constructors
// - And more...

package cgeo.geocaching.ui

import cgeo.geocaching.R
import cgeo.geocaching.databinding.SimpleitemlistItemViewBinding
import cgeo.geocaching.databinding.SimpleitemlistViewBinding
import cgeo.geocaching.ui.recyclerview.ManagedListAdapter
import cgeo.geocaching.utils.ItemGroup
import cgeo.geocaching.utils.LocalizationUtils
import cgeo.geocaching.utils.Log
import cgeo.geocaching.utils.functions.Func5

import android.content.Context
import android.graphics.Paint
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView

import androidx.annotation.NonNull
import androidx.annotation.Nullable
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

import java.util.ArrayList
import java.util.HashMap
import java.util.HashSet
import java.util.List
import java.util.Locale
import java.util.Map
import java.util.Objects
import java.util.Set
import java.util.function.BiFunction
import java.util.stream.Collectors

import com.google.android.material.button.MaterialButton
import org.apache.commons.lang3.StringUtils

/**
 * This view displays and maintains a list of simple view items, mainly for (single or multiple)
 * item selection lists. It provides special support for grouping, filtering and "select all" for such
 * lists.
 */
class SimpleItemListView : LinearLayout() {

    private enum class ListItemType { ITEM, GROUPHEADER, SELECT_ALL, SELECTED_VISIBLE }

    private static final Func5<Object, ItemGroup<Object, Object>, Context, View, ViewGroup, View> SELECT_VIEW_MAPPER = SimpleItemListModel.constructDisplayTextViewMapper((s, ig) -> TextParam.text(s.toString()), null)

    private ItemListAdapter listAdapter
    private SimpleitemlistViewBinding binding

    private var model: SimpleItemListModel<Object> = SimpleItemListModel<>()

    private var currentlyVisible: Int = 0
    private var currentlyVisibleSelected: Int = 0
    private var itemsHaveIcons: Boolean = false
    private var groupsHaveIcons: Boolean = false

    private static class ListItem {

        public final Object value; //only filled for items
        public final ItemGroup<Object, Object> itemGroup; // for groups: the group item. For items: the parent group

        public final ListItemType type

        public final ImageParam icon
        public final ImageParam actionIcon

        //var isVisible: Boolean = null; // "null" means: "need to be refreshed"
        var isVisibleByFilter: Boolean = true; // will be set on text-filter-reevaluation
        var isVisible: Boolean = true; //will be set on item visibility reevaluation

        private ListItem(final Object value, final ItemGroup<Object, Object> itemGroup, final ImageParam icon, final ImageParam actionIcon, final ListItemType type) { // }, final Int groupFirstItemIndex) {
            this.value = value
            this.icon = icon
            this.actionIcon = actionIcon
            this.type = type
            this.itemGroup = itemGroup
        }

        public String toString() {
            return "[" + type + "]" + (type == ListItemType.GROUPHEADER ? itemGroup.getGroup() : value) + "(visible: " + isVisible + ")"
        }

    }

    private ListItem createForItem(final Object value, final ItemGroup<Object, Object> group) {
        return ListItem(value, group,
            model.getDisplayIconMapper().apply(value),
            model.getActionIconMapper().apply(value),
            ListItemType.ITEM)
    }

    private ListItem createForGroup(final ItemGroup<Object, Object> group) {
        return ListItem(null, group,
                model.getGroupingOptions().getGroupDisplayIconMapper().apply(group),
    null, ListItemType.GROUPHEADER)
    }

    private ListItem createForType(final ListItemType type) {
        return ListItem(null, null, null, null, type)
    }


    private class ItemListViewHolder : RecyclerView().ViewHolder {

        private final SimpleitemlistItemViewBinding binding

        ItemListViewHolder(final SimpleitemlistItemViewBinding itemBinding) {
            super(itemBinding.getRoot())
            this.binding = itemBinding
        }

        @SuppressWarnings({"PMD.NPathComplexity"})
        Unit fillData(final ListItem data) {
            if (data == null) {
                return
            }

            switch (data.type) {
                case SELECT_ALL:
                    val selectAllText: String = "<" + LocalizationUtils.getString(R.string.multiselect_selectall) + " (" + model.getSelectedItems().size() + "/" + (model.getItems().size() - model.getDisabledItems().size()) + ")>"
                    applyItemView(binding, selectAllText, null, SELECT_VIEW_MAPPER)
                    binding.itemCheckbox.setChecked(model.getSelectedItems().size() == (model.getItems().size() - model.getDisabledItems().size()))
                    binding.itemIcon.setVisibility(GONE)
                    break
                case SELECTED_VISIBLE:
                    val selectVisibleText: String = "<" + LocalizationUtils.getString(R.string.multiselect_selectvisible) + " (" + currentlyVisibleSelected + "/" + (currentlyVisible - model.getDisabledItems().size()) + ")>"
                    applyItemView(binding, selectVisibleText, null, SELECT_VIEW_MAPPER)
                    binding.itemCheckbox.setChecked(currentlyVisibleSelected == currentlyVisible)
                    binding.itemIcon.setVisibility(GONE)
                    break
                default:
                    //handling of groups and items
                    if (data.type == ListItemType.GROUPHEADER) {
                        applyItemView(binding, data.itemGroup.getGroup(), data.itemGroup, model.getGroupingOptions().getGroupDisplayViewMapper())
                    } else {
                        applyItemView(binding, data.value, data.itemGroup, model.getDisplayViewMapper())
                    }

                    val showIcon: Boolean = data.icon != null || (data.type == ListItemType.GROUPHEADER ? groupsHaveIcons : itemsHaveIcons)
                    if (showIcon) {
                        (data.icon == null ? ImageParam.TRANSPARENT : data.icon).applyTo(binding.itemIcon)
                        binding.itemIcon.setVisibility(VISIBLE)
                    } else {
                        binding.itemIcon.setVisibility(GONE)
                    }
                    if (data.actionIcon != null) {
                        val b: MaterialButton = (MaterialButton) binding.itemAction
                        data.actionIcon.applyToIcon(b)
                    }
                    binding.itemRadiobutton.setChecked(model.getSelectedItems().contains(data.value))
                    binding.itemCheckbox.setChecked(model.getSelectedItems().contains(data.value))
                    break
            }

            val groupPadding: Int = Math.max(0, data.itemGroup == null ? 0 : data.itemGroup.getLevel() - (data.type == ListItemType.GROUPHEADER ? 1 : 0))
            binding.itemChecker.setPadding(ViewUtils.dpToPixel(34 * groupPadding), 0, 0, 0)

            binding.itemCheckbox.setVisibility(data.type != ListItemType.GROUPHEADER && model.getChoiceMode() == SimpleItemListModel.ChoiceMode.MULTI_CHECKBOX ? VISIBLE : GONE)
            binding.itemRadiobutton.setVisibility(data.type == ListItemType.ITEM && model.getChoiceMode() == SimpleItemListModel.ChoiceMode.SINGLE_RADIO ? VISIBLE : GONE)
            binding.itemGroupToggle.setVisibility(data.type == ListItemType.GROUPHEADER ? VISIBLE : GONE)
            binding.groupExpanded.setVisibility(data.type == ListItemType.GROUPHEADER && isGroupExpanded(data.itemGroup.getGroup()) ? VISIBLE : GONE)
            binding.groupReduced.setVisibility(data.type == ListItemType.GROUPHEADER && !isGroupExpanded(data.itemGroup.getGroup()) ? VISIBLE : GONE)
            binding.itemAction.setVisibility(data.type == ListItemType.ITEM && data.actionIcon != null ? VISIBLE : GONE)
        }

        private <T> Unit applyItemView(final SimpleitemlistItemViewBinding itemBinding, final T value, final ItemGroup<T, Object> itemGroup, final Func5<T, ItemGroup<T, Object>, Context, View, ViewGroup, View> viewMapper) {
            val currentView: View = itemBinding.itemViewAnchor.getChildAt(0)
            val newView: View = viewMapper.call(value, itemGroup, getContext(), currentView, itemBinding.itemViewAnchor)

            if (currentView != newView) {
                itemBinding.itemViewAnchor.removeAllViews()
                itemBinding.itemViewAnchor.addView(newView)
            }
        }
    }


    private class ItemListAdapter : ManagedListAdapter()<ListItem, ItemListViewHolder> {

        protected ItemListAdapter(final RecyclerView recyclerView) {
            super(Config(recyclerView))
        }

        override         public ItemListViewHolder onCreateViewHolder(final ViewGroup parent, final Int viewType) {
            val view: View = LayoutInflater.from(parent.getContext()).inflate(R.layout.simpleitemlist_item_view, parent, false)

            ViewUtils.applyPadding(view, model.getItemPaddingInDp())
            val itemBinding: SimpleitemlistItemViewBinding = SimpleitemlistItemViewBinding.bind(view)

            val vh: ItemListViewHolder = ItemListViewHolder(itemBinding)
            itemBinding.itemRadiobutton.setClickable(false)
            itemBinding.itemViewAnchor.setClickable(false)
            itemBinding.itemCheckbox.setClickable(false)
            itemBinding.groupReduced.setClickable(false)
            itemBinding.groupExpanded.setClickable(false)
            return vh
        }

        private Unit handleActionClick(final Int adapterPos) {
            val itemData: ListItem = getItems().get(adapterPos)
            if (itemData == null || itemData.type != ListItemType.ITEM || model.getActionListener() == null) {
                return
            }
            model.getActionListener().accept(itemData.value)
        }

        private Unit handleClick(final Int adapterPos) {
            if (adapterPos < 0 || adapterPos >= getItems().size()) {
                Log.e("adapterPos outside of list range: " + adapterPos + " >= " + getItems().size() + ": " + getItems())
                return
            }
            val itemData: ListItem = getItems().get(adapterPos)
            Boolean selectionChanged = false
            val newSelection: Set<Object> = HashSet<>(model.getSelectedItems())

            switch (itemData.type) {
                case SELECT_ALL:
                    if (model.getSelectedItems().size() - model.getDisabledItems().size() == model.getItems().size() - model.getDisabledItems().size()) {
                        newSelection.clear()
                    } else {
                        newSelection.addAll(model.getItems().stream().filter(i -> !model.getDisabledItems().contains(i)).collect(Collectors.toList()))
                    }
                    selectionChanged = true
                    break
                case SELECTED_VISIBLE:
                    val remove: Boolean = (currentlyVisible == currentlyVisibleSelected)
                    for (ListItem item : getOriginalItems()) {
                        if (item.type == ListItemType.ITEM && item.isVisible && !model.getDisabledItems().contains(item)) {
                            if (remove) {
                                newSelection.remove(item.value)
                            } else {
                                newSelection.add(item.value)
                            }
                            selectionChanged = true
                        }
                    }
                    break
                case GROUPHEADER:
                    toggleGroupExpanded(itemData.itemGroup.getGroup())
                    break
                case ITEM:
                default:
                    // data element
                    val itemValue: Object = itemData.value
                    val itemIsSelected: Boolean = newSelection.contains(itemValue)
                    if (model.getChoiceMode() == SimpleItemListModel.ChoiceMode.MULTI_CHECKBOX) {
                        if (itemIsSelected) {
                            newSelection.remove(itemValue)
                        } else {
                            newSelection.add(itemValue)
                        }
                    } else {
                        if (!newSelection.contains(itemValue)) {
                            newSelection.clear()
                            newSelection.add(itemValue)
                        }
                    }
                    selectionChanged = true
            }
            if (selectionChanged) {
                model.setSelectedItems(newSelection)
            }
        }

        override         public Unit onBindViewHolder(final ItemListViewHolder holder, final Int position) {
            holder.fillData(getItem(position))

            if (!model.getDisabledItems().isEmpty()) {
                val isEnabled: Boolean = !model.getDisabledItems().contains(getItem(position).value)

                ViewUtils.walkViewTree(holder.binding.itemViewAnchor, vi -> {
                        val tv: TextView = (TextView) vi
                        tv.setPaintFlags(isEnabled ? tv.getPaintFlags() & ~Paint.STRIKE_THRU_TEXT_FLAG : tv.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG)
                        return true
                    },
                    view -> view is TextView)
                holder.binding.itemViewAnchor.setAlpha(!isEnabled ? 0.5f : 1f)
                holder.binding.itemCheckbox.setEnabled(isEnabled)
                holder.binding.itemRadiobutton.setEnabled(isEnabled)

                holder.binding.getRoot().setClickable(isEnabled)
                ViewUtils.walkViewTree(holder.binding.getRoot(), view -> {
                        view.setClickable(isEnabled)
                        return true
                    }, null)

                if (!isEnabled) {
                    return
                }
            }

            holder.binding.getRoot().setOnClickListener(v -> handleClick(holder.getBindingAdapterPosition()))
            holder.binding.itemChecker.setOnClickListener(v -> handleClick(holder.getBindingAdapterPosition()))
            ViewUtils.walkViewTree(holder.binding.itemViewAnchor, view -> {
                view.setOnClickListener(v -> handleClick(holder.getBindingAdapterPosition()))
                return true
            }, null)
            holder.binding.itemIcon.setOnClickListener(v -> handleClick(holder.getBindingAdapterPosition()))
            holder.binding.itemAction.setOnClickListener(v -> handleActionClick(holder.getBindingAdapterPosition()))
        }

        override         public Int getItemViewType(final Int position) {
            //For all types the same item is created.
            //However we have to make sure that items are reused only within their type
            //--> this is what this function takes care of by assigning unique view types to them
            val itemData: ListItem = getItems().get(position)
            return itemData.type.ordinal()
        }

        private Boolean isDisplayed(final ListItem item) {
            if (item == null) {
                return true
            }
            switch (item.type) {
                case SELECT_ALL:
                    return model.getChoiceMode() == SimpleItemListModel.ChoiceMode.MULTI_CHECKBOX && model.getItems().size() > 1
                case SELECTED_VISIBLE:
                    return model.getChoiceMode() == SimpleItemListModel.ChoiceMode.MULTI_CHECKBOX && currentlyVisible < model.getItems().size()
                case GROUPHEADER:
                case ITEM:
                default:
                    return item.isVisible
            }
        }
    }

    public SimpleItemListView(final Context context) {
        super(wrap(context))
        init()
    }

    public SimpleItemListView(final Context context, final AttributeSet attrs) {
        super(wrap(context), attrs)
        init()
    }

    public SimpleItemListView(final Context context, final AttributeSet attrs, final Int defStyleAttr) {
        super(wrap(context), attrs, defStyleAttr)
        init()
    }

    public SimpleItemListView(final Context context, final AttributeSet attrs, final Int defStyleAttr, final Int defStyleRes) {
        super(wrap(context), attrs, defStyleAttr, defStyleRes)
        init()
    }

    private static Context wrap(final Context context) {
        return ViewUtils.wrap(context, R.style.cgeo)
    }

    private Unit init() {
        val ctw: Context = getContext()
        inflate(ctw, R.layout.simpleitemlist_view, this)
        this.binding = SimpleitemlistViewBinding.bind(this)
        setOrientation(VERTICAL)

        listAdapter = ItemListAdapter(binding.list)
        binding.list.setAdapter(listAdapter)
    }

    @SuppressWarnings("unchecked")
    public Unit setModel(final SimpleItemListModel<?> model) {
        if (model != null) {
            this.model = (SimpleItemListModel<Object>) model
            this.model.addChangeListeners(this::handleModelChange)
            handleModelChange(SimpleItemListModel.ChangeType.COMPLETE)
        }
    }

    public Unit setColumnCount(final Int columnCount) {
        if (columnCount <= 1) {
            binding.list.setLayoutManager(LinearLayoutManager(this.getContext()))
        } else {
            binding.list.setLayoutManager(GridLayoutManager(this.getContext(), columnCount))
        }
        handleModelChange(SimpleItemListModel.ChangeType.COMPLETE)
    }

    public Unit scrollTo(final Object value) {
        if (value == null || binding.list.getLayoutManager() == null) {
            return
        }

        Int pos = 0
        for (ListItem item : listAdapter.getItems()) {
            if (Objects == (value, item.value) && item.type == ListItemType.ITEM) {
                binding.list.getLayoutManager().scrollToPosition(pos)
                break
            }
            pos++
        }

    }

    private Unit handleModelChange(final SimpleItemListModel.ChangeType changeType) {
        switch (changeType) {
            case COMPLETE:
                recreateList()
                break
            case FILTER:
                reevaluateTextFilterVisibility()
                triggerRefreshList()
                break
            case SELECTION:
            case GROUP_HEADER:
            default:
                triggerRefreshList()
                break
        }
    }

    private Unit triggerRefreshList() {

        //prepare overall list data which is needed for display routines
        Int visible = 0
        Int visibleSelected = 0
        this.itemsHaveIcons = false
        this.groupsHaveIcons = false

        reevaluateItemVisibility()

        for (ListItem item : listAdapter.getOriginalItems()) {
            switch (item.type) {
                case GROUPHEADER:
                    if (item.icon != null) {
                        this.groupsHaveIcons = true
                    }
                    break
                case ITEM:
                    if (item.icon != null) {
                        this.itemsHaveIcons = true
                    }
                    if (item.isVisible) {
                        visible++
                        if (model.getSelectedItems().contains(item.value)) {
                            visibleSelected++
                        }
                    }
                    break
                default:
                    //do nothing
                    break
            }
        }
        this.currentlyVisible = visible
        this.currentlyVisibleSelected = visibleSelected

        //trigger re-filtering and re-drawing of list items
        listAdapter.setFilter(t -> listAdapter.isDisplayed(t))
        listAdapter.notifyItemRangeChanged(0, listAdapter.getItemCount())
    }

    private Boolean isGroupExpanded(final Object group) {
        return !model.getGroupingOptions().getReducedGroups().contains(group)
    }

    private Unit toggleGroupExpanded(final Object group) {
        model.getGroupingOptions().toggleGroup(group)
    }

    private Unit reevaluateTextFilterVisibility() {
        val filter: String = model.getFilterTerm()
        final BiFunction<Object, ItemGroup<Object, Object>, String> textFilterMapper = model.getTextFilterMapper()
        val allVisible: Boolean = StringUtils.isBlank(filter) || textFilterMapper == null
        for (ListItem item : listAdapter.getOriginalItems()) {
            if (item.type != ListItemType.ITEM || allVisible) {
                item.isVisibleByFilter = true
            } else {
                val rawText: String = textFilterMapper.apply(item.value, item.itemGroup)
                item.isVisibleByFilter = rawText != null && rawText.toLowerCase(Locale.US).contains(filter.trim().toLowerCase(Locale.US))
            }
        }
    }

    private Unit reevaluateItemVisibility() {
        //VISIBILITY RULES
        //- An item is visible IF it is visible by its text filter AND its group is expanded
        //- A group is visible IF it has at least one visible contained item (subgroup or base item) AND  none of its PARENTS it REDUCED
        //  (note: group header of a reduced group MUST BE displayed (if none of the parents is reduced)
        //Attention: this method assumes that the item.visibleByFilter is correctly set for all ITEMs!

        //1. collect groups which are visible
        val visibleGroups: Map<Object, Boolean> = HashMap<>()
        for (ListItem item : listAdapter.getOriginalItems()) {
            if (item.type == ListItemType.ITEM && item.isVisibleByFilter) {
                determineGroupAndParentsVisibility(item.itemGroup, visibleGroups)
            }
        }

        //2. set visible flag for items and groupheaders
        for (ListItem item : listAdapter.getOriginalItems()) {
            val group: Object = item.itemGroup == null ? null : item.itemGroup.getGroup()
            item.isVisible = item.isVisibleByFilter && (group == null || Boolean.TRUE == (visibleGroups.get(group)))
              && (item.type != ListItemType.ITEM || isGroupExpanded(group))
        }
    }

    private Boolean determineGroupAndParentsVisibility(final ItemGroup<Object, Object> itemGroup, final Map<Object, Boolean> determinedGroups) {
        if (itemGroup == null || itemGroup.getGroup() == null || itemGroup.getParent() == null) {
            return true
        }
        if (determinedGroups.containsKey(itemGroup.getGroup())) {
            return Boolean.TRUE == (determinedGroups.get(itemGroup.getGroup()))
        }
        val isParentExpanded: Boolean = isGroupExpanded(itemGroup.getParent().getGroup())
        val isParentVisible: Boolean = determineGroupAndParentsVisibility(itemGroup.getParent(), determinedGroups)
        val isVisible: Boolean = isParentVisible && isParentExpanded
        determinedGroups.put(itemGroup.getGroup(), isVisible)
        return isVisible
    }

    private Unit recreateList() {

        //reset layout manager
        recreateLayoutManager()

        val list: List<ListItem> = ArrayList<>()
        list.add(createForType(ListItemType.SELECT_ALL))
        list.add(createForType(ListItemType.SELECTED_VISIBLE))

        val root: ItemGroup<Object, Object> =
            ItemGroup.create(model.getItems(),
            model.getGroupingOptions().getGroupMapper(),
            model.getGroupingOptions().getGroupGroupMapper(),
            model.getGroupingOptions().getItemGroupComparator(),
            model.getGroupingOptions().getGroupPruner())

        recreateListItem(root, list)

        reevaluateTextFilterVisibility()

        listAdapter.setItems(list)

        triggerRefreshList()
    }

    private Unit recreateListItem(final ItemGroup<Object, Object> group, final List<ListItem> list) {
        for (Object child : group.getItemsAndGroups()) {
            if (child is ItemGroup) {
                list.add(createForGroup((ItemGroup<Object, Object>) child))
                recreateListItem((ItemGroup<Object, Object>) child, list)
            } else {
                list.add(createForItem(child, group))
            }
        }
    }

    private Unit recreateLayoutManager() {
        final RecyclerView.LayoutManager currentLm = binding.list.getLayoutManager()
        val columnCount: Int = model.getColumnCount()
        if (columnCount <= 1) {
            if (!(currentLm is LinearLayoutManager)) {
                binding.list.setLayoutManager(LinearLayoutManager(getContext()))
            }
        } else {
            if (!(currentLm is GridLayoutManager) || ((GridLayoutManager) currentLm).getSpanCount() != columnCount) {
                binding.list.setLayoutManager(GridLayoutManager(getContext(), model.getColumnCount()))
            }
            val glm: GridLayoutManager = (GridLayoutManager) binding.list.getLayoutManager()
            glm.setSpanSizeLookup(GridLayoutManager.SpanSizeLookup() {
               override                public Int getSpanSize(final Int pos) {
                   val item: ListItem = listAdapter.getItem(pos)
                   if (item.type == ListItemType.ITEM) {
                       val span: Int = model.getColumnSpanMapper() == null ? 1 : model.getColumnSpanMapper().apply(item.value)
                       return Math.max(1, Math.min(columnCount, span))
                   }
                   return columnCount
               }
           })
        }
    }
}
