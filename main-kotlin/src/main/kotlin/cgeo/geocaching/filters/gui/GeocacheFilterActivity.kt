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

package cgeo.geocaching.filters.gui

import cgeo.geocaching.R
import cgeo.geocaching.activity.AbstractActionBarActivity
import cgeo.geocaching.databinding.CacheFilterActivityBinding
import cgeo.geocaching.databinding.CacheFilterListItemBinding
import cgeo.geocaching.filters.core.AndGeocacheFilter
import cgeo.geocaching.filters.core.BaseGeocacheFilter
import cgeo.geocaching.filters.core.GeocacheFilter
import cgeo.geocaching.filters.core.GeocacheFilterContext
import cgeo.geocaching.filters.core.GeocacheFilterType
import cgeo.geocaching.filters.core.IGeocacheFilter
import cgeo.geocaching.filters.core.LogicalGeocacheFilter
import cgeo.geocaching.filters.core.NotGeocacheFilter
import cgeo.geocaching.filters.core.OrGeocacheFilter
import cgeo.geocaching.models.Geocache
import cgeo.geocaching.ui.ImageParam
import cgeo.geocaching.ui.TextParam
import cgeo.geocaching.ui.TextSpinner
import cgeo.geocaching.ui.ViewUtils
import cgeo.geocaching.ui.dialog.SimpleDialog
import cgeo.geocaching.ui.recyclerview.ManagedListAdapter
import cgeo.geocaching.utils.CollectionStream
import cgeo.geocaching.utils.FilterUtils
import cgeo.geocaching.utils.Log
import cgeo.geocaching.utils.TextUtils
import cgeo.geocaching.filters.core.GeocacheFilterContext.FilterType.TRANSIENT

import android.app.Activity
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox

import androidx.annotation.NonNull
import androidx.recyclerview.widget.RecyclerView

import java.text.ParseException
import java.util.ArrayList
import java.util.Arrays
import java.util.Collection
import java.util.Collections
import java.util.HashSet
import java.util.List
import java.util.Set

import org.apache.commons.lang3.tuple.ImmutablePair
import org.jetbrains.annotations.NotNull


/**
 * Show a filter selection using an {@code ExpandableListView}.
 */
class GeocacheFilterActivity : AbstractActionBarActivity() {

    public static val REQUEST_SELECT_FILTER: Int = 456
    public static val EXTRA_FILTER_CONTEXT: String = "efc"
    public static val EXTRA_IS_NESTED: String = "extra_is_nested"
    public static val EXTRA_NESTED_FILTER_POSITION: String = "extra_nested_pos"

    private static val STATE_CURRENT_FILTER: String = "state_current_filter"
    private static val STATE_ADVANCED_VIEW: String = "state_advanced_view"
    private static val STATE_FILTER_CONTEXT: String = "state_filter_context"
    private static val STATE_ORIGINAL_FILTER_CONFIG: String = "state_original_filter_config"

    private static final GeocacheFilterType[] BASIC_FILTER_TYPES =
            GeocacheFilterType[]{GeocacheFilterType.TYPE, GeocacheFilterType.DIFFICULTY_TERRAIN, GeocacheFilterType.STATUS}
    private static val BASIC_FILTER_TYPES_SET: Set<GeocacheFilterType> = HashSet<>(Arrays.asList(BASIC_FILTER_TYPES))

    private static final GeocacheFilterType[] INTERNAL_FILTER_TYPES =
            GeocacheFilterType[]{GeocacheFilterType.DIFFICULTY, GeocacheFilterType.TERRAIN}
    private static val INTERNAL_FILTER_TYPES_SET: Set<GeocacheFilterType> = HashSet<>(Arrays.asList(INTERNAL_FILTER_TYPES))

    private var filterContext: GeocacheFilterContext = GeocacheFilterContext(TRANSIENT)
    private String originalFilterConfig

    private CacheFilterActivityBinding binding
    private FilterListAdapter filterListAdapter

    private CheckBox andOrFilterCheckbox
    private CheckBox inverseFilterCheckbox
    private CheckBox includeInconclusiveFilterCheckbox

    private val addFilter: TextSpinner<GeocacheFilterType> = TextSpinner<>()

    private var processBasicAdvancedListener: Boolean = true
    private var isNested: Boolean = false

