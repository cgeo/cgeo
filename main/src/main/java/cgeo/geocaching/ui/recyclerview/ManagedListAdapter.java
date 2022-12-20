package cgeo.geocaching.ui.recyclerview;

import cgeo.geocaching.CgeoApplication;
import cgeo.geocaching.R;

import android.annotation.SuppressLint;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.core.util.Predicate;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * Adapter for {@link RecyclerView} which also maintains the list of current elements inside it.
 * If provides helper methods to access and modify these items. Usage of these methods will also trigger
 * the necessary "notify" methods on adapter (unless set otherwise in configuration) so list animations can work as expected.
 *
 * Adapter supports following additional features: <ul>
 * <li>Support for swapping whole lists using {@link #setItems(List)} methods</li>
 * <li>Support for lists where view also changed when position of item is NOT changed (using {@link Config#setNotifyOnPositionChange(boolean)}</li>
 * <li>Support for including drag/drop using {@link Config#setSupportDragDrop(boolean)}, {@link #registerStartDrag(RecyclerView.ViewHolder, View)} and {@link ItemTouchHelper} in the background</li>
 * <li>Filtering visible elements by a given filter. Whole list is maintained in background nevertheless and can be retrieved any time</li>
 * <li>Automatically adapting to dark/light theme of c:geo for drap/drop support</li>
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

    private final boolean notifyOnEvents;
    private final boolean notifyOnPositionChange;
    private ItemTouchHelper touchHelper = null;

    //filtering
    private Predicate<T> filter;
    private final List<T> originalItemList = new ArrayList<>();
    private final List<T> originalItemListReadonly = Collections.unmodifiableList(originalItemList);
    private final SortedMap<Integer, Integer> itemToOriginalItemMap = new TreeMap<>((i1, i2) -> -i1.compareTo(i2));
    private Comparator<T> originalItemListInsertOrder = null;

    /**
     * When subclassing {@link ManagedListAdapter}, pass an instance of Config to its default constructor
     */
    public static class Config {
        private final RecyclerView recyclerView;
        private boolean notifyOnEvents = true;
        private boolean notifyOnPositionChange = false;
        private boolean supportDragDrop = false;

        /**
         * Pass RecyclerView object to bind this adapter to
         */
        public Config(final RecyclerView recyclerView) {
            this.recyclerView = recyclerView;
        }

        /**
         * Whether calling this adapter's change methods should trigger notification events (defaults to true)
         */
        public Config setNotifyOnEvents(final boolean notifyOnEvents) {
            this.notifyOnEvents = notifyOnEvents;
            return this;
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
        this.notifyOnEvents = builder.notifyOnEvents;
        this.notifyOnPositionChange = builder.notifyOnEvents && builder.notifyOnPositionChange;

        //initialize recyclerView (but don't store a reference to it!)
        if (builder.recyclerView != null) {
            builder.recyclerView.setAdapter(this);
            builder.recyclerView.setLayoutManager(new LinearLayoutManager(builder.recyclerView.getContext()));
        }
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
                    if (notifyOnEvents) {
                        notifyItemMoved(srcIdx, trgIdx);
                    }
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
                        notifyItemRangeChanged(Math.min(currentDragItemStart, viewHolder.getBindingAdapterPosition()), Math.abs(currentDragItemStart - viewHolder.getBindingAdapterPosition()) + 1);
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
    public List<T> getItems() {
        return itemListReadonly;
    }

    public List<T> getOriginalItems() {
        return originalItemListReadonly;
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

    @SuppressLint("NotifyDataSetChanged")
    public void setItems(final List<T> list) {
        this.originalItemList.clear();
        this.originalItemList.addAll(list);
        initializeFromOriginalList();
    }

    public void swapItems(final int srcIdx, final int trgIdx) {
        if (!checkIdx(srcIdx, trgIdx)) {
            return;
        }
        Collections.swap(this.itemList, srcIdx, trgIdx);
        Collections.swap(this.originalItemList, originalIndex(srcIdx), originalIndex(trgIdx));
        if (notifyOnEvents) {
            this.notifyItemMoved(srcIdx, trgIdx);
        }

        if (this.notifyOnPositionChange) {
            this.notifyItemChanged(srcIdx);
            this.notifyItemChanged(trgIdx);
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    public void sortItems(final Comparator<T> comparator) {
        Collections.sort(this.originalItemList, comparator);
        initializeFromOriginalList();
    }

    public void addItems(final Collection<T> items) {
        addItems(itemList.size(), items);
    }

    public void addItems(final int pos, final Collection<T> items) {
        final int insertedCount = addItemInternal(pos, items);
        if (notifyOnEvents) {
            this.notifyItemRangeInserted(pos, insertedCount);
        }
    }

    public void addItem(final T item) {
        addItem(this.itemList.size(), item);
    }

    public void addItem(final int pos, final T item) {
        addItemInternal(pos, Collections.singleton(item));
        if (notifyOnEvents) {
            this.notifyItemInserted(pos);
        }
    }

    // method readability will not improve by splitting it up
    @SuppressWarnings("PMD.NPathComplexity")
    private int addItemInternal(final int ppos, final Collection<T> items) {
        if (items == null || items.isEmpty()) {
            return 0;
        }

        final int pos = Math.max(0, Math.min(this.itemList.size(), ppos));

        int posOrigStart = 0;
        int posOrigEnd = originalItemList.size();

        if (this.itemToOriginalItemMap.containsKey(pos)) {
            posOrigStart = this.itemToOriginalItemMap.containsKey(pos - 1) ? this.itemToOriginalItemMap.get(pos - 1) + 1 : 0;
            posOrigEnd = this.itemToOriginalItemMap.get(pos);
        } else if (this.itemToOriginalItemMap.containsKey(pos - 1)) {
            //append to end of filtered list, so after last filtered item in original list
            posOrigStart = originalIndex(pos - 1) + 1;
        }

        int posInOriginal = posOrigStart;
        final T firstItem = items.iterator().next();
        while (posInOriginal < posOrigEnd && originalItemListInsertOrder != null &&
                originalItemListInsertOrder.compare(originalItemList.get(posInOriginal), firstItem) < 0) {
            posInOriginal++;
        }

        this.originalItemList.addAll(posInOriginal, items);

        int insertCount = 0;
        for (T newItem : items) {
            if (isFiltered(newItem)) {
                this.itemList.add(pos + insertCount, newItem);
                insertCount++;
            }
        }
        for (Integer key : new ArrayList<>(this.itemToOriginalItemMap.keySet())) {
            if (key < pos) {
                break;
            }
            this.itemToOriginalItemMap.put(key + insertCount, originalIndex(key) + items.size());
        }
        int newPos = pos;
        int i = 0;
        for (T item : items) {
            if (isFiltered(item)) {
                this.itemToOriginalItemMap.put(newPos, posInOriginal + i);
                newPos++;
            }
            i++;
        }

        return insertCount;
    }

    public T removeItem(final int pos) {
        if (!checkIdx(pos)) {
            return null;
        }
        final T item = this.itemList.remove(pos);
        this.originalItemList.remove(originalIndex(pos));
        for (int p = pos; p < this.itemList.size(); p++) {
            this.itemToOriginalItemMap.put(p, originalIndex(p + 1) - 1);
        }
        this.itemToOriginalItemMap.remove(this.itemList.size());

        if (notifyOnEvents) {
            this.notifyItemRemoved(pos);
        }

        if (this.notifyOnPositionChange) {
            this.notifyItemRangeChanged(pos, this.itemList.size() - pos);
        }
        return item;
    }

    public void updateItem(final T item, final int pos) {
        if (!checkIdx(pos)) {
            return;
        }
        this.originalItemList.set(originalIndex(pos), item);
        if (!isFiltered(item)) {
            removeItem(pos);
        } else {
            this.itemList.set(pos, item);
            if (notifyOnEvents) {
                this.notifyItemChanged(pos);
            }
        }
    }

    public void setOriginalItemListInsertOrderer(final Comparator<T> originalItemListInsertOrder) {
        this.originalItemListInsertOrder = originalItemListInsertOrder;
    }

    public void setFilter(final Predicate<T> filter) {
        setFilter(filter, false);
    }

    /**
     * Sets a new filter.
     *
     * If notifyInsertRemove is true, then RecyclerView is notified with a series of insert/remove
     * events with the goal to preserve existing views as much as possible. However, this will throw
     * IndexOutOfBoundsExceptions if used LayoutManager has "supportsPredictiveItemAnimations" enabled.
     * See https://stackoverflow.com/questions/31759171/recyclerview-and-java-lang-indexoutofboundsexception-inconsistency-detected-in
     */
    public void setFilter(final Predicate<T> filter, final boolean notifyInsertRemove) {

        this.filter = filter;

        if (!notifyInsertRemove) {
            initializeFromOriginalList();
            return;
        }

        final Set<Integer> oldFilteredIndexes = new HashSet<>(this.itemToOriginalItemMap.values());
        this.itemToOriginalItemMap.clear();

        int origIdx = 0;
        int idx = 0;
        for (T origItem : originalItemList) {
            final boolean isOldFiltered = oldFilteredIndexes.contains(origIdx);
            final boolean isNewFiltered = isFiltered(origItem);
            if (isOldFiltered && !isNewFiltered) {
                itemList.remove(idx);
                if (this.notifyOnEvents) {
                    notifyItemRemoved(idx);
                }
            } else if (!isOldFiltered && isNewFiltered) {
                itemList.add(idx, origItem);
                this.itemToOriginalItemMap.put(idx, origIdx);
                if (this.notifyOnEvents) {
                    notifyItemInserted(idx);
                }
            } else if (isOldFiltered && isNewFiltered) {
                this.itemToOriginalItemMap.put(idx, origIdx);
            }
            if (isNewFiltered) {
                idx++;
            }
            origIdx++;
        }

    }

    /**
     * the debug string is used for debug and test purposes. Changing it may break some Unit-Tests
     */
    @NonNull
    public String getDebugString() {
        return this.itemList + "|" + this.originalItemList + "|" + this.itemToOriginalItemMap;
    }

    private boolean isFiltered(final T item) {
        return filter == null || filter.test(item);
    }

    private int originalIndex(final int idx) {
        return this.itemToOriginalItemMap.get(idx);
    }

    //reinitializes itemList and original-item-mapping from current filter and originalList
    @SuppressLint("NotifyDataSetChanged")
    private void initializeFromOriginalList() {

        this.itemList.clear();
        this.itemToOriginalItemMap.clear();
        int idx = 0;
        for (T item : originalItemList) {
            if (isFiltered(item)) {
                this.itemToOriginalItemMap.put(this.itemList.size(), idx);
                this.itemList.add(item);
            }
            idx++;
        }

        if (notifyOnEvents) {
            this.notifyDataSetChanged();
        }
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
    @SuppressLint("ClickableViewAccessibility")
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
