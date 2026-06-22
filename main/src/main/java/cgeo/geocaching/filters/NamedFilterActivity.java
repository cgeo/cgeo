package cgeo.geocaching.filters;

import cgeo.geocaching.R;
import cgeo.geocaching.activity.AbstractActionBarActivity;
import cgeo.geocaching.activity.ActivityMixin;
import cgeo.geocaching.databinding.ActivityNamedFilterBinding;
import cgeo.geocaching.databinding.NamedFilterListItemBinding;
import cgeo.geocaching.filters.core.GeocacheFilter;
import cgeo.geocaching.filters.core.GeocacheFilterContext;
import cgeo.geocaching.filters.gui.GeocacheFilterActivity;
import cgeo.geocaching.ui.TextSpinner;
import cgeo.geocaching.ui.ViewUtils;
import cgeo.geocaching.ui.dialog.SimpleDialog;
import cgeo.geocaching.ui.recyclerview.AbstractRecyclerViewHolder;
import cgeo.geocaching.ui.recyclerview.ManagedListAdapter;
import cgeo.geocaching.utils.EmojiUtils;
import cgeo.geocaching.utils.LocalizationUtils;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Arrays;
import java.util.List;
import java.util.function.BooleanSupplier;

import com.google.android.material.button.MaterialButton;
import org.apache.commons.lang3.StringUtils;

public class NamedFilterActivity extends AbstractActionBarActivity {

    private static final String STATE_CURRENT_NAMED_FILTER_EDIT = "state_current_named_filter:edit";

    private ActivityNamedFilterBinding binding;
    private NamedFilterListAdapter filterAdapter;
    private int pendingFilterEditPosition = -1;
    private BooleanSupplier navUpHandler;

    /** Guard flag to prevent the header checkbox listener from firing during programmatic updates. */
    private boolean updatingEnableAllCheckbox = false;

    // -------------------------------------------------------------------------
    // Static helpers
    // -------------------------------------------------------------------------

    public static void startActivity(@NonNull final Activity activity) {
        final Intent intent = new Intent(activity, NamedFilterActivity.class);
        activity.startActivity(intent);
    }

    // -------------------------------------------------------------------------
    // View holder
    // -------------------------------------------------------------------------

    protected static final class NamedFilterViewHolder extends AbstractRecyclerViewHolder {
        final NamedFilterListItemBinding itemBinding;
        final TextSpinner<NamedFilter.MarkerPriority> markerPrioSpinner = new TextSpinner<>();

        NamedFilterViewHolder(final View rowView) {
            super(rowView);
            itemBinding = NamedFilterListItemBinding.bind(rowView);
            markerPrioSpinner.setValues(Arrays.asList(NamedFilter.MarkerPriority.values()))
                .setTextView(itemBinding.markerPriority)
                .setTextDisplayMapperPure(rp -> rp.getL10n() + " ▼")
                .setDisplayMapperPure(NamedFilter.MarkerPriority::getL10n);
        }
    }

    // -------------------------------------------------------------------------
    // Adapter
    // -------------------------------------------------------------------------

    private final class NamedFilterListAdapter extends ManagedListAdapter<NamedFilter, NamedFilterViewHolder> {

        NamedFilterListAdapter(final androidx.recyclerview.widget.RecyclerView recyclerView) {
            super(new ManagedListAdapter.Config(recyclerView)
                    .setSupportDragDrop(true)
                    .setNotifyOnPositionChange(true));
        }

