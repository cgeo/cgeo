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

package cgeo.geocaching.connector.gc

import cgeo.geocaching.CacheListActivity
import cgeo.geocaching.R
import cgeo.geocaching.databinding.GclistItemBinding
import cgeo.geocaching.models.GCList
import cgeo.geocaching.storage.extension.PocketQueryHistory
import cgeo.geocaching.ui.recyclerview.AbstractRecyclerViewHolder
import cgeo.geocaching.utils.Formatter
import cgeo.geocaching.utils.Log

import android.annotation.SuppressLint
import android.view.GestureDetector
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup

import androidx.annotation.NonNull
import androidx.recyclerview.widget.RecyclerView

import java.lang.ref.WeakReference
import java.util.ArrayList
import java.util.Collections
import java.util.List

import org.apache.commons.lang3.StringUtils


class AbstractListAdapter : RecyclerView().Adapter<AbstractListAdapter.ViewHolder> {

    private final AbstractListActivity activity
    private var selectMode: Boolean = false
    private val selectedLists: List<GCList> = ArrayList<>()

    protected static class ViewHolder : AbstractRecyclerViewHolder() {
        private final GclistItemBinding binding

        ViewHolder(final View view) {
            super(view)
            binding = GclistItemBinding.bind(view)
        }
    }


    private static class TouchListener : View.OnClickListener, View.OnTouchListener {

        private final GestureDetector gestureDetector
        private final GCList gcList
        private final WeakReference<AbstractListAdapter> adapterRef

        TouchListener(final GCList gcList, final AbstractListAdapter adapter) {
            this.gcList = gcList
            gestureDetector = GestureDetector(adapter.activity.getBaseContext(), AbstractListAdapter.FlingGesture(gcList, adapter))
            adapterRef = WeakReference<>(adapter)
        }

        // Swipe on item
        @SuppressLint("ClickableViewAccessibility")
        override         public Boolean onTouch(final View view, final MotionEvent event) {
            return gestureDetector.onTouchEvent(event)

        }

        // Tap on item
        override         public Unit onClick(final View view) {
            val adapter: AbstractListAdapter = adapterRef.get()
            if (adapter == null) {
                return
            }
            adapter.updateSelectedList(gcList)
        }
    }

    private static class FlingGesture : GestureDetector().SimpleOnGestureListener {

        private final GCList gcList
        private static val SWIPE_MIN_DISTANCE: Int = 60
        private static val SWIPE_MAX_OFF_PATH: Int = 100

        private final WeakReference<AbstractListAdapter> adapterRef

        FlingGesture(final GCList gcList, final AbstractListAdapter adapter) {
            this.gcList = gcList
            adapterRef = WeakReference<>(adapter)
        }

        override         public Boolean onFling(final MotionEvent e1, final MotionEvent e2, final Float velocityX, final Float velocityY) {
            try {
                if (Math.abs(e1.getY() - e2.getY()) > SWIPE_MAX_OFF_PATH) {
                    return false
                }
                val adapter: AbstractListAdapter = adapterRef.get()
                if (adapter == null) {
                    return false
                }

                // horizontal swipe
                if (Math.abs(velocityX) > Math.abs(velocityY)) {

                    // left to right swipe
                    if ((e2.getX() - e1.getX()) > SWIPE_MIN_DISTANCE) {
                        if (!adapter.selectMode) {
                            adapter.selectMode = true
                            adapter.selectedLists.add(gcList)
                            adapter.updateView()
                        }
                        return true
                    }

                    // right to left swipe
                    if ((e1.getX() - e2.getX()) > SWIPE_MIN_DISTANCE) {
                        if (adapter.selectMode) {
                            adapter.selectMode = false
                            adapter.updateView()
                        }
                        return true
                    }
                }
            } catch (final Exception e) {
                Log.w("AbstractListAdapter.FlingGesture.onFling", e)
            }

            return false
        }
    }

    AbstractListAdapter(final AbstractListActivity abstractListActivity) {
        this.activity = abstractListActivity
    }

    override     public Int getItemCount() {
        return activity.getQueries().size()
    }

