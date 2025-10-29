package cgeo.geocaching.connector.gc;

import cgeo.geocaching.CacheListActivity;
import cgeo.geocaching.R;
import cgeo.geocaching.databinding.GclistItemBinding;
import cgeo.geocaching.models.GCList;
import cgeo.geocaching.storage.extension.PocketQueryHistory;
import cgeo.geocaching.ui.recyclerview.AbstractRecyclerViewHolder;
import cgeo.geocaching.utils.Formatter;
import cgeo.geocaching.utils.Log;

import android.annotation.SuppressLint;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.StringUtils;


class AbstractListAdapter extends RecyclerView.Adapter<AbstractListAdapter.ViewHolder> {

    @NonNull private final AbstractListActivity activity;
    private boolean selectMode = false;
    private final List<GCList> selectedLists = new ArrayList<>();

    protected static final class ViewHolder extends AbstractRecyclerViewHolder {
        private final GclistItemBinding binding;

        ViewHolder(final View view) {
            super(view);
            binding = GclistItemBinding.bind(view);
        }
    }


    private static class TouchListener implements View.OnClickListener, View.OnTouchListener {

        private final GestureDetector gestureDetector;
        private final GCList gcList;
        @NonNull private final WeakReference<AbstractListAdapter> adapterRef;

        TouchListener(final GCList gcList, @NonNull final AbstractListAdapter adapter) {
            this.gcList = gcList;
            gestureDetector = new GestureDetector(adapter.activity.getBaseContext(), new AbstractListAdapter.FlingGesture(gcList, adapter));
            adapterRef = new WeakReference<>(adapter);
        }

        // Swipe on item
        @SuppressLint("ClickableViewAccessibility")
        @Override
        public boolean onTouch(final View view, final MotionEvent event) {
            return gestureDetector.onTouchEvent(event);

        }

        // Tap on item
        @Override
        public void onClick(final View view) {
            final AbstractListAdapter adapter = adapterRef.get();
            if (adapter == null) {
                return;
            }
            adapter.updateSelectedList(gcList);
        }
    }

    private static class FlingGesture extends GestureDetector.SimpleOnGestureListener {

        private final GCList gcList;
        private static final int SWIPE_MIN_DISTANCE = 60;
        private static final int SWIPE_MAX_OFF_PATH = 100;

        @NonNull private final WeakReference<AbstractListAdapter> adapterRef;

        FlingGesture(final GCList gcList, @NonNull final AbstractListAdapter adapter) {
            this.gcList = gcList;
            adapterRef = new WeakReference<>(adapter);
        }

        @Override
        public boolean onFling(final MotionEvent e1, final MotionEvent e2, final float velocityX, final float velocityY) {
            try {
                if (Math.abs(e1.getY() - e2.getY()) > SWIPE_MAX_OFF_PATH) {
                    return false;
                }
                final AbstractListAdapter adapter = adapterRef.get();
                if (adapter == null) {
                    return false;
                }

                // horizontal swipe
                if (Math.abs(velocityX) > Math.abs(velocityY)) {

                    // left to right swipe
                    if ((e2.getX() - e1.getX()) > SWIPE_MIN_DISTANCE) {
                        if (!adapter.selectMode) {
                            adapter.selectMode = true;
                            adapter.selectedLists.add(gcList);
                            adapter.updateView();
                        }
                        return true;
                    }

                    // right to left swipe
                    if ((e1.getX() - e2.getX()) > SWIPE_MIN_DISTANCE) {
                        if (adapter.selectMode) {
                            adapter.selectMode = false;
                            adapter.updateView();
                        }
                        return true;
                    }
                }
            } catch (final Exception e) {
                Log.w("AbstractListAdapter.FlingGesture.onFling", e);
            }

            return false;
        }
    }

    AbstractListAdapter(@NonNull final AbstractListActivity abstractListActivity) {
        this.activity = abstractListActivity;
    }

    @Override
    public int getItemCount() {
        return activity.getQueries().size();
    }