        private void fillViewHolder(final NamedFilterViewHolder holder, final NamedFilter item) {
            if (item == null) {
                return;
            }

            // Row 1: marker button
            final String markerId = item.getMarkerId();
            final MaterialButton markerBtn = holder.itemBinding.markerButton;
            if (StringUtils.isNotBlank(markerId)) {
                markerBtn.setText(markerId);
                markerBtn.setIcon(null);
            } else {
                markerBtn.setText(null);
                markerBtn.setIconResource(R.drawable.ic_menu_marker_off);
            }

            // Row 1: filter name
            holder.itemBinding.filterName.setText(item.getName());

            // Row 2: filter description
            final GeocacheFilter filter = item.getFilter();
            if (filter == null || !filter.isFiltering()) {
                holder.itemBinding.filterDescription.setText(LocalizationUtils.getString(R.string.named_filter_nofilter_label));
            } else {
                holder.itemBinding.filterDescription.setText(filter.toUserDisplayableString());
            }

            // Row 3: activate marker checkbox
            holder.itemBinding.activateMarkerCheckbox.setOnCheckedChangeListener(null);
            holder.itemBinding.activateMarkerCheckbox.setChecked(item.isConditionalMarkerActive());
            holder.itemBinding.activateMarkerCheckbox.setOnCheckedChangeListener((v, checked) -> {
                final int pos = holder.getBindingAdapterPosition();
                if (pos != androidx.recyclerview.widget.RecyclerView.NO_ID) {
                    getItem(pos).setConditionalMarkerActive(checked);
                    updateEnableAllMarkersCheckbox();
                }
            });
            holder.markerPrioSpinner.set(item.getConditionalMarkerPriority());
        }

        @NonNull
        @Override
        public NamedFilterViewHolder onCreateViewHolder(@NonNull final ViewGroup parent, final int viewType) {
            final View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.named_filter_list_item, parent, false);
            final NamedFilterViewHolder holder = new NamedFilterViewHolder(view);

            // Marker button
            holder.itemBinding.markerButton.setOnClickListener(v -> {
                final int pos = holder.getBindingAdapterPosition();
                if (pos == androidx.recyclerview.widget.RecyclerView.NO_ID) {
                    return;
                }
                final NamedFilter nf = getItem(pos);
                EmojiUtils.selectEmojiPopup(NamedFilterActivity.this, nf.getMarkerId(), false, null, newMarkerId -> {
                    nf.setMarkerId(newMarkerId);
                    notifyItemChanged(pos);
                    updateEmptyHint();
                });
            });

            // Edit filter button
            holder.itemBinding.editFilterButton.setOnClickListener(v -> {
                final int pos = holder.getBindingAdapterPosition();
                if (pos == androidx.recyclerview.widget.RecyclerView.NO_ID) {
                    return;
                }
                final NamedFilter nf = getItem(pos);
                final GeocacheFilterContext ctx = new GeocacheFilterContext(GeocacheFilterContext.FilterType.TRANSIENT);
                if (nf.getFilter() != null) {
                    ctx.set(nf.getFilter());
                }
                pendingFilterEditPosition = pos;
                //we have to save before opening filter editor, otherwise edited filters may not show up there correctly
                saveFilters();
                GeocacheFilterActivity.selectFilter(NamedFilterActivity.this, ctx, null, false);
            });

            // Delete button
            holder.itemBinding.deleteButton.setOnClickListener(v -> {
                final int pos = holder.getBindingAdapterPosition();
                if (pos != androidx.recyclerview.widget.RecyclerView.NO_ID) {
                    removeItem(pos);
                    updateEmptyHint();
                }
            });

            //Name field
            holder.itemBinding.filterName.addTextChangedListener(ViewUtils.createSimpleWatcher(s -> {
                final int pos = holder.getBindingAdapterPosition();
                if (pos == androidx.recyclerview.widget.RecyclerView.NO_ID) {
                    return;
                }
                final NamedFilter nf = getItem(pos);
                nf.setName(s.toString());
            }));

            // Marker priority spinner
            holder.markerPrioSpinner.setChangeListener(mp -> {
                final int pos = holder.getBindingAdapterPosition();
                if (pos == androidx.recyclerview.widget.RecyclerView.NO_ID) {
                    return;
                }
                final NamedFilter nf = getItem(pos);
                nf.setConditionalMarkerPriority(mp);
            });

            return holder;
        }

