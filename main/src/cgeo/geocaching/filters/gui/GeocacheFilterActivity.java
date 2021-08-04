package cgeo.geocaching.filters.gui;

import cgeo.geocaching.R;
import cgeo.geocaching.activity.AbstractActionBarActivity;
import cgeo.geocaching.databinding.CacheFilterActivityBinding;
import cgeo.geocaching.databinding.CacheFilterListItemBinding;
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
import cgeo.geocaching.ui.TextParam;
import cgeo.geocaching.ui.TextSpinner;
import cgeo.geocaching.ui.ViewUtils;
import cgeo.geocaching.ui.dialog.SimpleDialog;
import cgeo.geocaching.ui.recyclerview.ManagedListAdapter;
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

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;


/**
 * Show a filter selection using an {@code ExpandableListView}.
 */
public class GeocacheFilterActivity extends AbstractActionBarActivity {

    public static final int REQUEST_SELECT_FILTER = 456;
    public static final String EXTRA_FILTER_CONTEXT = "efc";

    private static final String STATE_CURRENT_FILTER = "state_current_filter";
    private static final String STATE_ADVANCED_VIEW = "state_advanced_view";
    private static final String STATE_FILTER_CONTEXT = "state_filter_context";
    private static final String STATE_ORIGINAL_FILTER_CONFIG = "state_original_filter_config";

    private static final GeocacheFilterType[] BASIC_FILTER_TYPES =
        new GeocacheFilterType[]{GeocacheFilterType.TYPE, GeocacheFilterType.DIFFICULTY_TERRAIN, GeocacheFilterType.STATUS };
    private static final Set<GeocacheFilterType> BASIC_FILTER_TYPES_SET = new HashSet<>(Arrays.asList(BASIC_FILTER_TYPES));

    private static final GeocacheFilterType[] INTERNAL_FILTER_TYPES =
        new GeocacheFilterType[]{GeocacheFilterType.DIFFICULTY, GeocacheFilterType.TERRAIN };
    private static final Set<GeocacheFilterType> INTERNAL_FILTER_TYPES_SET = new HashSet<>(Arrays.asList(INTERNAL_FILTER_TYPES));

    private GeocacheFilterContext filterContext = new GeocacheFilterContext(TRANSIENT);
    private String originalFilterConfig;

    private CacheFilterActivityBinding binding;
    private FilterListAdapter filterListAdapter;

    private CheckBox andOrFilterCheckbox;
    private CheckBox inverseFilterCheckbox;
    private CheckBox includeInconclusiveFilterCheckbox;

    private final TextSpinner<GeocacheFilterType> addFilter = new TextSpinner<>();

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setThemeAndContentView(R.layout.cache_filter_activity);
        binding = CacheFilterActivityBinding.bind(findViewById(R.id.activity_viewroot));

        binding.filterPropsCheckboxes.removeAllViews();
        this.andOrFilterCheckbox = ViewUtils.addCheckboxItem(this, binding.filterPropsCheckboxes, TextParam.id(R.string.cache_filter_option_and_or), R.drawable.ic_menu_logic);
        this.inverseFilterCheckbox = ViewUtils.addCheckboxItem(this, binding.filterPropsCheckboxes, TextParam.id(R.string.cache_filter_option_inverse), R.drawable.ic_menu_invert);
        this.includeInconclusiveFilterCheckbox = ViewUtils.addCheckboxItem(this, binding.filterPropsCheckboxes, TextParam.id(R.string.cache_filter_option_include_inconclusive), R.drawable.ic_menu_vague,
            TextParam.id(R.string.cache_filter_option_include_inconclusive_info));

        filterListAdapter = new FilterListAdapter(binding.filterList);
        initializeFilterAdd();
        initializeStorageOptions();

        // Get parameters from intent and basic cache information from database
        final Bundle extras = getIntent().getExtras();

        if (extras != null) {
            filterContext = extras.getParcelable(EXTRA_FILTER_CONTEXT);
        }
        if (filterContext == null) {
            filterContext = new GeocacheFilterContext(TRANSIENT);
        }


        setTitle(getString(filterContext.getType().titleId));
        fillViewFromFilter(filterContext.get().toConfig(), false);
        originalFilterConfig = getFilterFromView().toConfig();