    public List<GCList> getSelectedLists() {
        return selectedLists;
    }

    @Override
    @NonNull
    public ViewHolder onCreateViewHolder(final ViewGroup parent, final int viewType) {
        final View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.gclist_item, parent, false);
        final ViewHolder viewHolder = new ViewHolder(view);

        return viewHolder;
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, final int position) {
        final GCList pocketQuery = activity.getQueries().get(position);
        holder.binding.download.setVisibility(pocketQuery.isDownloadable() && !selectMode ? View.VISIBLE : View.GONE);

        // Now we are able to parse bookmark lists without download
        holder.binding.cachelist.setVisibility((!pocketQuery.isBookmarkList() && StringUtils.isBlank(pocketQuery.getPqHash())) || selectMode ? View.GONE : View.VISIBLE);
        holder.binding.label.setText(pocketQuery.getName());
        final String info = Formatter.formatPocketQueryInfo(pocketQuery);
        holder.binding.info.setVisibility(StringUtils.isNotBlank(info) ? View.VISIBLE : View.GONE);
        holder.binding.info.setText(info);

        final boolean showCheckbox = selectMode && (StringUtils.isNotBlank(pocketQuery.getPqHash()) || pocketQuery.isBookmarkList());
        holder.binding.checkbox.setVisibility(showCheckbox ? View.VISIBLE : View.GONE);
        if (showCheckbox) {
            final boolean isSelected = selectedLists.contains(pocketQuery);
            holder.binding.checkbox.setChecked(isSelected);
        }

        holder.binding.cachelist.setOnClickListener(view1 -> CacheListActivity.startActivityPocket(holder.itemView.getContext(), Collections.singletonList(pocketQuery)));
        holder.binding.download.setOnClickListener(v -> {
            PocketQueryHistory.updateLastDownload(pocketQuery);
            notifyDataSetChanged();

            if (activity.getStartDownload()) {
                CacheListActivity.startActivityPocketDownload(holder.itemView.getContext(), Collections.singletonList(pocketQuery));
            } else {
                activity.returnResult(pocketQuery);
            }
        });

        final AbstractListAdapter.TouchListener touchListener = new AbstractListAdapter.TouchListener(pocketQuery, this);
        holder.binding.checkbox.setOnClickListener(touchListener);
        holder.itemView.setOnTouchListener(touchListener);
        holder.itemView.setOnClickListener(touchListener);

        holder.itemView.setOnLongClickListener(v -> {
            selectMode = !selectMode;
            final GCList selectedList = activity.getQueries().get(holder.getBindingAdapterPosition());
            updateSelectedList(selectedList);
            return true;
        });

    }

    public void updateView() {
        if (!selectMode) {
            selectedLists.clear();
        }

        activity.findViewById(R.id.switchAB).setVisibility(!selectMode ? View.VISIBLE : View.GONE);

        final View buttonPreviewSelected = activity.findViewById(R.id.cachelist_selected);
        buttonPreviewSelected.setVisibility(selectMode && activity.supportMultiPreview() ? View.VISIBLE : View.GONE);
        buttonPreviewSelected.setEnabled(!selectedLists.isEmpty());

        // enable download button only, all selected lists are downloadable
        final View buttonDownloadSelected = activity.findViewById(R.id.download_selected);
        final boolean enableDownload = !selectedLists.isEmpty() && selectedLists.stream().allMatch(GCList::isDownloadable);
        buttonDownloadSelected.setVisibility(selectMode ? View.VISIBLE : View.GONE);
        buttonDownloadSelected.setAlpha(enableDownload ? 1.0f : 0.4f);
        buttonDownloadSelected.setEnabled(enableDownload);

        notifyDataSetChanged();
    }

    private void updateSelectedList(final GCList selectedList) {
        if (selectMode) {
            final boolean isSelected = selectedLists.contains(selectedList);
            if (isSelected) {
                selectedLists.remove(selectedList);
            } else {
                selectedLists.add(selectedList);
            }
        }
        updateView();
    }
}