        @Override
        public void onBindViewHolder(@NonNull final NamedFilterViewHolder holder, final int position) {
            fillViewHolder(holder, getItem(position));
        }
    }

    // -------------------------------------------------------------------------
    // Activity lifecycle
    // -------------------------------------------------------------------------

    @SuppressLint("NotifyDataSetChanged")
    @Override
    public void onCreate(@Nullable final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTheme();
        setTitle(LocalizationUtils.getString(R.string.named_filter_activity_title));

        binding = ActivityNamedFilterBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        filterAdapter = new NamedFilterListAdapter(binding.namedFilterList);

        final List<NamedFilter> filters;
        if (savedInstanceState != null && savedInstanceState.getString(STATE_CURRENT_NAMED_FILTER_EDIT) != null) {
            filters = NamedFilter.createFromListConfig(savedInstanceState.getString(STATE_CURRENT_NAMED_FILTER_EDIT));
        } else {
            filters = NamedFilter.getAllDeepCopy();
        }
        filterAdapter.setItems(filters);

        navUpHandler = ActivityMixin.registerBackNavigationInterceptor(this, this::onNavigationIntercepted);

        binding.enableAllMarkersCheckbox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (updatingEnableAllCheckbox) {
                return;
            }
            final List<NamedFilter> items = filterAdapter.getItems();
            for (final NamedFilter nf : items) {
                nf.setConditionalMarkerActive(isChecked);
            }
            filterAdapter.notifyDataSetChanged();
        });

        updateEmptyHint();
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        getMenuInflater().inflate(R.menu.menu_named_filter, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        final int itemId = item.getItemId();
        if (itemId == R.id.menu_item_save) {
            saveFiltersAndFinish();
            return true;
        } else if (itemId == R.id.menu_item_cancel) {
            return onSupportNavigateUp();
        } else if (itemId == android.R.id.home) {
            return onSupportNavigateUp();
        } else if (itemId == R.id.menu_item_add) {
            addNewFilter();
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

    private boolean onNavigationIntercepted(final Runnable navigationAction) {
        final String currentJson = NamedFilter.toListConfig(filterAdapter.getItems());
        final String storedJson = NamedFilter.toListConfig(NamedFilter.getAll());
        if (storedJson != null && !storedJson.equals(currentJson)) {
            SimpleDialog.of(this).setTitle(R.string.confirm_unsaved_changes_title).setMessage(R.string.confirm_discard_changes)
                    .confirm(navigationAction);
            return true;
        }
        return false;
    }

    @Override
    protected void onActivityResult(final int requestCode, final int resultCode, @Nullable final Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == GeocacheFilterActivity.REQUEST_SELECT_FILTER
                && resultCode == Activity.RESULT_OK
                && data != null
                && pendingFilterEditPosition >= 0) {
            final GeocacheFilterContext resultCtx = data.getParcelableExtra(GeocacheFilterActivity.EXTRA_FILTER_CONTEXT);
            if (resultCtx != null && pendingFilterEditPosition < filterAdapter.getItemCount()) {
                filterAdapter.getItem(pendingFilterEditPosition).setFilter(resultCtx.get());
                filterAdapter.notifyItemChanged(pendingFilterEditPosition);
            }
            pendingFilterEditPosition = -1;
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull final Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(STATE_CURRENT_NAMED_FILTER_EDIT, NamedFilter.toListConfig(filterAdapter.getItems()));
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private void addNewFilter() {
        SimpleDialog.of(this)
                .setTitle(R.string.named_filter_add)
                .input(new SimpleDialog.InputOptions(), newName -> {
                    final NamedFilter nf = new NamedFilter(newName == null ? "" : newName, null);
                    filterAdapter.addItem(0, nf);
                    updateEmptyHint();
                    updateEnableAllMarkersCheckbox();
                });
    }

    private void saveFiltersAndFinish() {
        saveFilters();
        finish();
    }

    private void saveFilters() {
        final List<NamedFilter> items = filterAdapter.getItems();
        NamedFilter.storeAll(items);
    }

    private void updateEmptyHint() {
        final boolean isEmpty = filterAdapter.getItemCount() == 0;
        binding.emptyHint.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        binding.enableAllMarkersCheckbox.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
        binding.namedFilterList.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
        updateEnableAllMarkersCheckbox();
    }

    private void updateEnableAllMarkersCheckbox() {
        final List<NamedFilter> items = filterAdapter.getItems();
        final boolean allActive = !items.isEmpty() && items.stream().allMatch(NamedFilter::isConditionalMarkerActive);
        updatingEnableAllCheckbox = true;
        binding.enableAllMarkersCheckbox.setChecked(allActive);
        updatingEnableAllCheckbox = false;
    }

}

