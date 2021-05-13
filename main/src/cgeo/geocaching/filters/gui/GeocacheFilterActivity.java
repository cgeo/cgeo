package cgeo.geocaching.filters.gui;

import cgeo.geocaching.R;
import cgeo.geocaching.activity.AbstractActionBarActivity;
import cgeo.geocaching.databinding.CacheFilterActivityBinding;
import cgeo.geocaching.databinding.CacheFilterListItemBinding;
import cgeo.geocaching.filters.core.AndGeocacheFilter;
import cgeo.geocaching.filters.core.GeocacheFilter;
import cgeo.geocaching.filters.core.GeocacheFilterType;
import cgeo.geocaching.filters.core.IGeocacheFilter;
import cgeo.geocaching.filters.core.InconclusiveGeocacheFilter;
import cgeo.geocaching.filters.core.LogicalGeocacheFilter;
import cgeo.geocaching.filters.core.NotGeocacheFilter;
import cgeo.geocaching.filters.core.OrGeocacheFilter;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.ui.TextSpinner;
import cgeo.geocaching.ui.ViewUtils;
import cgeo.geocaching.ui.dialog.Dialogs;
import cgeo.geocaching.ui.recyclerview.ManagedListAdapter;
import cgeo.geocaching.utils.Log;
import cgeo.geocaching.utils.TextUtils;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
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
import java.util.Collections;
import java.util.List;

import org.jetbrains.annotations.NotNull;




/**
 * Show a filter selection using an {@code ExpandableListView}.
 */
public class GeocacheFilterActivity extends AbstractActionBarActivity {

    public static final int REQUEST_SELECT_FILTER = 456;
    public static final String EXTRA_FILTER_INPUT = "efi";
    public static final String EXTRA_FILTER_RESULT = "efr";

    private static final String STATE_CURRENT_FILTER = "state_current_filter";

    private CacheFilterActivityBinding binding;
    private FilterListAdapter filterListAdapter;

    private CheckBox andOrFilterCheckbox;
    private CheckBox inverseFilterCheckbox;
    private CheckBox includeInconclusiveFilterCheckbox;

    private final TextSpinner<GeocacheFilterType> addFilter = new TextSpinner<>();

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTheme(Settings.isLightSkin() ? R.style.light_material : R.style.dark_material);
        setContentView(R.layout.cache_filter_activity);
        //setThemeAndContentView(R.layout.cache_filter_activity);
        binding = CacheFilterActivityBinding.bind(findViewById(R.id.activity_viewroot));

        binding.filterPropsCheckboxes.removeAllViews();
        this.andOrFilterCheckbox = ViewUtils.addCheckboxProperty(this, binding.filterPropsCheckboxes, R.string.cache_filter_option_and_or, R.drawable.ic_menu_logic);
        this.inverseFilterCheckbox = ViewUtils.addCheckboxProperty(this, binding.filterPropsCheckboxes, R.string.cache_filter_option_inverse, R.drawable.ic_menu_invert);
        this.includeInconclusiveFilterCheckbox = ViewUtils.addCheckboxProperty(this, binding.filterPropsCheckboxes, R.string.cache_filter_option_include_inconclusive, R.drawable.ic_menu_vague,
            R.string.cache_filter_option_include_inconclusive_info);


        filterListAdapter = new FilterListAdapter(binding.filterList);
        initializeFilterAdd();
        initializeStorageOptions();

        // Get parameters from intent and basic cache information from database
        String inputFilter = null;
        final Bundle extras = getIntent().getExtras();

        if (extras != null) {
            inputFilter = extras.getString(EXTRA_FILTER_INPUT);
        }

