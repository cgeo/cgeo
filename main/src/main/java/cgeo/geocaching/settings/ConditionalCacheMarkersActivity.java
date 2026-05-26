package cgeo.geocaching.settings;

import cgeo.geocaching.R;
import cgeo.geocaching.activity.AbstractActionBarActivity;
import cgeo.geocaching.databinding.ActivityConditionalMarkersBinding;
import cgeo.geocaching.databinding.ConditionalMarkerListItemBinding;
import cgeo.geocaching.filters.core.GeocacheFilter;
import cgeo.geocaching.filters.core.GeocacheFilterContext;
import cgeo.geocaching.filters.gui.GeocacheFilterActivity;
import cgeo.geocaching.models.ConditionalCacheMarker;
import cgeo.geocaching.ui.recyclerview.AbstractRecyclerViewHolder;
import cgeo.geocaching.ui.recyclerview.ManagedListAdapter;
import cgeo.geocaching.utils.EmojiUtils;
import cgeo.geocaching.utils.JsonUtils;
import cgeo.geocaching.utils.LocalizationUtils;
import cgeo.geocaching.utils.MapMarkerUtils;

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

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.google.android.material.button.MaterialButton;

public class ConditionalCacheMarkersActivity extends AbstractActionBarActivity {

    private ActivityConditionalMarkersBinding binding;
    private ConditionalMarkerListAdapter markerAdapter;
    private String originalJson;

    // -------------------------------------------------------------------------
    // View holder
    // -------------------------------------------------------------------------

    protected static final class MarkerViewHolder extends AbstractRecyclerViewHolder {
        final ConditionalMarkerListItemBinding itemBinding;

        MarkerViewHolder(final View rowView) {
            super(rowView);
            itemBinding = ConditionalMarkerListItemBinding.bind(rowView);
        }
    }

    // -------------------------------------------------------------------------
    // Adapter
    // -------------------------------------------------------------------------

    private final class ConditionalMarkerListAdapter extends ManagedListAdapter<ConditionalCacheMarker, MarkerViewHolder> {

        ConditionalMarkerListAdapter(final androidx.recyclerview.widget.RecyclerView recyclerView) {
            super(new ManagedListAdapter.Config(recyclerView)
                    .setSupportDragDrop(true)
                    .setNotifyOnPositionChange(true));
        }

        private void fillViewHolder(final MarkerViewHolder holder, final ConditionalCacheMarker item) {
            if (item == null) {
                return;
            }
            // Marker button: show emoji glyph if set, else icon
            final int markerId = item.getMarkerId();
            final MaterialButton markerBtn = holder.itemBinding.markerButton;
            if (markerId != EmojiUtils.NO_EMOJI) {
                markerBtn.setText(EmojiUtils.getEmojiAsString(markerId));
                markerBtn.setIcon(null);
            } else {
                markerBtn.setText(null);
                markerBtn.setIconResource(R.drawable.ic_menu_emoticons);
            }

            // Filter label: null or non-filtering (empty) filter = marks all caches
            final GeocacheFilter filter = item.getFilter();
            if (filter == null || !filter.isFiltering()) {
                holder.itemBinding.filterLabel.setText(LocalizationUtils.getString(R.string.conditional_marker_nofilter_label));
            } else {
                holder.itemBinding.filterLabel.setText(filter.toUserDisplayableString());
            }
        }

        @NonNull
        @Override
        public MarkerViewHolder onCreateViewHolder(@NonNull final ViewGroup parent, final int viewType) {
            final View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.conditional_marker_list_item, parent, false);
            final MarkerViewHolder holder = new MarkerViewHolder(view);

            // Marker edit button
            holder.itemBinding.markerButton.setOnClickListener(v -> {
                final int pos = holder.getBindingAdapterPosition();
                if (pos == androidx.recyclerview.widget.RecyclerView.NO_ID) {
                    return;
                }
                final ConditionalCacheMarker rule = getItem(pos);
                EmojiUtils.selectEmojiPopup(
                        ConditionalCacheMarkersActivity.this,
                        rule.getMarkerId(),
                        null,
                        newMarkerId -> {
                            removeItem(pos);
                            addItem(pos, new ConditionalCacheMarker(newMarkerId, rule.getFilter()));
                            updateEmptyHint();
                        });
            });

