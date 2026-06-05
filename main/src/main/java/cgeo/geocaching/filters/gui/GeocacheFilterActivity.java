package cgeo.geocaching.filters.gui;

import cgeo.geocaching.R;
import cgeo.geocaching.activity.AbstractActionBarActivity;
import cgeo.geocaching.activity.ActivityMixin;
import cgeo.geocaching.databinding.CacheFilterActivityBinding;
import cgeo.geocaching.databinding.CacheFilterListItemBinding;
import cgeo.geocaching.filters.FilterUtils;
import cgeo.geocaching.filters.NamedFilter;
import cgeo.geocaching.filters.NamedFilterActivity;
import cgeo.geocaching.filters.core.AndGeocacheFilter;
import cgeo.geocaching.filters.core.BaseGeocacheFilter;
import cgeo.geocaching.filters.core.GeocacheFilter;
import cgeo.geocaching.filters.core.GeocacheFilterContext;
import cgeo.geocaching.filters.core.GeocacheFilterType;
import cgeo.geocaching.filters.core.IGeocacheFilter;
import cgeo.geocaching.filters.core.LogicalGeocacheFilter;
import cgeo.geocaching.filters.core.NotGeocacheFilter;
import cgeo.geocaching.filters.core.OrGeocacheFilter;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.ui.ImageParam;
import cgeo.geocaching.ui.TextParam;
import cgeo.geocaching.ui.TextSpinner;
import cgeo.geocaching.ui.ViewUtils;
import cgeo.geocaching.ui.dialog.SimpleDialog;
import cgeo.geocaching.ui.recyclerview.ManagedListAdapter;
import cgeo.geocaching.utils.CollectionStream;
import cgeo.geocaching.utils.LocalizationUtils;
import cgeo.geocaching.utils.Log;
import cgeo.geocaching.utils.TextUtils;
import static cgeo.geocaching.filters.core.GeocacheFilterContext.FilterType.TRANSIENT;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BooleanSupplier;

import org.apache.commons.lang3.tuple.ImmutablePair;

/**
 * Show a filter selection using an {@code ExpandableListView}.
 */
public class GeocacheFilterActivity extends AbstractActionBarActivity {

    public static final int REQUEST_SELECT_FILTER = 456;
    public static final String EXTRA_FILTER_CONTEXT = "efc";
    public static final String EXTRA_IS_NESTED = "extra_is_nested";
    public static final String EXTRA_NESTED_FILTER_POSITION = "extra_nested_pos";

    private static final String STATE_CURRENT_FILTER = "state_current_filter";
    private static final String STATE_ADVANCED_VIEW = "state_advanced_view";
    private static final String STATE_FILTER_CONTEXT = "state_filter_context";
    private static final String STATE_ORIGINAL_FILTER_CONFIG = "state_original_filter_config";

    private static final GeocacheFilterType[] BASIC_FILTER_TYPES =
            new GeocacheFilterType[]{GeocacheFilterType.TYPE, GeocacheFilterType.DIFFICULTY_TERRAIN, GeocacheFilterType.STATUS};
    private static final Set<GeocacheFilterType> BASIC_FILTER_TYPES_SET = new HashSet<>(Arrays.asList(BASIC_FILTER_TYPES));

    private static final GeocacheFilterType[] INTERNAL_FILTER_TYPES =
            new GeocacheFilterType[]{GeocacheFilterType.DIFFICULTY, GeocacheFilterType.TERRAIN};
    private static final Set<GeocacheFilterType> INTERNAL_FILTER_TYPES_SET = new HashSet<>(Arrays.asList(INTERNAL_FILTER_TYPES));

    private GeocacheFilterContext filterContext = new GeocacheFilterContext(TRANSIENT);
    private String originalFilterConfig;

    private CacheFilterActivityBinding binding;
    private FilterListAdapter filterListAdapter;

    private CheckBox andOrFilterCheckbox;
    private CheckBox inverseFilterCheckbox;
    private CheckBox includeInconclusiveFilterCheckbox;

    private final TextSpinner<GeocacheFilterType> addFilter = new TextSpinner<>();

    private boolean processBasicAdvancedListener = true;
    private boolean isNested = false;

    private BooleanSupplier navUpHandler;

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setThemeAndContentView(R.layout.cache_filter_activity);
        binding = CacheFilterActivityBinding.bind(findViewById(R.id.activity_content));

