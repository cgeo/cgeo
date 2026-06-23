package cgeo.geocaching.filters;

import cgeo.geocaching.R;
import cgeo.geocaching.activity.AbstractActionBarActivity;
import cgeo.geocaching.databinding.ActivityNamedFilterBinding;
import cgeo.geocaching.databinding.NamedFilterPriorityItemBinding;
import cgeo.geocaching.ui.recyclerview.AbstractRecyclerViewHolder;
import cgeo.geocaching.ui.recyclerview.ManagedListAdapter;
import cgeo.geocaching.utils.LocalizationUtils;

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
import androidx.recyclerview.widget.RecyclerView;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

public class NamedFilterPriorityActivity extends AbstractActionBarActivity {

    private ActivityNamedFilterBinding binding;
    private FilterPriorityAdapter filterAdapter;

    public static void startActivity(@NonNull final Activity activity) {
        activity.startActivity(new Intent(activity, NamedFilterPriorityActivity.class));
    }

    protected static final class FilterPriorityViewHolder extends AbstractRecyclerViewHolder {
        final NamedFilterPriorityItemBinding itemBinding;

        FilterPriorityViewHolder(final View view) {
            super(view);
            itemBinding = NamedFilterPriorityItemBinding.bind(view);
        }
    }

    private final class FilterPriorityAdapter extends ManagedListAdapter<NamedFilter, FilterPriorityViewHolder> {

        FilterPriorityAdapter(final RecyclerView recyclerView) {
            super(new Config(recyclerView).setSupportDragDrop(true));
        }

        @NonNull
        @Override
        public FilterPriorityViewHolder onCreateViewHolder(@NonNull final ViewGroup parent, final int viewType) {
            final View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.named_filter_priority_item, parent, false);
            final FilterPriorityViewHolder holder = new FilterPriorityViewHolder(view);
            registerStartDrag(holder, holder.itemBinding.dragHandle);
            return holder;
        }

        @Override
        public void onBindViewHolder(@NonNull final FilterPriorityViewHolder holder, final int position) {
            final NamedFilter item = getItem(position);
            if (item == null) {
                return;
            }
            final String markerId = item.getMarkerId();
            holder.itemBinding.filterIcon.setText(StringUtils.isNotBlank(markerId) ? markerId : "");
            holder.itemBinding.filterName.setText(item.getName());
        }
    }

    @Override
    public void onCreate(@Nullable final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTheme();
        setTitle(LocalizationUtils.getString(R.string.named_filter_activity_title));

        binding = ActivityNamedFilterBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        filterAdapter = new FilterPriorityAdapter(binding.namedFilterList);

        final List<NamedFilter> iconFilters = NamedFilter.getAllWithIcons().stream()
                .filter(nf -> nf.getMarkerId() != null)
                .sorted(Comparator.comparingInt(NamedFilter::getConditionalMarkerPriority))
                .collect(Collectors.toList());
        filterAdapter.setItems(iconFilters);

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
            savePrioritiesAndFinish();
            return true;
        } else if (itemId == R.id.menu_item_cancel || itemId == android.R.id.home) {
            finish();
            return true;
        }
        return false;
    }

    private void savePrioritiesAndFinish() {
        final List<NamedFilter> orderedItems = filterAdapter.getItems();
        final Map<Integer, Integer> priorityByFilterId = new HashMap<>();
        for (int i = 0; i < orderedItems.size(); i++) {
            priorityByFilterId.put(orderedItems.get(i).getId(), i);
        }
        final List<NamedFilter> allFilters = NamedFilter.getAllDeepCopy();
        for (final NamedFilter nf : allFilters) {
            final Integer newPriority = priorityByFilterId.get(nf.getId());
            if (newPriority != null) {
                nf.setConditionalMarkerPriority(newPriority);
            }
        }
        NamedFilter.storeAll(allFilters);
        finish();
    }

    private void updateEmptyHint() {
        final boolean isEmpty = filterAdapter.getItemCount() == 0;
        binding.emptyHint.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        binding.namedFilterList.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
    }
}