        fillViewFromFilter(inputFilter);
    }

    private void initializeStorageOptions() {

        //handling of "save" button
        binding.filterStorageSave.setOnClickListener(v ->
            Dialogs.input(this, getString(R.string.cache_filter_storage_save_title), InputType.TYPE_CLASS_TEXT,  binding.filterStorageName.getText().toString(), null, android.R.string.ok, newName -> {
                final GeocacheFilter filter = getFilterFromView();
                if (GeocacheFilter.Storage.existsAndDiffers(newName, filter)) {
                    Dialogs.confirm(this, R.string.cache_filter_storage_save_confirm_title, getString(R.string.cache_filter_storage_save_confirm_message, newName),
                        (dialog, which) -> saveAs(newName));
                } else {
                    saveAs(newName);
                }
            }));
        ViewUtils.setTooltip(binding.filterStorageSave, R.string.cache_filter_storage_save_title);

        //handling of "load/delete" button
        binding.filterStorageManage.setOnClickListener(v -> {
                final List<GeocacheFilter> filters = new ArrayList<>(GeocacheFilter.Storage.getStoredFilters());

                if (filters.isEmpty()) {
                    Dialogs.message(this, R.string.cache_filter_storage_load_delete_title, R.string.cache_filter_storage_load_delete_nofilter_message);
                } else {
                    Dialogs.select(this, filters, GeocacheFilter::getName, -1, R.string.cache_filter_storage_load_delete_title, R.string.cache_filter_storage_load_button, R.string.cache_filter_storage_delete_button, (b, f) -> {
                        switch (b) {
                            case DialogInterface.BUTTON_POSITIVE:
                                fillViewFromFilter(f.toConfig());
                                break;
                            case DialogInterface.BUTTON_NEGATIVE:
                                GeocacheFilter.Storage.delete(f);
                                //if currently shown view was just deleted -> then delete it in view as well
                                if (f.getName().contentEquals(binding.filterStorageName.getText())) {
                                    binding.filterStorageName.setText("");
                                }
                                break;
                            default:
                                break;
                        }
                    });
                }
            });
        ViewUtils.setTooltip(binding.filterStorageManage, R.string.cache_filter_storage_load_delete_title);

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
    }

    @Override
    protected void onRestoreInstanceState(@NonNull final Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        if (savedInstanceState.getString(STATE_CURRENT_FILTER) != null) {
            fillViewFromFilter(savedInstanceState.getString(STATE_CURRENT_FILTER));
        }
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        super.onCreateOptionsMenu(menu);
        menu.add(Menu.NONE, R.id.delete, 0, "Delete").setIcon(R.drawable.ic_menu_delete)
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        menu.add(Menu.NONE, R.id.menu_send, 1, "Go!").setIcon(R.drawable.ic_menu_send)
            .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        // Handle presses on the action bar items
        final int itemId = item.getItemId();
        if (itemId == R.id.delete) {
            setResult(false);
            return true;
        } else if (itemId == R.id.menu_send) {
            setResult(true);
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }


    private void fillViewFromFilter(final String inputFilter) {
        includeInconclusiveFilterCheckbox.setChecked(false);
        inverseFilterCheckbox.setChecked(false);
        andOrFilterCheckbox.setChecked(false);

        if (inputFilter != null) {
            try {
                final List<IFilterViewHolder<?>> filterList = new ArrayList<>();
                final GeocacheFilter filter = GeocacheFilter.checkConfig(inputFilter);
                binding.filterStorageName.setText(filter.getName());
                IGeocacheFilter filterTree = filter.getTree();
                if (filterTree instanceof InconclusiveGeocacheFilter) {
                    includeInconclusiveFilterCheckbox.setChecked(true);
                    filterTree = filterTree.getChildren().get(0);
                }
                if (filterTree instanceof NotGeocacheFilter) {
                    inverseFilterCheckbox.setChecked(true);
                    filterTree = filterTree.getChildren().get(0);
                }
                if (filterTree instanceof LogicalGeocacheFilter) {
                    andOrFilterCheckbox.setChecked(filterTree instanceof OrGeocacheFilter);
                    for (IGeocacheFilter c : filterTree.getChildren()) {
                        //filterList.add(FilterViewHolderUtils.createFor(c, binding.filterList.getContext()));
                        filterList.add(FilterViewHolderCreator.createFor(c, this));
                    }
                }
                filterListAdapter.submitList(filterList, this::adjustFilterEmptyView);

            } catch (ParseException pe) {
                Log.w("Exception parsing input filter", pe);
            }
        }
    }

    private void initializeFilterAdd() {
        final List<GeocacheFilterType> filterTypes = new ArrayList<>(Arrays.asList(GeocacheFilterType.values()));
        Collections.sort(filterTypes, (left, right) -> TextUtils.COLLATOR.compare(left.getUserDisplayableName(), right.getUserDisplayableName()));
        addFilter.setValues(filterTypes)
            .setDisplayMapper(GeocacheFilterType::getUserDisplayableName)
            .setTextHideSelectionMarker(true)
            .setView(binding.filterAdditem, (v, t) -> { })
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



    private void setResult(final boolean setFilter) {
        final Intent resultIntent = new Intent();

        if (setFilter) {
            GeocacheFilter result = getFilterFromView();
            //if current filter config does not match currently shown saved filter -> do not store under this name
            if (GeocacheFilter.Storage.existsAndDiffers(binding.filterStorageName.getText().toString(), result)) {
                binding.filterStorageName.setText("");
                result = getFilterFromView();
            }
            resultIntent.putExtra(EXTRA_FILTER_RESULT, result.toConfig());
        }
        FilterViewHolderCreator.clearListInfo();
        setResult(Activity.RESULT_OK, resultIntent);
        finish();
    }


    @NotNull
    private GeocacheFilter getFilterFromView() {
        IGeocacheFilter filter = andOrFilterCheckbox.isChecked() ? new OrGeocacheFilter() : new AndGeocacheFilter();
        for (IFilterViewHolder<?> f : filterListAdapter.getItems()) {
            filter.addChild(FilterViewHolderCreator.createFrom(f));
        }
        if (inverseFilterCheckbox.isChecked()) {
            final IGeocacheFilter notFilter = new NotGeocacheFilter();
            notFilter.addChild(filter);
            filter = notFilter;
        }
        if (includeInconclusiveFilterCheckbox.isChecked()) {
            final IGeocacheFilter incFilter = new InconclusiveGeocacheFilter();
            incFilter.addChild(filter);
            filter = incFilter;
        }

        return new GeocacheFilter(binding.filterStorageName.getText().toString(), filter);
    }

    public static void selectFilter(@NonNull final Activity context, final GeocacheFilter filter,
                                    final List<Geocache> filteredList, final boolean isComplete) {
        final Intent intent = new Intent(context, GeocacheFilterActivity.class);
        if (filter != null) {
            intent.putExtra(EXTRA_FILTER_INPUT, filter.toConfig());
        }
        FilterViewHolderCreator.setListInfo(filteredList, isComplete);
        context.startActivityForResult(intent, REQUEST_SELECT_FILTER);
    }

    private static class FilterViewHolder extends RecyclerView.ViewHolder {
        private final CacheFilterListItemBinding binding;

        FilterViewHolder(final View rowView) {
            super(rowView);
            binding = CacheFilterListItemBinding.bind(rowView);
        }
    }

    private final class FilterListAdapter extends ManagedListAdapter<IFilterViewHolder<?>, FilterViewHolder> {


        private FilterListAdapter(final RecyclerView recyclerView) {
            super(new ManagedListAdapter.Config(recyclerView)
                .setNotifyOnPositionChange(true)
                .setSupportDragDrop(true));
        }

        private void fillViewHolder(final FilterViewHolder holder, final IFilterViewHolder<?> filter) {
            if (filter == null) {
                return;
            }
            holder.binding.filterTitle.setText(filter.getType().getUserDisplayableName());

            //create view
            final View view = filter.getView();

            setTheme();
            // insert into main view
            final ViewGroup insertPoint = holder.binding.insertPoint;
            insertPoint.removeAllViews(); //views are reused, so make sure to cleanup
            if (view.getParent() != null) {
                ((ViewGroup) view.getParent()).removeAllViews();
            }
            insertPoint.addView(view);

        }

        @NonNull
        @Override
        public FilterViewHolder onCreateViewHolder(@NonNull final ViewGroup parent, final int viewType) {
            final View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.cache_filter_list_item, parent, false);
            final FilterViewHolder viewHolder = new FilterViewHolder(view);
            viewHolder.binding.filterDelete.setOnClickListener(v -> {
                removeItem(viewHolder.getBindingAdapterPosition());
                adjustFilterEmptyView();
            });
            registerStartDrag(viewHolder, viewHolder.binding.filterDrag);
            return viewHolder;
        }

        @Override
        public void onBindViewHolder(@NonNull final FilterViewHolder holder, final int position) {
            fillViewHolder(holder, getItem(position));
        }

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

 }