            // Filter edit button
            holder.itemBinding.filterButton.setOnClickListener(v -> {
                final int pos = holder.getBindingAdapterPosition();
                if (pos == androidx.recyclerview.widget.RecyclerView.NO_ID) {
                    return;
                }
                final ConditionalCacheMarker rule = getItem(pos);
                final GeocacheFilterContext ctx = new GeocacheFilterContext(GeocacheFilterContext.FilterType.TRANSIENT);
                final GeocacheFilter existingFilter = rule.getFilter();
                if (existingFilter != null) {
                    ctx.set(existingFilter);
                }
                // Store position in tag so onActivityResult can find it
                holder.itemBinding.filterButton.setTag(pos);
                GeocacheFilterActivity.selectFilter(
                        ConditionalCacheMarkersActivity.this,
                        ctx,
                        null,
                        false);
                // We store the position separately for use in onActivityResult
                pendingFilterEditPosition = pos;
            });

            // Delete button
            holder.itemBinding.deleteButton.setOnClickListener(v -> {
                final int pos = holder.getBindingAdapterPosition();
                if (pos != androidx.recyclerview.widget.RecyclerView.NO_ID) {
                    removeItem(pos);
                    updateEmptyHint();
                }
            });

            // Drag handle
            registerStartDrag(holder, holder.itemBinding.dragHandle);

            return holder;
        }

        @Override
        public void onBindViewHolder(@NonNull final MarkerViewHolder holder, final int position) {
            fillViewHolder(holder, getItem(position));
        }
    }

    // -------------------------------------------------------------------------
    // Activity lifecycle
    // -------------------------------------------------------------------------

    private int pendingFilterEditPosition = -1;

    @Override
    public void onCreate(@Nullable final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTheme();
        setTitle(LocalizationUtils.getString(R.string.conditional_marker_activity_title));

        binding = ActivityConditionalMarkersBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        markerAdapter = new ConditionalMarkerListAdapter(binding.markerList);

        final List<ConditionalCacheMarker> rules = new ArrayList<>(Settings.getConditionalCacheMarkers());
        markerAdapter.setItems(rules);
        originalJson = rulesToJson(rules);

        updateEmptyHint();
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        getMenuInflater().inflate(R.menu.menu_conditional_markers, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        final int itemId = item.getItemId();
        if (itemId == R.id.menu_item_save) {
            saveAndFinish();
            return true;
        } else if (itemId == R.id.menu_item_cancel) {
            finish();
            return true;
        } else if (itemId == android.R.id.home) {
            finish();
            return true;
        } else if (itemId == R.id.menu_item_add) {
            addNewRule();
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
            if (resultCtx != null && pendingFilterEditPosition < markerAdapter.getItemCount()) {
                final ConditionalCacheMarker rule = markerAdapter.getItem(pendingFilterEditPosition);
                // Store the returned filter as-is: non-null, evaluated on match (empty filter matches all caches)
                final ConditionalCacheMarker updated = new ConditionalCacheMarker(rule.getMarkerId(), resultCtx.get());
                markerAdapter.removeItem(pendingFilterEditPosition);
                markerAdapter.addItem(pendingFilterEditPosition, updated);
            }
            pendingFilterEditPosition = -1;
        }
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private void addNewRule() {
        final ConditionalCacheMarker newRule = new ConditionalCacheMarker(EmojiUtils.NO_EMOJI, null);
        final List<ConditionalCacheMarker> items = new ArrayList<>(markerAdapter.getItems());
        items.add(newRule);
        markerAdapter.setItems(items);
        updateEmptyHint();
    }

    private void saveAndFinish() {
        Settings.setConditionalCacheMarkers(markerAdapter.getItems());
        MapMarkerUtils.clearCachedItems();
        finish();
    }

    private void updateEmptyHint() {
        final boolean isEmpty = markerAdapter.getItemCount() == 0;
        binding.emptyHint.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        binding.markerList.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
    }

    @NonNull
    private static String rulesToJson(final List<ConditionalCacheMarker> rules) {
        final ArrayNode array = JsonUtils.createArrayNode();
        for (final ConditionalCacheMarker rule : rules) {
            array.add(rule.toJson());
        }
        return JsonUtils.nodeToString(array);
    }
}




