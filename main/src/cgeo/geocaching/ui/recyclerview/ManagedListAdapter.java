package cgeo.geocaching.ui.recyclerview;

import cgeo.geocaching.CgeoApplication;
import cgeo.geocaching.R;
import cgeo.geocaching.settings.Settings;

import android.graphics.Color;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Adapter for {@link RecyclerView} which also maintains the list of current elements inside it.
 * If provides helper methods to access and modify these items. Usage of these methods will also trigger
 * the necessary "notify" methods on adapter so list animations can work as expected.
 *
 * Adapter supports following additional features: <ul>
 * <li>Support for swapping whole lists using {@link #setItems(List)} methods</li>
 * <li>Support for lists where view also changed when position of item is NOT changed (using {@link Config#setNotifyOnPositionChange(boolean)}</li>
 * <li>Support for including drag/drop using {@link Config#setSupportDragDrop(boolean)}, {@link #registerStartDrag(RecyclerView.ViewHolder, View)} and {@link ItemTouchHelper} in the background</li>
 * <li>AUtomatically adapting to dark/light theme of c:geo for drap/drop support</li>
 * </ul>
 *
 * To use this adapter, subclass it, pass an instance of {@link Config} to its constructor and
 * override methods {@link #onCreateViewHolder(ViewGroup, int)} and {@link #onBindViewHolder(RecyclerView.ViewHolder, int)}
 *
 * @param <T> class of items in the list managed by this adapter
 * @param <V> viewholder class
 */
public abstract class ManagedListAdapter<T, V extends RecyclerView.ViewHolder> extends RecyclerView.Adapter<V> {

    private final List<T> itemList = new ArrayList<>();
    private final List<T> itemListReadonly = Collections.unmodifiableList(itemList);

    private final boolean notifyOnPositionChange;
    private ItemTouchHelper touchHelper = null;

    /**
     * When subclassing {@link ManagedListAdapter}, pass an instance of Config to its default constructor
     */
    public static class Config {
        private final RecyclerView recyclerView;
        private boolean notifyOnPositionChange = false;
        private boolean supportDragDrop = false;

        /**
         * Pass RecyclerView object to bind this adapter to
         */
        public Config(final RecyclerView recyclerView) {
            this.recyclerView = recyclerView;
        }

        /**
         * Whether {@link #onBindViewHolder(RecyclerView.ViewHolder, int)} should also be called on item position changes or not
         */
        public Config setNotifyOnPositionChange(final boolean notifyOnPositionChange) {
            this.notifyOnPositionChange = notifyOnPositionChange;
            return this;
        }

        /**
         * Whether this adapter should support drag&drop. If true, then use {@link #registerStartDrag(RecyclerView.ViewHolder, View)}
         * in your implementation of {@link #onCreateViewHolder(ViewGroup, int)} to register a GUI element for starting drag/drop
         */
        public Config setSupportDragDrop(final boolean supportDragDrop) {
            this.supportDragDrop = supportDragDrop;
            return this;
        }
    }

    protected ManagedListAdapter(final Config builder) {
       final boolean supportDragDrop = builder.supportDragDrop;
        this.notifyOnPositionChange = builder.notifyOnPositionChange;

        //initialize adapter
        setItems(itemList);

        //initialize recyclerView (but don't store a reference to it!)
        builder.recyclerView.setAdapter(this);
        builder.recyclerView.setLayoutManager(new LinearLayoutManager(builder.recyclerView.getContext()));
        if (supportDragDrop) {
            this.touchHelper = createItemTouchHelper();
            this.touchHelper.attachToRecyclerView(builder.recyclerView);
        }
    }

    @Override
    public int getItemCount() {
        return itemList.size();
    }


    private ItemTouchHelper createItemTouchHelper() {
        return new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(ItemTouchHelper.UP | ItemTouchHelper.DOWN, 0) {

            private int currentDragItemStart = -1;

            @Override
            public boolean onMove(@NonNull final RecyclerView recyclerView, @NonNull final RecyclerView.ViewHolder viewHolder, @NonNull final RecyclerView.ViewHolder target) {
                //Note: do NOT use method "swapItems" here, because the "notifyItemChanged" calls sometimes stop the drag for some reason
                final int srcIdx = viewHolder.getBindingAdapterPosition();
                final int trgIdx = target.getBindingAdapterPosition();
                if (srcIdx != trgIdx) {
                    Collections.swap(itemList, srcIdx, trgIdx);
                    notifyItemMoved(srcIdx, trgIdx);
                }

                return true;
            }

            @Override
            public void onSwiped(@NonNull final RecyclerView.ViewHolder viewHolder, final int direction) {
                //not supported currently
            }

            @Override
            public void onSelectedChanged(final RecyclerView.ViewHolder viewHolder, final int actionState) {
                super.onSelectedChanged(viewHolder, actionState);
                // We only want the active item
                if (actionState != ItemTouchHelper.ACTION_STATE_IDLE) {

                    //if DRAG starts, remember starting position
                    if (actionState == ItemTouchHelper.ACTION_STATE_DRAG) {
                        currentDragItemStart = viewHolder.getBindingAdapterPosition();
                    }

                     //mark view as "selected"
                    viewHolder.itemView.setBackgroundColor(CgeoApplication.getInstance().getResources().getColor(R.color.colorBackgroundSelected));
                }
            }
            @Override
            public void clearView(@NonNull final RecyclerView recyclerView, @NonNull final RecyclerView.ViewHolder viewHolder) {
                super.clearView(recyclerView, viewHolder);

                //remove background color
                viewHolder.itemView.setBackgroundColor(0);

                //if necessary, pass change event to views with position change
                if (currentDragItemStart >= 0) {
                    if (notifyOnPositionChange && currentDragItemStart != viewHolder.getBindingAdapterPosition()) {
                        notifyItemRangeChanged(Math.min(currentDragItemStart, viewHolder.getBindingAdapterPosition()), Math.abs(currentDragItemStart - viewHolder.getAdapterPosition()) + 1);
                    }
                    currentDragItemStart = -1;

                }

            }

            public boolean isLongPressDragEnabled() {
                return false;
            }

            public boolean isItemViewSwipeEnabled() {
                return false;
            }
        });
    }

    /**
     * Return only READONLY list to prevent unwanted modifications
     */
    @NonNull
    public List<T> getCurrentList() {
        return itemListReadonly;
    }

    public List<T> getItems() {
        return getCurrentList();
    }

    public T getItem(final int pos) {
        if (!checkIdx(pos)) {
            return null;
        }
        return itemList.get(pos);
    }

    public void clearList() {
        setItems(Collections.emptyList());
    }

    public void setItems(final List<T> list) {
        final int oldSize = this.itemList.size();
        this.itemList.clear();
        this.itemList.addAll(list);
        //tell the recycler view that all the old items are gone
        notifyItemRangeRemoved(0, oldSize);
        //tell the recycler view how many new items we added
        notifyItemRangeInserted(0, this.itemList.size());
        //this.notifyItemRangeChanged(0, this.itemList.size());
    }

    public void swapItems(final int srcIdx, final int trgIdx) {
        if (!checkIdx(srcIdx, trgIdx)) {
            return;
        }
        Collections.swap(this.itemList, srcIdx, trgIdx);
        this.notifyItemMoved(srcIdx, trgIdx);

        if (this.notifyOnPositionChange) {
            this.notifyItemChanged(srcIdx);
            this.notifyItemChanged(trgIdx);
        }
    }

    public void addItems(final Collection<T> items) {
        this.itemList.addAll(items);
        this.notifyItemRangeInserted(this.itemList.size() - items.size(), items.size());
    }

    public void addItem(final T item) {
        addItem(this.itemList.size(), item);
    }

    public void addItem(final int pos, final T item) {
        this.itemList.add(pos, item);
        this.notifyItemInserted(pos);
    }

    public T removeItem(final int pos) {
        if (!checkIdx(pos)) {
            return null;
        }
        final T item = this.itemList.remove(pos);
        this.notifyItemRemoved(pos);

        if (this.notifyOnPositionChange) {
            this.notifyItemRangeChanged(pos, this.itemList.size() - pos);
        }
        return item;
    }

    public void updateItem(final T item, final int pos) {
        if (!checkIdx(pos)) {
            return;
        }
        this.itemList.set(pos, item);
        this.notifyItemChanged(pos);
    }

    private boolean checkIdx(final int... idxs) {
        for (int idx : idxs) {
            if (idx < 0 || idx >= getItemCount()) {
                return false;
            }
        }
        return true;
    }

    /**
     * Use this method inside implementations of {@link #onCreateViewHolder(ViewGroup, int)} to
     * register a GUI element as "start drag" action field
     */
    protected void registerStartDrag(final RecyclerView.ViewHolder viewHolder, final View button) {
        if (this.touchHelper == null) {
            return;
        }

        //special handling for "normal" case (where button is imageview with standard c:geo drag/drop image)
        if (button instanceof ImageView) {
            ((ImageView) button).setImageResource(R.drawable.ic_menu_reorder);
        }

        button.setOnTouchListener((v, e) -> {
            if (e.getActionMasked() == MotionEvent.ACTION_DOWN) {
                this.touchHelper.startDrag(viewHolder);
            }
            return false;
        });
    }

}