        binding.filterPropsCheckboxes.removeAllViews();
        this.andOrFilterCheckbox = ViewUtils.addCheckboxItem(this, binding.filterPropsCheckboxes, TextParam.id(R.string.cache_filter_option_and_or), R.drawable.ic_menu_logic);
        this.inverseFilterCheckbox = ViewUtils.addCheckboxItem(this, binding.filterPropsCheckboxes, TextParam.id(R.string.cache_filter_option_inverse), R.drawable.ic_menu_invert);

        final ImmutablePair<View, CheckBox> includeInconclusiveFilterCheckboxItem = ViewUtils.createCheckboxItem(this, binding.filterPropsCheckboxes, TextParam.id(R.string.cache_filter_option_include_inconclusive), ImageParam.id(R.drawable.ic_menu_vague), TextParam.id(R.string.cache_filter_option_include_inconclusive_info));
        binding.filterPropsCheckboxes.addView(includeInconclusiveFilterCheckboxItem.left);
        this.includeInconclusiveFilterCheckbox = includeInconclusiveFilterCheckboxItem.right;

        filterListAdapter = new FilterListAdapter(binding.filterList);
        initializeFilterAdd();
        initializeNamedFilterButtons();

        // Get parameters from intent and basic cache information from database
        final Bundle extras = getIntent().getExtras();

        if (extras != null) {
            filterContext = extras.getParcelable(EXTRA_FILTER_CONTEXT);
            isNested = extras.getBoolean(EXTRA_IS_NESTED);
        }
        if (filterContext == null) {
            filterContext = new GeocacheFilterContext(TRANSIENT);
        }

        setTitle(LocalizationUtils.getString(filterContext.getType().titleId));
        fillViewFromFilter(filterContext.get().toConfig(), false);
        originalFilterConfig = getFilterFromView().toConfig();

        navUpHandler = ActivityMixin.registerBackNavigationInterceptor(this, this::onNavigationIntercepted);

        // Some features do not work / make no sense for nested filters
        if (isNested) {
            setTitle(LocalizationUtils.getString(R.string.cache_filter_contexttype_nestedfilter_title));
            binding.filterBasicAdvanced.setVisibility(View.GONE);
            binding.filterNamedFilterOptions.setVisibility(View.GONE);
            includeInconclusiveFilterCheckboxItem.left.setVisibility(View.GONE);
        }