        this.binding.filterBasicAdvanced.setOnCheckedChangeListener((v, c) -> {
            if (c) {
                switchToAdvanced();
            } else if (isBasicPossibleWithoutLoss()) {
                switchToBasic();
            } else {
                SimpleDialog.of(this).setTitle(R.string.cache_filter_mode_basic_change_confirm_loss_title).setMessage(R.string.cache_filter_mode_basic_change_confirm_loss_message).confirm(
                    (vv, ii) -> switchToBasic(), (vv, ii) -> this.binding.filterBasicAdvanced.setChecked(true));
            }
        });
    }

    @Override
    public void onConfigurationChanged(@NonNull final Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    private void initializeStorageOptions() {

        //handling of "save" button
        binding.filterStorageSave.setOnClickListener(v -> {
            String filterName = binding.filterStorageName.getText().toString();
            if (filterName.endsWith("*")) {
                filterName = filterName.substring(0, filterName.length() - 1);
            }
            SimpleDialog.of(this).setTitle(R.string.cache_filter_storage_save_title)
                .input(-1, filterName, null, null, newName -> {
                    final GeocacheFilter filter = getFilterFromView();
                    if (GeocacheFilter.Storage.existsAndDiffers(newName, filter)) {
                        SimpleDialog.of(this).setTitle(R.string.cache_filter_storage_save_confirm_title).setMessage(R.string.cache_filter_storage_save_confirm_message, newName).confirm(
                            (dialog, which) -> saveAs(newName));
                    } else {
                        saveAs(newName);
                    }
                });
        });
        ViewUtils.setTooltip(binding.filterStorageSave, TextParam.id(R.string.cache_filter_storage_save_title));

        //handling of "load/delete" button
        binding.filterStorageManage.setOnClickListener(v -> {
                final List<GeocacheFilter> filters = new ArrayList<>(GeocacheFilter.Storage.getStoredFilters());

                if (filters.isEmpty()) {
                    SimpleDialog.of(this).setTitle(R.string.cache_filter_storage_load_delete_title).setMessage(R.string.cache_filter_storage_load_delete_nofilter_message).show();
                } else {
                    SimpleDialog.of(this).setTitle(R.string.cache_filter_storage_load_delete_title)
                        .setButtons(R.string.cache_filter_storage_load_button, R.string.cache_filter_storage_delete_button, 0)
                        .selectSingle(filters, (f, i) -> TextParam.text(f.getName()), -1, true,
                            (f, pos) -> fillViewFromFilter(f.toConfig(), isAdvancedView()),
                            (f, pos) -> {
                                GeocacheFilter.Storage.delete(f);
                                //if currently shown view was just deleted -> then delete it in view as well
                                if (f.getName().contentEquals(binding.filterStorageName.getText())) {
                                    binding.filterStorageName.setText("");
                                }
                            });
               }
            });
        ViewUtils.setTooltip(binding.filterStorageManage, TextParam.id(R.string.cache_filter_storage_load_delete_title));

    }

    private void saveAs(final String newName) {
        binding.filterStorageName.setText(newName);
        final GeocacheFilter filter = getFilterFromView();
        GeocacheFilter.Storage.save(filter);
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
            finish();
            return true;
        } else if (itemId == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return false;
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
                binding.filterStorageName.setText(filter.getNameForUserDisplay());
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
                }
                if (filterTree instanceof BaseGeocacheFilter) {
                    filterList.add(FilterViewHolderCreator.createFor(filterTree, this));
                }
                filterListAdapter.setItems(filterList);
                adjustFilterEmptyView();
                //filterListAdapter.submitList(filterList, this::adjustFilterEmptyView);

            } catch (ParseException pe) {
                Log.w("Exception parsing input filter", pe);
            }
        }

        //set basic/advanced switch
        if (!forceAdvanced && !setAdvanced && isBasicPossibleWithoutLoss()) {
            this.binding.filterBasicAdvanced.setChecked(false);
            switchToBasic();
        } else {
            this.binding.filterBasicAdvanced.setChecked(true);
            switchToAdvanced();
        }
    }

    private void initializeFilterAdd() {
        final List<GeocacheFilterType> filterTypes = new ArrayList<>(Arrays.asList(GeocacheFilterType.values()));
        filterTypes.removeAll(INTERNAL_FILTER_TYPES_SET);

        Collections.sort(filterTypes, (left, right) -> TextUtils.COLLATOR.compare(left.getUserDisplayableName(), right.getUserDisplayableName()));
        addFilter.setValues(filterTypes)
            .setDisplayMapper(GeocacheFilterType::getUserDisplayableName)
            .setTextHideSelectionMarker(true)
            .setView(binding.filterAdditem, (v, t) -> { })
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
        binding.filterStorageName.setText("");
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
        FilterViewHolderCreator.clearListInfo();
        setResult(Activity.RESULT_OK, resultIntent);
        finish();
    }

    @Override
    public void onBackPressed() {
        final GeocacheFilter newFilter = getFilterFromView();
        final boolean filterWasChanged = !originalFilterConfig.equals(newFilter.toConfig());
        if (filterWasChanged) {
            SimpleDialog.of(this).setTitle(R.string.confirm_unsaved_changes_title).setMessage(R.string.confirm_discard_changes).confirm((dialog, which) -> finish());
        } else {
            finish();
        }
    }


    @NotNull
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
            binding.filterStorageName.getText().toString(),
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

    private boolean isBasicPossibleWithoutLoss() {
        if (!StringUtils.isBlank(binding.filterStorageName.getText()) ||
            this.inverseFilterCheckbox.isChecked() ||
            this.andOrFilterCheckbox.isChecked() ||
            this.includeInconclusiveFilterCheckbox.isChecked()) {
            return false;
        }

        final Set<GeocacheFilterType> found = new HashSet<>();
        for (IFilterViewHolder<?> fvh : filterListAdapter.getItems()) {
            if (!BASIC_FILTER_TYPES_SET.contains(fvh.getType()) || found.contains(fvh.getType())) {
                return false;
            }
            if (fvh.isOf(StatusFilterViewHolder.class) && !fvh.castTo(StatusFilterViewHolder.class).canBeSimplifiedLossless()) {
                return false;
            }
            found.add(fvh.getType());
        }
        return true;
    }

    private void switchToAdvanced() {
        this.binding.filterBasicAdvanced.setChecked(true);
        this.binding.filterStorageOptions.setVisibility(View.VISIBLE);
        this.binding.filterStorageOptionsLine.setVisibility(View.VISIBLE);
        this.binding.filterPropsCheckboxes.setVisibility(View.VISIBLE);
        this.binding.filterPropsCheckboxesLine.setVisibility(View.VISIBLE);
        this.binding.filterAdditem.setVisibility(View.VISIBLE);

        for (int pos = 0; pos < filterListAdapter.getItemCount(); pos++) {
            final ItemHolder itemHolder = (ItemHolder) this.binding.filterList.findViewHolderForLayoutPosition(pos);
            if (itemHolder != null) {
                itemHolder.setControlsEnabled(true);
                if (itemHolder.getFilterViewHolder().isOf(StatusFilterViewHolder.class)) {
                    itemHolder.getFilterViewHolder().castTo(StatusFilterViewHolder.class).setSimpleView(false);
                }
            }
        }
        adjustFilterEmptyView();

    }

    private void switchToBasic() {
        this.binding.filterBasicAdvanced.setChecked(false);
        this.binding.filterStorageName.setText("");
        this.inverseFilterCheckbox.setChecked(false);
        this.andOrFilterCheckbox.setChecked(false);
        this.includeInconclusiveFilterCheckbox.setChecked(false);

        this.binding.filterStorageOptions.setVisibility(View.GONE);
        this.binding.filterStorageOptionsLine.setVisibility(View.GONE);
        this.binding.filterPropsCheckboxes.setVisibility(View.GONE);
        this.binding.filterPropsCheckboxesLine.setVisibility(View.GONE);
        this.binding.filterAdditem.setVisibility(View.GONE);

        for (int pos = 0; pos < filterListAdapter.getItemCount(); pos++) {
            final ItemHolder itemHolder = (ItemHolder) this.binding.filterList.findViewHolderForLayoutPosition(pos);
            if (itemHolder != null) {
                itemHolder.setControlsEnabled(false);
            }
        }

        int startPos = 0;
        for (GeocacheFilterType type : BASIC_FILTER_TYPES) {
            boolean found = false;
            for (int pos = startPos; pos < this.filterListAdapter.getItemCount(); pos++) {
                final IFilterViewHolder<?> fvh = this.filterListAdapter.getItem(pos);
                if (fvh.getType() == type) {
                    if (pos > startPos) {
                        final IFilterViewHolder<?> item = this.filterListAdapter.removeItem(pos);
                        this.filterListAdapter.addItem(startPos, item);
                    }
                    final IFilterViewHolder<?> item = this.filterListAdapter.getItem(startPos);
                    if (item.isOf(StatusFilterViewHolder.class)) {
                        item.castTo(StatusFilterViewHolder.class).setSimpleView(true);
                    }
                    found = true;
                    break;
                }
            }
            if (!found) {
                final IFilterViewHolder<?> item = FilterViewHolderCreator.createFor(type, this);
                if (item.isOf(StatusFilterViewHolder.class)) {
                    item.castTo(StatusFilterViewHolder.class).setSimpleView(true);
                }
                this.filterListAdapter.addItem(startPos, item);
            }
            startPos++;
        }
        while (this.filterListAdapter.getItemCount() > BASIC_FILTER_TYPES.length) {
            this.filterListAdapter.removeItem(this.filterListAdapter.getItemCount() - 1);
        }
        adjustFilterEmptyView();
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

        public IFilterViewHolder<?> getFilterViewHolder() {
            return this.filterViewHolder;
        }

        public void setControlsEnabled(final boolean enabled) {
            binding.filterDelete.setVisibility(enabled ? View.VISIBLE : View.GONE);
            binding.filterDrag.setVisibility(enabled ? View.VISIBLE : View.GONE);
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

        @NonNull
        @Override
        public ItemHolder onCreateViewHolder(@NonNull final ViewGroup parent, final int viewType) {
            final View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.cache_filter_list_item, parent, false);
            final ItemHolder viewHolder = new ItemHolder(view);
            viewHolder.setControlsEnabled(isAdvancedView());
            viewHolder.binding.filterDelete.setOnClickListener(v -> {
                removeItem(viewHolder.getBindingAdapterPosition());
                adjustFilterEmptyView();
            });
            registerStartDrag(viewHolder, viewHolder.binding.filterDrag);
            return viewHolder;
        }

        @Override
        public void onBindViewHolder(@NonNull final ItemHolder holder, final int position) {
            holder.setControlsEnabled(isAdvancedView());
            fillViewHolder(holder, getItem(position));
        }

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

 }

