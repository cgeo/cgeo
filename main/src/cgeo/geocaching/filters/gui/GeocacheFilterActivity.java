package cgeo.geocaching.filters.gui;

import cgeo.geocaching.R;
import cgeo.geocaching.activity.AbstractActionBarActivity;
import cgeo.geocaching.databinding.CacheFilterActivityBinding;
import cgeo.geocaching.databinding.CacheFilterListItemBinding;
import cgeo.geocaching.filters.core.AndGeocacheFilter;
import cgeo.geocaching.filters.core.GeocacheFilterType;
import cgeo.geocaching.filters.core.GeocacheFilterUtils;
import cgeo.geocaching.filters.core.IGeocacheFilter;
import cgeo.geocaching.filters.core.InconclusiveGeocacheFilter;
import cgeo.geocaching.filters.core.LogicalGeocacheFilter;
import cgeo.geocaching.filters.core.NotGeocacheFilter;
import cgeo.geocaching.filters.core.OrGeocacheFilter;
import cgeo.geocaching.models.Geocache;
import cgeo.geocaching.ui.recyclerview.ManagedListAdapter;
import cgeo.geocaching.utils.Log;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;




/**
 * Show a filter selection using an {@code ExpandableListView}.
 */
public class GeocacheFilterActivity extends AbstractActionBarActivity {

    public static final int REQUEST_SELECT_FILTER = 456;
    public static final String EXTRA_FILTER_INPUT = "efi";
    public static final String EXTRA_FILTER_RESULT = "efr";

    private CacheFilterActivityBinding binding;
    private FilterListAdapter filterListAdapter;

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setThemeAndContentView(R.layout.cache_filter_activity);
        binding = CacheFilterActivityBinding.bind(findViewById(R.id.activity_viewroot));
        binding.applyFilter.setOnClickListener(b -> setResult(true));
        binding.clearFilter.setOnClickListener(b -> setResult(false));



        // Get parameters from intent and basic cache information from database
        String inputFilter = null;
        final Bundle extras = getIntent().getExtras();
        if (extras != null) {
            inputFilter = extras.getString(EXTRA_FILTER_INPUT);
        }
        filterListAdapter = new FilterListAdapter(binding.filterList);
        initializeFilterAdd();

        fillViewFromFilter(inputFilter);
    }

    private void fillViewFromFilter(final String inputFilter) {
        binding.includeInconclusiveFilter.setChecked(false);
        binding.inverseFilter.setChecked(false);
        binding.andOrFilter.setChecked(false);

        if (inputFilter != null) {
            try {
                final List<IFilterViewHolder<?>> filterList = new ArrayList<>();
                IGeocacheFilter filter = GeocacheFilterUtils.createFilter(inputFilter);
                if (filter instanceof InconclusiveGeocacheFilter) {
                    binding.includeInconclusiveFilter.setChecked(true);
                    filter = filter.getChildren().get(0);
                }
                if (filter instanceof NotGeocacheFilter) {
                    binding.inverseFilter.setChecked(true);
                    filter = filter.getChildren().get(0);
                }
                if (filter instanceof LogicalGeocacheFilter) {
                    binding.andOrFilter.setChecked(filter instanceof OrGeocacheFilter);
                    for (IGeocacheFilter c : filter.getChildren()) {
                        filterList.add(FilterViewHolderUtils.createFor(c, binding.filterList.getContext()));
                    }
                }
                filterListAdapter.submitList(filterList);
            } catch (ParseException pe) {
                Log.w("Exception parsing input filter", pe);
            }
        }
    }

    private void initializeFilterAdd() {
        final List<String> addChoices = new ArrayList<>();
        addChoices.add("<Click here to add>");
        for (GeocacheFilterType gcf : GeocacheFilterType.values()) {
            addChoices.add(gcf.getUserDisplayableName());
        }
        final ArrayAdapter<CharSequence> adapter = new ArrayAdapter(this,
            android.R.layout.simple_spinner_item, addChoices);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.addFilter.setAdapter(adapter);
        binding.addFilter.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(final AdapterView<?> parent, final View view, final int position, final long id) {
                if (position > 0) {
                    filterListAdapter.addItem(0, FilterViewHolderUtils.createFor(GeocacheFilterType.values()[position - 1], binding.filterList.getContext()));
                    binding.addFilter.setSelection(0);
                    binding.filterList.smoothScrollToPosition(0);
                }
            }

            @Override
            public void onNothingSelected(final AdapterView<?> parent) {
                //empty on purpose
            }
        });

    }


    private void setResult(final boolean setFilter) {
        final Intent resultIntent = new Intent();

        if (setFilter) {
            final IGeocacheFilter filter = getFilterFromView();
            resultIntent.putExtra(EXTRA_FILTER_RESULT, GeocacheFilterUtils.getFilterConfig(filter));
        }
        FilterViewHolderUtils.clearListInfo();
        setResult(Activity.RESULT_OK, resultIntent);
        finish();
    }

    @NotNull
    private IGeocacheFilter getFilterFromView() {
        IGeocacheFilter filter = binding.andOrFilter.isChecked() ? new OrGeocacheFilter() : new AndGeocacheFilter();
        for (IFilterViewHolder<?> f : filterListAdapter.getItems()) {
            filter.addChild(FilterViewHolderUtils.createFrom(f));
        }
        if (binding.inverseFilter.isChecked()) {
            final IGeocacheFilter notFilter = new NotGeocacheFilter();
            notFilter.addChild(filter);
            filter = notFilter;
        }
        if (binding.includeInconclusiveFilter.isChecked()) {
            final IGeocacheFilter incFilter = new InconclusiveGeocacheFilter();
            incFilter.addChild(filter);
            filter = incFilter;
        }
        return filter;
    }

    public static void selectFilter(@NonNull final Activity context, final String filterString,
                                    final List<Geocache> filteredList, final boolean isComplete) {
        final Intent intent = new Intent(context, GeocacheFilterActivity.class);
        if (!StringUtils.isBlank(filterString)) {
            intent.putExtra(EXTRA_FILTER_INPUT, filterString);
        }
        FilterViewHolderUtils.setListInfo(filteredList, isComplete);
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

        private Context viewCreateContext;

        private FilterListAdapter(final RecyclerView recyclerView) {
            super(new ManagedListAdapter.Config(recyclerView)
                .setNotifyOnPositionChange(true)
                .setSupportDragDrop(true));
            this.viewCreateContext = recyclerView.getContext();
        }

        private void fillViewHolder(final FilterViewHolder holder, final IFilterViewHolder<?> filter) {
            if (filter == null) {
                return;
            }
            holder.binding.filterTitle.setText("Filter: " + filter.getType().getUserDisplayableName());

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
            viewHolder.binding.filterDelete.setOnClickListener(v -> removeItem(viewHolder.getAdapterPosition()));
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