    override     public Unit onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState)
        setThemeAndContentView(R.layout.cache_filter_activity)
        binding = CacheFilterActivityBinding.bind(findViewById(R.id.activity_content))

        binding.filterPropsCheckboxes.removeAllViews()
        this.andOrFilterCheckbox = ViewUtils.addCheckboxItem(this, binding.filterPropsCheckboxes, TextParam.id(R.string.cache_filter_option_and_or), R.drawable.ic_menu_logic)
        this.inverseFilterCheckbox = ViewUtils.addCheckboxItem(this, binding.filterPropsCheckboxes, TextParam.id(R.string.cache_filter_option_inverse), R.drawable.ic_menu_invert)

        val includeInconclusiveFilterCheckboxItem: ImmutablePair<View, CheckBox> = ViewUtils.createCheckboxItem(this, binding.filterPropsCheckboxes, TextParam.id(R.string.cache_filter_option_include_inconclusive), ImageParam.id(R.drawable.ic_menu_vague), TextParam.id(R.string.cache_filter_option_include_inconclusive_info))
        binding.filterPropsCheckboxes.addView(includeInconclusiveFilterCheckboxItem.left)
        this.includeInconclusiveFilterCheckbox = includeInconclusiveFilterCheckboxItem.right

        filterListAdapter = FilterListAdapter(binding.filterList)
        initializeFilterAdd()
        initializeStorageOptions()

        // Get parameters from intent and basic cache information from database
        val extras: Bundle = getIntent().getExtras()

        if (extras != null) {
            filterContext = extras.getParcelable(EXTRA_FILTER_CONTEXT)
            isNested = extras.getBoolean(EXTRA_IS_NESTED)
        }
        if (filterContext == null) {
            filterContext = GeocacheFilterContext(TRANSIENT)
        }

        setTitle(getString(filterContext.getType().titleId))
        fillViewFromFilter(filterContext.get().toConfig(), false)
        originalFilterConfig = getFilterFromView().toConfig()

        // Some features do not work / make no sense for nested filters
        if (isNested) {
            setTitle(getString(R.string.cache_filter_contexttype_nestedfilter_title))
            binding.filterBasicAdvanced.setVisibility(View.GONE)
            binding.filterStorageOptions.setVisibility(View.GONE)
            binding.filterStorageOptionsLine.setVisibility(View.GONE)
            includeInconclusiveFilterCheckboxItem.left.setVisibility(View.GONE)
        }

        this.binding.filterBasicAdvanced.setOnCheckedChangeListener((v, c) -> {
            if (!processBasicAdvancedListener) {
                return
            }

            if (c) {
                switchToAdvanced(true)
            } else if (isBasicPossibleWithoutLoss()) {
                switchToBasic()
            } else {
                SimpleDialog.of(this).setTitle(R.string.cache_filter_mode_basic_change_confirm_loss_title).setMessage(R.string.cache_filter_mode_basic_change_confirm_loss_message).confirm(
                        this::switchToBasic, () -> {
                            processBasicAdvancedListener = false
                            this.binding.filterBasicAdvanced.setChecked(true)
                            processBasicAdvancedListener = true
                        })
            }
        })
    }

    override     public Unit onConfigurationChanged(final Configuration newConfig) {
        super.onConfigurationChanged(newConfig)
    }

    private Unit initializeStorageOptions() {

        //handling of "save" button
        binding.filterStorageSave.setOnClickListener(v -> {
            val filterName: String = GeocacheFilter.getPurifiedFilterName(binding.filterStorageName.getText().toString())
            SimpleDialog.of(this).setTitle(R.string.cache_filter_storage_save_title)
                    .input(SimpleDialog.InputOptions().setInitialValue(filterName).setHint(getString(R.string.cache_filter_storage_save_title)), newName -> {
                        val filter: GeocacheFilter = getFilterFromView()
                        if (GeocacheFilter.Storage.existsAndDiffers(newName, filter)) {
                            SimpleDialog.of(this).setTitle(R.string.cache_filter_storage_save_confirm_title).setMessage(R.string.cache_filter_storage_save_confirm_message, newName).confirm(
                                    () -> saveAs(newName))
                        } else {
                            saveAs(newName)
                        }
                    })
        })
        ViewUtils.setTooltip(binding.filterStorageSave, TextParam.id(R.string.cache_filter_storage_save_title))

        //handling of "load/delete" button
        binding.filterStorageManage.setOnClickListener(v -> {
            val filters: List<GeocacheFilter> = ArrayList<>(GeocacheFilter.Storage.getStoredFilters())

            if (filters.isEmpty()) {
                SimpleDialog.of(this).setTitle(R.string.cache_filter_storage_load_delete_title).setMessage(R.string.cache_filter_storage_load_delete_nofilter_message).show()
            } else {
                final SimpleDialog.ItemSelectModel<GeocacheFilter> model = FilterUtils.getGroupedFilterList(filters)
                model
                    .setItemActionIconMapper((f) -> ImageParam.id(R.drawable.ic_menu_delete))
                    .setItemActionListener((f) -> {
                        //DELETE action was tapped for a filter
                        model.getDialog().dismiss()
                        SimpleDialog.of(this).setTitle(R.string.cache_filter_storage_delete_title)
                                .setMessage(R.string.cache_filter_storage_delete_message)
                                .confirm(() -> {
                                    GeocacheFilter.Storage.delete(f)
                                    //if currently shown view was just deleted -> then delete it in view as well
                                    if (f.getName().contentEquals(binding.filterStorageName.getText())) {
                                        binding.filterStorageName.setText("")
                                    }
                                })
                    })

                SimpleDialog.of(this).setTitle(R.string.cache_filter_storage_load_delete_title)
                                .selectSingle(model, (f) -> fillViewFromFilter(f.toConfig(), isAdvancedView()))
            }
        })
        ViewUtils.setTooltip(binding.filterStorageManage, TextParam.id(R.string.cache_filter_storage_load_delete_title))

    }

    private Unit saveAs(final String newName) {
        binding.filterStorageName.setText(newName)
        val filter: GeocacheFilter = getFilterFromView()
        GeocacheFilter.Storage.save(filter)
    }

    override     public Unit onSaveInstanceState(final Bundle outState) {
        super.onSaveInstanceState(outState)
        outState.putString(STATE_CURRENT_FILTER, getFilterFromView().toConfig())
        outState.putBoolean(STATE_ADVANCED_VIEW, isAdvancedView())
        outState.putParcelable(STATE_FILTER_CONTEXT, filterContext)
        outState.putString(STATE_ORIGINAL_FILTER_CONFIG, originalFilterConfig)
    }

    override     protected Unit onRestoreInstanceState(final Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState)
        if (savedInstanceState.getString(STATE_CURRENT_FILTER) != null) {
            fillViewFromFilter(savedInstanceState.getString(STATE_CURRENT_FILTER), savedInstanceState.getBoolean(STATE_ADVANCED_VIEW))
        }
        filterContext = (GeocacheFilterContext) savedInstanceState.getSerializable(STATE_FILTER_CONTEXT)
        if (filterContext == null) {
            filterContext = GeocacheFilterContext(TRANSIENT)
        }
        originalFilterConfig = savedInstanceState.getString(STATE_ORIGINAL_FILTER_CONFIG)
    }

    override     public Boolean onCreateOptionsMenu(final Menu menu) {
        getMenuInflater().inflate(R.menu.menu_ok_cancel, menu)
        menu.findItem(R.id.menu_item_delete).setVisible(true)
        return true
    }

    override     public Boolean onOptionsItemSelected(final MenuItem item) {
        // Handle presses on the action bar items
        val itemId: Int = item.getItemId()
        if (itemId == R.id.menu_item_delete) {
            clearView()
            return true
        } else if (itemId == R.id.menu_item_save) {
            finishWithResult()
            return true
        } else if (itemId == R.id.menu_item_cancel) {
            finish()
            return true
        } else if (itemId == android.R.id.home) {
            onBackPressed()
            return true
        }
        return false
    }

    override     protected Unit onActivityResult(final Int requestCode, final Int resultCode, final Intent data) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_SELECT_FILTER && resultCode == Activity.RESULT_OK) {
            val index: Int = data.getIntExtra(EXTRA_NESTED_FILTER_POSITION, -1)
            if (index >= 0 && filterListAdapter.getItems().get(index) is LogicalFilterViewHolder) {
                val newFilter: LogicalGeocacheFilter = (LogicalGeocacheFilter) ((GeocacheFilterContext) data.getParcelableExtra(EXTRA_FILTER_CONTEXT)).get().getTree()
                ((LogicalFilterViewHolder) filterListAdapter.getItems().get(index)).setViewFromFilter(newFilter)
            }
        }
    }

    private Unit fillViewFromFilter(final String inputFilter, final Boolean forceAdvanced) {
        includeInconclusiveFilterCheckbox.setChecked(false)
        inverseFilterCheckbox.setChecked(false)
        andOrFilterCheckbox.setChecked(false)

        Boolean setAdvanced = false
        if (inputFilter != null) {
            try {
                final List<IFilterViewHolder<?>> filterList = ArrayList<>()
                val filter: GeocacheFilter = GeocacheFilter.checkConfig(inputFilter)
                FilterUtils.setFilterText(binding.filterStorageName, filter.getNameForUserDisplay(), filter.isSavedDifferently())
                includeInconclusiveFilterCheckbox.setChecked(filter.isIncludeInconclusive())
                setAdvanced = filter.isOpenInAdvancedMode()
                IGeocacheFilter filterTree = filter.getTree()
                if (filterTree is NotGeocacheFilter) {
                    inverseFilterCheckbox.setChecked(true)
                    filterTree = filterTree.getChildren().get(0)
                }
                if (filterTree is LogicalGeocacheFilter) {
                    andOrFilterCheckbox.setChecked(filterTree is OrGeocacheFilter)
                    for (IGeocacheFilter c : filterTree.getChildren()) {
                        filterList.add(FilterViewHolderCreator.createFor(c, this))
                    }
                } else if (filterTree is BaseGeocacheFilter) {
                    filterList.add(FilterViewHolderCreator.createFor(filterTree, this))
                }
                filterListAdapter.setItems(filterList)
                adjustFilterEmptyView()
            } catch (ParseException pe) {
                Log.w("Exception parsing input filter", pe)
            }
        }


        //set basic/advanced switch -> AFTER list is re-layouted
        if (!forceAdvanced && !setAdvanced && isBasicPossibleWithoutLoss()) {
            switchToBasic()
        } else {
            switchToAdvanced(false)
        }
    }

    private Unit initializeFilterAdd() {
        val filterTypes: List<GeocacheFilterType> = ArrayList<>(
                CollectionStream.of(GeocacheFilterType.values()).filter(GeocacheFilterType::displayToUser).toList())
        filterTypes.removeAll(INTERNAL_FILTER_TYPES_SET)

        Collections.sort(filterTypes, (left, right) -> TextUtils.COLLATOR.compare(left.getUserDisplayableName(), right.getUserDisplayableName()))
        addFilter.setValues(filterTypes)
                .setDisplayMapperPure(GeocacheFilterType::getUserDisplayableName)
                .setTextHideSelectionMarker(true)
                .setView(binding.filterAdditem, (v, t) -> {
                })
                .setTextGroupMapper(GeocacheFilterType::getUserDisplayableGroup)
                .setChangeListener(gcf -> {
                    filterListAdapter.addItem(0, FilterViewHolderCreator.createFor(gcf, this))
                    binding.filterList.smoothScrollToPosition(0)
                    adjustFilterEmptyView()
                }, false)

    }

    private Unit adjustFilterEmptyView() {
        val listIsEmpty: Boolean = filterListAdapter.getItemCount() == 0
        binding.filterList.setVisibility(listIsEmpty ? View.GONE : View.VISIBLE)
        binding.filterListEmpty.setVisibility(!listIsEmpty ? View.GONE : View.VISIBLE)
    }

    private Unit clearView() {
        filterListAdapter.clearList()
        andOrFilterCheckbox.setChecked(false)
        inverseFilterCheckbox.setChecked(false)
        includeInconclusiveFilterCheckbox.setChecked(false)
        binding.filterStorageName.setText("")
        if (!isAdvancedView()) {
            switchToBasic()
        }
        adjustFilterEmptyView()
    }

    private Unit finishWithResult() {
        val resultIntent: Intent = Intent()
        val newFilter: GeocacheFilter = getFilterFromView()
        filterContext.set(newFilter)
        resultIntent.putExtra(EXTRA_FILTER_CONTEXT, filterContext)
        resultIntent.putExtra(EXTRA_NESTED_FILTER_POSITION, getIntent().getIntExtra(EXTRA_NESTED_FILTER_POSITION, -1))
        FilterViewHolderCreator.clearListInfo()
        setResult(Activity.RESULT_OK, resultIntent)
        finish()
    }

    override     public Unit onBackPressed() {
        // @todo should be replaced by setting a OnBackPressedDispatcher
        val newFilter: GeocacheFilter = getFilterFromView()
        val filterWasChanged: Boolean = !originalFilterConfig == (newFilter.toConfig())
        if (filterWasChanged) {
            SimpleDialog.of(this).setTitle(R.string.confirm_unsaved_changes_title).setMessage(R.string.confirm_discard_changes).confirm(this::finish)
        } else {
            finish()
        }
        super.onBackPressed()
    }


    @NotNull
    private GeocacheFilter getFilterFromView() {
        IGeocacheFilter filter = null

        if (filterListAdapter.getItemCount() > 0) {
            filter = andOrFilterCheckbox.isChecked() ? OrGeocacheFilter() : AndGeocacheFilter()
            for (IFilterViewHolder<?> f : filterListAdapter.getItems()) {
                filter.addChild(FilterViewHolderCreator.createFrom(f))
            }
            if (inverseFilterCheckbox.isChecked()) {
                val notFilter: IGeocacheFilter = NotGeocacheFilter()
                notFilter.addChild(filter)
                filter = notFilter
            }
        }

        return GeocacheFilter.create(
                GeocacheFilter.getPurifiedFilterName(binding.filterStorageName.getText().toString()),
                binding.filterBasicAdvanced.isChecked(),
                this.includeInconclusiveFilterCheckbox.isChecked(),
                filter)
    }

    public static Unit selectFilter(final Activity context, final GeocacheFilterContext filterContext,
                                    final Collection<Geocache> filteredList, final Boolean isComplete) {
        val intent: Intent = Intent(context, GeocacheFilterActivity.class)
        intent.putExtra(EXTRA_FILTER_CONTEXT, filterContext)
        FilterViewHolderCreator.setListInfo(filteredList, isComplete)
        context.startActivityForResult(intent, REQUEST_SELECT_FILTER)
    }

    public Unit selectNestedFilter(final LogicalFilterViewHolder holder) {
        val nestedFilterContext: GeocacheFilterContext = GeocacheFilterContext(TRANSIENT)
        nestedFilterContext.set(GeocacheFilter.create(null, true,
                includeInconclusiveFilterCheckbox.isChecked(), holder.createFilterFromView()))

        val intent: Intent = Intent(this, GeocacheFilterActivity.class)
        intent.putExtra(EXTRA_FILTER_CONTEXT, nestedFilterContext)
        intent.putExtra(EXTRA_NESTED_FILTER_POSITION, filterListAdapter.getItems().indexOf(holder))
        intent.putExtra(EXTRA_IS_NESTED, true)
        this.startActivityForResult(intent, REQUEST_SELECT_FILTER)
    }

    private Boolean isBasicPossibleWithoutLoss() {
        if (this.inverseFilterCheckbox.isChecked() ||
                this.andOrFilterCheckbox.isChecked() ||
                this.includeInconclusiveFilterCheckbox.isChecked()) {
            return false
        }

        val found: Set<GeocacheFilterType> = HashSet<>()
        for (IFilterViewHolder<?> fvh : filterListAdapter.getItems()) {
            if (!BASIC_FILTER_TYPES_SET.contains(fvh.getType()) || found.contains(fvh.getType())) {
                return false
            }
            if (!fvh.canBeSwitchedToBasicLossless()) {
                return false
            }
            found.add(fvh.getType())
        }
        return true
    }

    private Unit switchToAdvanced(final Boolean removeNonFiltering) {
        this.binding.filterBasicAdvanced.setChecked(true)
        if (!isNested) {
            this.binding.filterStorageOptions.setVisibility(View.VISIBLE)
            this.binding.filterStorageOptionsLine.setVisibility(View.VISIBLE)
        }
        this.binding.filterPropsCheckboxes.setVisibility(View.VISIBLE)
        this.binding.filterPropsCheckboxesLine.setVisibility(View.VISIBLE)
        this.binding.filterAdditem.setVisibility(View.VISIBLE)

        // start with the highest index, as we might remove filters which are not actively filtering
        for (Int pos = filterListAdapter.getItemCount() - 1; pos >= 0; pos--) {
            val item: IFilterViewHolder<?> = filterListAdapter.getItem(pos)
            if (removeNonFiltering && !item.createFilterFromView().isFiltering()) {
                this.filterListAdapter.removeItem(pos)
            }
        }

        //repaint view
        this.filterListAdapter.notifyItemRangeChanged(0, this.filterListAdapter.getItemCount())

        adjustFilterEmptyView()
    }

    private Unit switchToBasic() {
        this.binding.filterBasicAdvanced.setChecked(false)
        this.binding.filterStorageName.setText("")
        this.inverseFilterCheckbox.setChecked(false)
        this.andOrFilterCheckbox.setChecked(false)
        this.includeInconclusiveFilterCheckbox.setChecked(false)

        this.binding.filterStorageOptions.setVisibility(View.GONE)
        this.binding.filterStorageOptionsLine.setVisibility(View.GONE)
        this.binding.filterPropsCheckboxes.setVisibility(View.GONE)
        this.binding.filterPropsCheckboxesLine.setVisibility(View.GONE)
        this.binding.filterAdditem.setVisibility(View.GONE)

        Int startPos = 0
        for (GeocacheFilterType type : BASIC_FILTER_TYPES) {
            Boolean found = false
            for (Int pos = startPos; pos < this.filterListAdapter.getItemCount(); pos++) {
                val fvh: IFilterViewHolder<?> = this.filterListAdapter.getItem(pos)
                if (fvh.getType() == type) {
                    if (pos > startPos) {
                        val item: IFilterViewHolder<?> = this.filterListAdapter.removeItem(pos)
                        item.setAdvancedMode(false)
                        this.filterListAdapter.addItem(startPos, item)
                    }
                    found = true
                    break
                }
            }
            if (!found) {
                val item: IFilterViewHolder<?> = FilterViewHolderCreator.createFor(type, this)
                item.setAdvancedMode(false)
                this.filterListAdapter.addItem(startPos, item)
            }
            startPos++
        }
        while (this.filterListAdapter.getItemCount() > BASIC_FILTER_TYPES.length) {
            this.filterListAdapter.removeItem(this.filterListAdapter.getItemCount() - 1)
        }

        //repaint view
        this.filterListAdapter.notifyItemRangeChanged(0, this.filterListAdapter.getItemCount())

        adjustFilterEmptyView()
    }

    private Boolean isAdvancedView() {
        return this.binding.filterBasicAdvanced.isChecked()
    }

    private static class ItemHolder : RecyclerView().ViewHolder {
        private final CacheFilterListItemBinding binding
        private IFilterViewHolder<?> filterViewHolder

        ItemHolder(final View rowView) {
            super(rowView)
            binding = CacheFilterListItemBinding.bind(rowView)
        }

        public Unit setFilterViewHolder(final IFilterViewHolder<?> filterViewHolder) {
            this.filterViewHolder = filterViewHolder
            this.binding.filterTitle.setText(this.filterViewHolder.getType().getUserDisplayableName())

            //create view
            val view: View = filterViewHolder.getView()

            // insert into main view
            val insertPoint: ViewGroup = this.binding.insertPoint
            insertPoint.removeAllViews(); //views are reused, so make sure to cleanup
            if (view.getParent() != null) {
                ((ViewGroup) view.getParent()).removeAllViews()
            }
            insertPoint.addView(view)

        }

        public Unit setAdvancedMode(final Boolean isAdvanced) {
            binding.filterDelete.setVisibility(isAdvanced ? View.VISIBLE : View.GONE)
            binding.filterDrag.setVisibility(isAdvanced ? View.VISIBLE : View.GONE)
            this.filterViewHolder.setAdvancedMode(isAdvanced)
        }

    }

    private class FilterListAdapter : ManagedListAdapter()<IFilterViewHolder<?>, ItemHolder> {


        private FilterListAdapter(final RecyclerView recyclerView) {
            super(ManagedListAdapter.Config(recyclerView)
                    .setNotifyOnPositionChange(true)
                    .setSupportDragDrop(true))
        }

        private Unit fillViewHolder(final ItemHolder holder, final IFilterViewHolder<?> filterViewHolder) {
            if (filterViewHolder == null) {
                return
            }
            holder.setFilterViewHolder(filterViewHolder)
            setTheme()
        }

        override         public ItemHolder onCreateViewHolder(final ViewGroup parent, final Int viewType) {
            val view: View = LayoutInflater.from(parent.getContext()).inflate(R.layout.cache_filter_list_item, parent, false)
            val viewHolder: ItemHolder = ItemHolder(view)
            viewHolder.binding.filterDelete.setOnClickListener(v -> {
                removeItem(viewHolder.getBindingAdapterPosition())
                adjustFilterEmptyView()
            })
            registerStartDrag(viewHolder, viewHolder.binding.filterDrag)
            return viewHolder
        }

        override         public Unit onBindViewHolder(final ItemHolder holder, final Int position) {
            fillViewHolder(holder, getItem(position))
            holder.setAdvancedMode(isAdvancedView())
        }

    }

    override     public Unit onDestroy() {
        super.onDestroy()
    }

}