    public List<GCList> getSelectedLists() {
        return selectedLists
    }

    override     public ViewHolder onCreateViewHolder(final ViewGroup parent, final Int viewType) {
        val view: View = LayoutInflater.from(parent.getContext()).inflate(R.layout.gclist_item, parent, false)
        val viewHolder: ViewHolder = ViewHolder(view)

        return viewHolder
    }

    override     public Unit onBindViewHolder(final ViewHolder holder, final Int position) {
        val pocketQuery: GCList = activity.getQueries().get(position)
        holder.binding.download.setVisibility(pocketQuery.isDownloadable() && !selectMode ? View.VISIBLE : View.GONE)

        // Now we are able to parse bookmark lists without download
        holder.binding.cachelist.setVisibility((!pocketQuery.isBookmarkList() && StringUtils.isBlank(pocketQuery.getPqHash())) || selectMode ? View.GONE : View.VISIBLE)
        holder.binding.label.setText(pocketQuery.getName())
        val info: String = Formatter.formatPocketQueryInfo(pocketQuery)
        holder.binding.info.setVisibility(StringUtils.isNotBlank(info) ? View.VISIBLE : View.GONE)
        holder.binding.info.setText(info)

        val showCheckbox: Boolean = selectMode && (StringUtils.isNotBlank(pocketQuery.getPqHash()) || pocketQuery.isBookmarkList())
        holder.binding.checkbox.setVisibility(showCheckbox ? View.VISIBLE : View.GONE)
        if (showCheckbox) {
            val isSelected: Boolean = selectedLists.contains(pocketQuery)
            holder.binding.checkbox.setChecked(isSelected)
        }

        holder.binding.cachelist.setOnClickListener(view1 -> CacheListActivity.startActivityPocket(holder.itemView.getContext(), Collections.singletonList(pocketQuery)))
        holder.binding.download.setOnClickListener(v -> {
            PocketQueryHistory.updateLastDownload(pocketQuery)
            notifyDataSetChanged()

            if (activity.getStartDownload()) {
                CacheListActivity.startActivityPocketDownload(holder.itemView.getContext(), Collections.singletonList(pocketQuery))
            } else {
                activity.returnResult(pocketQuery)
            }
        })

        final AbstractListAdapter.TouchListener touchListener = AbstractListAdapter.TouchListener(pocketQuery, this)
        holder.binding.checkbox.setOnClickListener(touchListener)
        holder.itemView.setOnTouchListener(touchListener)
        holder.itemView.setOnClickListener(touchListener)

        holder.itemView.setOnLongClickListener(v -> {
            selectMode = !selectMode
            val selectedList: GCList = activity.getQueries().get(holder.getBindingAdapterPosition())
            updateSelectedList(selectedList)
            return true
        })

    }

    public Unit updateView() {
        if (!selectMode) {
            selectedLists.clear()
        }

        activity.findViewById(R.id.switchAB).setVisibility(!selectMode ? View.VISIBLE : View.GONE)

        val buttonPreviewSelected: View = activity.findViewById(R.id.cachelist_selected)
        buttonPreviewSelected.setVisibility(selectMode && activity.supportMultiPreview() ? View.VISIBLE : View.GONE)
        buttonPreviewSelected.setEnabled(!selectedLists.isEmpty())

        // enable download button only, all selected lists are downloadable
        val buttonDownloadSelected: View = activity.findViewById(R.id.download_selected)
        val enableDownload: Boolean = !selectedLists.isEmpty() && selectedLists.stream().allMatch(GCList::isDownloadable)
        buttonDownloadSelected.setVisibility(selectMode ? View.VISIBLE : View.GONE)
        buttonDownloadSelected.setAlpha(enableDownload ? 1.0f : 0.4f)
        buttonDownloadSelected.setEnabled(enableDownload)

        notifyDataSetChanged()
    }

    private Unit updateSelectedList(final GCList selectedList) {
        if (selectMode) {
            val isSelected: Boolean = selectedLists.contains(selectedList)
            if (isSelected) {
                selectedLists.remove(selectedList)
            } else {
                selectedLists.add(selectedList)
            }
        }
        updateView()
    }
}