        this.binding.filterBasicAdvanced.setOnCheckedChangeListener((v, c) -> {
            if (!processBasicAdvancedListener) {
                return;
            }

            if (c) {
                switchToAdvanced(true);
            } else if (isBasicPossibleWithoutLoss()) {
                switchToBasic();
            } else {
                SimpleDialog.of(this).setTitle(R.string.cache_filter_mode_basic_change_confirm_loss_title).setMessage(R.string.cache_filter_mode_basic_change_confirm_loss_message).confirm(
                        this::switchToBasic, () -> {
                            processBasicAdvancedListener = false;
                            this.binding.filterBasicAdvanced.setChecked(true);
                            processBasicAdvancedListener = true;
                        });
            }
        });
    }

    @Override
    public void onConfigurationChanged(@NonNull final Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    private void initializeNamedFilterButtons() {
        // Add Named Filter criterion button
        binding.filterFillWithNamed.setOnClickListener(v -> {
            FilterUtils.openDialogSelectNamedFilter(this, TextParam.id(R.string.named_filter_fill_with_named), selected ->
                fillViewFromFilter(selected == null || selected.getFilter() == null ? null : selected.getFilter().toConfig(), true));
        });
        ViewUtils.setTooltip(binding.filterFillWithNamed, TextParam.id(R.string.named_filter_fill_with_named));

        // Save as Named Filter button
        binding.filterSaveAsNamed.setOnClickListener(v -> {
            SimpleDialog.of(this).setTitle(R.string.named_filter_save_as_title)
                    .input(new SimpleDialog.InputOptions(), name -> {
                        if (name == null || name.trim().isEmpty()) {
                            return;
                        }
                        final GeocacheFilter currentFilter = getFilterFromView();
                        if (NamedFilter.nameExists(name)) {
                            SimpleDialog.of(this).setTitle(R.string.named_filter_name_exists_title)
                                    .setMessage(R.string.named_filter_name_exists_message, name)
                                    .confirm(() -> NamedFilter.addNew(name, currentFilter));
                        } else {
                            final NamedFilter existing = NamedFilter.filterConfigExists(currentFilter);
                            if (existing != null) {
                                SimpleDialog.of(this).setTitle(R.string.named_filter_config_exists_title)
                                        .setMessage(R.string.named_filter_config_exists_message, existing.getName())
                                        .confirm(() -> NamedFilter.addNew(name, currentFilter));
                            } else {
                                NamedFilter.addNew(name, currentFilter);
                            }
                        }
                    });
        });
        ViewUtils.setTooltip(binding.filterSaveAsNamed, TextParam.id(R.string.named_filter_save_as_title));

        // Open NamedFilterActivity button
        binding.filterOpenNamedFilterActivity.setOnClickListener(v ->
                startActivity(new Intent(this, NamedFilterActivity.class)));
        ViewUtils.setTooltip(binding.filterOpenNamedFilterActivity, TextParam.id(R.string.named_filter_activity_title));
    }

    @Override
    public void onSaveInstanceState(@NonNull final Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(STATE_CURRENT_FILTER, getFilterFromView().toConfig());
        outState.putBoolean(STATE_ADVANCED_VIEW, isAdvancedView());
        outState.putParcelable(STATE_FILTER_CONTEXT, filterContext);
        outState.putString(STATE_ORIGINAL_FILTER_CONFIG, originalFilterConfig);
    }

    @Override
    protected void onRestoreInstanceState(@NonNull final Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        if (savedInstanceState.getString(STATE_CURRENT_FILTER) != null) {
            fillViewFromFilter(savedInstanceState.getString(STATE_CURRENT_FILTER), savedInstanceState.getBoolean(STATE_ADVANCED_VIEW));
        }
        filterContext = (GeocacheFilterContext) savedInstanceState.getSerializable(STATE_FILTER_CONTEXT);
        if (filterContext == null) {
            filterContext = new GeocacheFilterContext(TRANSIENT);
        }
        originalFilterConfig = savedInstanceState.getString(STATE_ORIGINAL_FILTER_CONFIG);
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        getMenuInflater().inflate(R.menu.menu_ok_cancel, menu);
        menu.findItem(R.id.menu_item_delete).setVisible(true);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        // Handle presses on the action bar items
        final int itemId = item.getItemId();
        if (itemId == R.id.menu_item_delete) {
            clearView();
            return true;
        } else if (itemId == R.id.menu_item_save) {
            finishWithResult();
            return true;
        } else if (itemId == R.id.menu_item_cancel) {
            return onSupportNavigateUp();
        } else if (itemId == android.R.id.home) {
            return onSupportNavigateUp();
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_SELECT_FILTER && resultCode == Activity.RESULT_OK) {
            final int index = data.getIntExtra(EXTRA_NESTED_FILTER_POSITION, -1);
            if (index >= 0 && filterListAdapter.getItems().get(index) instanceof LogicalFilterViewHolder) {
                final LogicalGeocacheFilter newFilter = (LogicalGeocacheFilter) ((GeocacheFilterContext) data.getParcelableExtra(EXTRA_FILTER_CONTEXT)).get().getTree();
                ((LogicalFilterViewHolder) filterListAdapter.getItems().get(index)).setViewFromFilter(newFilter);
            }
        }
    }

    private void fillViewFromFilter(final String inputFilter, final boolean forceAdvanced) {
        includeInconclusiveFilterCheckbox.setChecked(false);
        inverseFilterCheckbox.setChecked(false);
        andOrFilterCheckbox.setChecked(false);

        boolean setAdvanced = false;
        if (inputFilter != null) {
            try {
                final List<IFilterViewHolder<?>> filterList = new ArrayList<>();
                final GeocacheFilter filter = GeocacheFilter.checkConfig(inputFilter);
                includeInconclusiveFilterCheckbox.setChecked(filter.isIncludeInconclusive());
                setAdvanced = filter.isOpenInAdvancedMode();
                IGeocacheFilter filterTree = filter.getTree();
                if (filterTree instanceof NotGeocacheFilter) {
                    inverseFilterCheckbox.setChecked(true);
                    filterTree = filterTree.getChildren().get(0);
                }
                if (filterTree instanceof LogicalGeocacheFilter) {
                    andOrFilterCheckbox.setChecked(filterTree instanceof OrGeocacheFilter);
                    for (IGeocacheFilter c : filterTree.getChildren()) {
                        filterList.add(FilterViewHolderCreator.createFor(c, this));
                    }
                } else if (filterTree instanceof BaseGeocacheFilter) {
                    filterList.add(FilterViewHolderCreator.createFor(filterTree, this));
                }
                filterListAdapter.setItems(filterList);
                adjustFilterEmptyView();
            } catch (ParseException pe) {
                Log.w("Exception parsing input filter", pe);
            }
        }

        // set basic/advanced switch -> AFTER list is re-layouted
        if (!forceAdvanced && !setAdvanced && isBasicPossibleWithoutLoss()) {
            switchToBasic();
        } else {
            switchToAdvanced(false);
        }
    }

    private void initializeFilterAdd() {
        final List<GeocacheFilterType> filterTypes = new ArrayList<>(
                CollectionStream.of(GeocacheFilterType.values()).filter(GeocacheFilterType::displayToUser).toList());
        filterTypes.removeAll(INTERNAL_FILTER_TYPES_SET);

        Collections.sort(filterTypes, (left, right) -> TextUtils.COLLATOR.compare(left.getUserDisplayableName(), right.getUserDisplayableName()));
        addFilter.setValues(filterTypes)
                .setDisplayMapperPure(GeocacheFilterType::getUserDisplayableName)
                .setTextHideSelectionMarker(true)
                .setView(binding.filterAdditem, (v, t) -> {
                })
                .setTextGroupMapper(GeocacheFilterType::getUserDisplayableGroup)
                .setChangeListener(gcf -> {
                    filterListAdapter.addItem(0, FilterViewHolderCreator.createFor(gcf, this));
                    binding.filterList.smoothScrollToPosition(0);
                    adjustFilterEmptyView();
                }, false);
    }

    private void adjustFilterEmptyView() {
        final boolean listIsEmpty = filterListAdapter.getItemCount() == 0;
        binding.filterList.setVisibility(listIsEmpty ? View.GONE : View.VISIBLE);
        binding.filterListEmpty.setVisibility(!listIsEmpty ? View.GONE : View.VISIBLE);
    }

    private void clearView() {
        filterListAdapter.clearList();
        andOrFilterCheckbox.setChecked(false);
        inverseFilterCheckbox.setChecked(false);
        includeInconclusiveFilterCheckbox.setChecked(false);
        if (!isAdvancedView()) {
            switchToBasic();
        }
        adjustFilterEmptyView();
    }

    private void finishWithResult() {
        final Intent resultIntent = new Intent();
        final GeocacheFilter newFilter = getFilterFromView();
        filterContext.set(newFilter);
        resultIntent.putExtra(EXTRA_FILTER_CONTEXT, filterContext);
        resultIntent.putExtra(EXTRA_NESTED_FILTER_POSITION, getIntent().getIntExtra(EXTRA_NESTED_FILTER_POSITION, -1));
        FilterViewHolderCreator.clearListInfo();
        setResult(Activity.RESULT_OK, resultIntent);
        finish();
    }

    /**
     * Called when the user attempts to leave via back press or navigate up.
     *
     * @param navigationAction concludes the intercepted back/up navigation when executed
     * @return true to STOP navigation (a confirmation dialog was shown), false to CONTINUE
     */
    private boolean onNavigationIntercepted(final Runnable navigationAction) {
        final GeocacheFilter newFilter = getFilterFromView();
        final boolean filterWasChanged = originalFilterConfig != null && !originalFilterConfig.equals(newFilter.toConfig());
        if (filterWasChanged) {
            SimpleDialog.of(this).setTitle(R.string.confirm_unsaved_changes_title).setMessage(R.string.confirm_discard_changes)
                    .confirm(navigationAction);
            return true;
        }
        return false;
    }

    @Override
    public boolean onSupportNavigateUp() {
        if (navUpHandler.getAsBoolean()) {
            return true;
        }
        return ActivityMixin.navigateUp(this) || super.onSupportNavigateUp();
    }

    @NonNull
    private GeocacheFilter getFilterFromView() {
        IGeocacheFilter filter = null;

        if (filterListAdapter.getItemCount() > 0) {
            filter = andOrFilterCheckbox.isChecked() ? new OrGeocacheFilter() : new AndGeocacheFilter();
            for (IFilterViewHolder<?> f : filterListAdapter.getItems()) {
                filter.addChild(FilterViewHolderCreator.createFrom(f));
            }
            if (inverseFilterCheckbox.isChecked()) {
                final IGeocacheFilter notFilter = new NotGeocacheFilter();
                notFilter.addChild(filter);
                filter = notFilter;
            }
        }

        return GeocacheFilter.create(
                binding.filterBasicAdvanced.isChecked(),
                this.includeInconclusiveFilterCheckbox.isChecked(),
                filter);
    }

    public static void selectFilter(@NonNull final Activity context, final GeocacheFilterContext filterContext,
                                    final Collection<Geocache> filteredList, final boolean isComplete) {
        final Intent intent = new Intent(context, GeocacheFilterActivity.class);
        intent.putExtra(EXTRA_FILTER_CONTEXT, filterContext);
        FilterViewHolderCreator.setListInfo(filteredList, isComplete);
        context.startActivityForResult(intent, REQUEST_SELECT_FILTER);
    }

    public void selectNestedFilter(final LogicalFilterViewHolder holder) {
        final GeocacheFilterContext nestedFilterContext = new GeocacheFilterContext(TRANSIENT);
        nestedFilterContext.set(GeocacheFilter.create(true,
                includeInconclusiveFilterCheckbox.isChecked(), holder.createFilterFromView()));

        final Intent intent = new Intent(this, GeocacheFilterActivity.class);
        intent.putExtra(EXTRA_FILTER_CONTEXT, nestedFilterContext);
        intent.putExtra(EXTRA_NESTED_FILTER_POSITION, filterListAdapter.getItems().indexOf(holder));
        intent.putExtra(EXTRA_IS_NESTED, true);
        this.startActivityForResult(intent, REQUEST_SELECT_FILTER);
    }

    private boolean isBasicPossibleWithoutLoss() {
        if (this.inverseFilterCheckbox.isChecked() ||
                this.andOrFilterCheckbox.isChecked() ||
                this.includeInconclusiveFilterCheckbox.isChecked()) {
            return false;
        }

        final Set<GeocacheFilterType> found = new HashSet<>();
        for (IFilterViewHolder<?> fvh : filterListAdapter.getItems()) {
            if (!BASIC_FILTER_TYPES_SET.contains(fvh.getType()) || found.contains(fvh.getType())) {
                return false;
            }
            if (!fvh.canBeSwitchedToBasicLossless()) {
                return false;
            }
            found.add(fvh.getType());
        }
        return true;
    }

    private void switchToAdvanced(final boolean removeNonFiltering) {
        this.binding.filterBasicAdvanced.setChecked(true);
        if (!isNested) {
            this.binding.filterNamedFilterOptions.setVisibility(View.VISIBLE);
        }
        this.binding.filterPropsCheckboxes.setVisibility(View.VISIBLE);
        this.binding.filterPropsCheckboxesLine.setVisibility(View.VISIBLE);
        this.binding.filterAdditem.setVisibility(View.VISIBLE);

        // start with the highest index, as we might remove filters which are not actively filtering
        for (int pos = filterListAdapter.getItemCount() - 1; pos >= 0; pos--) {
            final IFilterViewHolder<?> item = filterListAdapter.getItem(pos);
            if (removeNonFiltering && !item.createFilterFromView().isFiltering()) {
                this.filterListAdapter.removeItem(pos);
            }
        }

        // repaint view
        this.filterListAdapter.notifyItemRangeChanged(0, this.filterListAdapter.getItemCount());

        adjustFilterEmptyView();
        // Refresh window insets to ensure proper padding after view visibility changes
        refreshActivityContentInsets();
    }

    private void switchToBasic() {
        this.binding.filterBasicAdvanced.setChecked(false);
        this.inverseFilterCheckbox.setChecked(false);
        this.andOrFilterCheckbox.setChecked(false);
        this.includeInconclusiveFilterCheckbox.setChecked(false);

        this.binding.filterNamedFilterOptions.setVisibility(View.GONE);
        this.binding.filterPropsCheckboxes.setVisibility(View.GONE);
        this.binding.filterPropsCheckboxesLine.setVisibility(View.GONE);
        this.binding.filterAdditem.setVisibility(View.GONE);

        int startPos = 0;
        for (GeocacheFilterType type : BASIC_FILTER_TYPES) {
            boolean found = false;
            for (int pos = startPos; pos < this.filterListAdapter.getItemCount(); pos++) {
                final IFilterViewHolder<?> fvh = this.filterListAdapter.getItem(pos);
                if (fvh.getType() == type) {
                    if (pos > startPos) {
                        final IFilterViewHolder<?> item = this.filterListAdapter.removeItem(pos);
                        item.setAdvancedMode(false);
                        this.filterListAdapter.addItem(startPos, item);
                    }
                    found = true;
                    break;
                }
            }
            if (!found) {
                final IFilterViewHolder<?> item = FilterViewHolderCreator.createFor(type, this);
                item.setAdvancedMode(false);
                this.filterListAdapter.addItem(startPos, item);
            }
            startPos++;
        }
        while (this.filterListAdapter.getItemCount() > BASIC_FILTER_TYPES.length) {
            this.filterListAdapter.removeItem(this.filterListAdapter.getItemCount() - 1);
        }

        // repaint view
        this.filterListAdapter.notifyItemRangeChanged(0, this.filterListAdapter.getItemCount());

        adjustFilterEmptyView();
        // Refresh window insets to ensure proper padding after view visibility changes
        refreshActivityContentInsets();
    }

    private boolean isAdvancedView() {
        return this.binding.filterBasicAdvanced.isChecked();
    }

    private static class ItemHolder extends RecyclerView.ViewHolder {
        private final CacheFilterListItemBinding binding;
        private IFilterViewHolder<?> filterViewHolder;

        ItemHolder(final View rowView) {
            super(rowView);
            binding = CacheFilterListItemBinding.bind(rowView);
        }

        public void setFilterViewHolder(final IFilterViewHolder<?> filterViewHolder) {
            this.filterViewHolder = filterViewHolder;
            this.binding.filterTitle.setText(this.filterViewHolder.getType().getUserDisplayableName());

            //create view
            final View view = filterViewHolder.getView();

            // insert into main view
            final ViewGroup insertPoint = this.binding.insertPoint;
            insertPoint.removeAllViews(); //views are reused, so make sure to cleanup
            if (view.getParent() != null) {
                ((ViewGroup) view.getParent()).removeAllViews();
            }
            insertPoint.addView(view);
        }

        public void setAdvancedMode(final boolean isAdvanced) {
            binding.filterDelete.setVisibility(isAdvanced ? View.VISIBLE : View.GONE);
            binding.filterDrag.setVisibility(isAdvanced ? View.VISIBLE : View.GONE);
            this.filterViewHolder.setAdvancedMode(isAdvanced);
        }
    }

    private final class FilterListAdapter extends ManagedListAdapter<IFilterViewHolder<?>, ItemHolder> {

        private FilterListAdapter(final RecyclerView recyclerView) {
            super(new ManagedListAdapter.Config(recyclerView)
                    .setNotifyOnPositionChange(true)
                    .setSupportDragDrop(true));
        }

        private void fillViewHolder(final ItemHolder holder, final IFilterViewHolder<?> filterViewHolder) {
            if (filterViewHolder == null) {
                return;
            }
            holder.setFilterViewHolder(filterViewHolder);
            setTheme();
        }

        @Override
        @NonNull
        public ItemHolder onCreateViewHolder(@NonNull final ViewGroup parent, final int viewType) {
            final View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.cache_filter_list_item, parent, false);
            final ItemHolder viewHolder = new ItemHolder(view);
            viewHolder.binding.filterDelete.setOnClickListener(v -> {
                removeItem(viewHolder.getBindingAdapterPosition());
                adjustFilterEmptyView();
            });
            registerStartDrag(viewHolder, viewHolder.binding.filterDrag);
            return viewHolder;
        }

        @Override
        public void onBindViewHolder(@NonNull final ItemHolder holder, final int position) {
            fillViewHolder(holder, getItem(position));
            holder.setAdvancedMode(isAdvancedView());
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }
}
