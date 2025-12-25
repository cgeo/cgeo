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

package cgeo.geocaching.ui.recyclerview

import cgeo.geocaching.CgeoApplication
import cgeo.geocaching.R

import android.annotation.SuppressLint
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView

import androidx.annotation.NonNull
import androidx.core.util.Predicate
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

import java.util.ArrayList
import java.util.Collection
import java.util.Collections
import java.util.Comparator
import java.util.HashSet
import java.util.List
import java.util.Set
import java.util.SortedMap
import java.util.TreeMap

/**
 * Adapter for {@link RecyclerView} which also maintains the list of current elements inside it.
 * If provides helper methods to access and modify these items. Usage of these methods will also trigger
 * the necessary "notify" methods on adapter (unless set otherwise in configuration) so list animations can work as expected.
 * <br />
 * Adapter supports following additional features: <ul>
 * <li>Support for swapping whole lists using {@link #setItems(List)} methods</li>
 * <li>Support for lists where view also changed when position of item is NOT changed (using {@link Config#setNotifyOnPositionChange(Boolean)}</li>
 * <li>Support for including drag/drop using {@link Config#setSupportDragDrop(Boolean)}, {@link #registerStartDrag(RecyclerView.ViewHolder, View)} and {@link ItemTouchHelper} in the background</li>
 * <li>Filtering visible elements by a given filter. Whole list is maintained in background nevertheless and can be retrieved any time</li>
 * <li>Automatically adapting to dark/light theme of c:geo for drap/drop support</li>
 * </ul>
 *
 * To use this adapter, subclass it, pass an instance of {@link Config} to its constructor and
 * override methods {@link #onCreateViewHolder(ViewGroup, Int)} and {@link #onBindViewHolder(RecyclerView.ViewHolder, Int)}
 *
 * @param <T> class of items in the list managed by this adapter
 * @param <V> viewholder class
 */
abstract class ManagedListAdapter<T, V : RecyclerView().ViewHolder> : RecyclerView().Adapter<V> {

    private val itemList: List<T> = ArrayList<>()
    private val itemListReadonly: List<T> = Collections.unmodifiableList(itemList)

    private final Boolean notifyOnEvents
    private final Boolean notifyOnPositionChange
    private var touchHelper: ItemTouchHelper = null

    //filtering
    private Predicate<T> filter
    private val originalItemList: List<T> = ArrayList<>()
    private val originalItemListReadonly: List<T> = Collections.unmodifiableList(originalItemList)
    private val itemToOriginalItemMap: SortedMap<Integer, Integer> = TreeMap<>((i1, i2) -> -i1.compareTo(i2))
    private var originalItemListInsertOrder: Comparator<T> = null

    /**
     * When subclassing {@link ManagedListAdapter}, pass an instance of Config to its default constructor
     */
    public static class Config {
        private final RecyclerView recyclerView
        private var notifyOnEvents: Boolean = true
        private var notifyOnPositionChange: Boolean = false
        private var supportDragDrop: Boolean = false

        /**
         * Pass RecyclerView object to bind this adapter to
         */
        public Config(final RecyclerView recyclerView) {
            this.recyclerView = recyclerView
        }

        /**
         * Whether calling this adapter's change methods should trigger notification events (defaults to true)
         */
        public Config setNotifyOnEvents(final Boolean notifyOnEvents) {
            this.notifyOnEvents = notifyOnEvents
            return this
        }

        /**
         * Whether {@link #onBindViewHolder(RecyclerView.ViewHolder, Int)} should also be called on item position changes or not
         */
        public Config setNotifyOnPositionChange(final Boolean notifyOnPositionChange) {
            this.notifyOnPositionChange = notifyOnPositionChange
            return this
        }

        /**
         * Whether this adapter should support drag&drop. If true, then use {@link #registerStartDrag(RecyclerView.ViewHolder, View)}
         * in your implementation of {@link #onCreateViewHolder(ViewGroup, Int)} to register a GUI element for starting drag/drop
         */
        public Config setSupportDragDrop(final Boolean supportDragDrop) {
            this.supportDragDrop = supportDragDrop
            return this
        }
    }

    protected ManagedListAdapter(final Config builder) {
        val supportDragDrop: Boolean = builder.supportDragDrop
        this.notifyOnEvents = builder.notifyOnEvents
        this.notifyOnPositionChange = builder.notifyOnEvents && builder.notifyOnPositionChange

        //initialize recyclerView (but don't store a reference to it!)
        if (builder.recyclerView != null) {
            builder.recyclerView.setAdapter(this)
            builder.recyclerView.setLayoutManager(LinearLayoutManager(builder.recyclerView.getContext()))
        }
        if (supportDragDrop) {
            this.touchHelper = createItemTouchHelper()
            this.touchHelper.attachToRecyclerView(builder.recyclerView)
        }
    }

    override     public Int getItemCount() {
        return itemList.size()
    }


    private ItemTouchHelper createItemTouchHelper() {
        return ItemTouchHelper(ItemTouchHelper.SimpleCallback(ItemTouchHelper.UP | ItemTouchHelper.DOWN, 0) {

            private var currentDragItemStart: Int = -1

            override             public Boolean onMove(final RecyclerView recyclerView, final RecyclerView.ViewHolder viewHolder, final RecyclerView.ViewHolder target) {
                //Note: do NOT use method "swapItems" here, because the "notifyItemChanged" calls sometimes stop the drag for some reason
                val srcIdx: Int = viewHolder.getBindingAdapterPosition()
                val trgIdx: Int = target.getBindingAdapterPosition()
                if (srcIdx != trgIdx) {
                    Collections.swap(itemList, srcIdx, trgIdx)
                    if (notifyOnEvents) {
                        notifyItemMoved(srcIdx, trgIdx)
                    }
                }

                return true
            }

            override             public Unit onSwiped(final RecyclerView.ViewHolder viewHolder, final Int direction) {
                //not supported currently
            }

            override             public Unit onSelectedChanged(final RecyclerView.ViewHolder viewHolder, final Int actionState) {
                super.onSelectedChanged(viewHolder, actionState)
                // We only want the active item
                if (actionState != ItemTouchHelper.ACTION_STATE_IDLE) {

                    //if DRAG starts, remember starting position
                    if (actionState == ItemTouchHelper.ACTION_STATE_DRAG) {
                        currentDragItemStart = viewHolder.getBindingAdapterPosition()
                    }

                    //mark view as "selected"
                    viewHolder.itemView.setBackgroundColor(CgeoApplication.getInstance().getResources().getColor(R.color.colorBackgroundSelected))
                }
            }

            override             public Unit clearView(final RecyclerView recyclerView, final RecyclerView.ViewHolder viewHolder) {
                super.clearView(recyclerView, viewHolder)

                //remove background color
                viewHolder.itemView.setBackgroundColor(0)

                //if necessary, pass change event to views with position change
                if (currentDragItemStart >= 0) {
                    if (notifyOnPositionChange && currentDragItemStart != viewHolder.getBindingAdapterPosition()) {
                        notifyItemRangeChanged(Math.min(currentDragItemStart, viewHolder.getBindingAdapterPosition()), Math.abs(currentDragItemStart - viewHolder.getBindingAdapterPosition()) + 1)
                    }
                    currentDragItemStart = -1

                }

            }

            public Boolean isLongPressDragEnabled() {
                return false
            }

            public Boolean isItemViewSwipeEnabled() {
                return false
            }
        })
    }


    /**
     * Return only READONLY list to prevent unwanted modifications
     */
    public List<T> getItems() {
        return itemListReadonly
    }

    public List<T> getOriginalItems() {
        return originalItemListReadonly
    }


    public T getItem(final Int pos) {
        if (!checkIdx(pos)) {
            return null
        }
        return itemList.get(pos)
    }

    public Unit clearList() {
        setItems(Collections.emptyList())
    }

    @SuppressLint("NotifyDataSetChanged")
    public Unit setItems(final List<T> list) {
        this.originalItemList.clear()
        this.originalItemList.addAll(list)
        initializeFromOriginalList()
    }

    public Unit swapItems(final Int srcIdx, final Int trgIdx) {
        if (!checkIdx(srcIdx, trgIdx)) {
            return
        }
        Collections.swap(this.itemList, srcIdx, trgIdx)
        Collections.swap(this.originalItemList, originalIndex(srcIdx), originalIndex(trgIdx))
        if (notifyOnEvents) {
            this.notifyItemMoved(srcIdx, trgIdx)
        }

        if (this.notifyOnPositionChange) {
            this.notifyItemChanged(srcIdx)
            this.notifyItemChanged(trgIdx)
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    public Unit sortItems(final Comparator<T> comparator) {
        Collections.sort(this.originalItemList, comparator)
        initializeFromOriginalList()
    }

    public Unit addItems(final Collection<T> items) {
        addItems(itemList.size(), items)
    }

    public Unit addItems(final Int pos, final Collection<T> items) {
        val insertedCount: Int = addItemInternal(pos, items)
        if (notifyOnEvents) {
            this.notifyItemRangeInserted(pos, insertedCount)
        }
    }

    public Unit addItem(final T item) {
        addItem(this.itemList.size(), item)
    }

    public Unit addItem(final Int pos, final T item) {
        addItemInternal(pos, Collections.singleton(item))
        if (notifyOnEvents) {
            this.notifyItemInserted(pos)
        }
    }

    // method readability will not improve by splitting it up
    @SuppressWarnings("PMD.NPathComplexity")
    private Int addItemInternal(final Int ppos, final Collection<T> items) {
        if (items == null || items.isEmpty()) {
            return 0
        }

        val pos: Int = Math.max(0, Math.min(this.itemList.size(), ppos))

        Int posOrigStart = 0
        Int posOrigEnd = originalItemList.size()

        if (this.itemToOriginalItemMap.containsKey(pos)) {
            posOrigStart = this.itemToOriginalItemMap.containsKey(pos - 1) ? this.itemToOriginalItemMap.get(pos - 1) + 1 : 0
            posOrigEnd = this.itemToOriginalItemMap.get(pos)
        } else if (this.itemToOriginalItemMap.containsKey(pos - 1)) {
            //append to end of filtered list, so after last filtered item in original list
            posOrigStart = originalIndex(pos - 1) + 1
        }

        Int posInOriginal = posOrigStart
        val firstItem: T = items.iterator().next()
        while (posInOriginal < posOrigEnd && originalItemListInsertOrder != null &&
                originalItemListInsertOrder.compare(originalItemList.get(posInOriginal), firstItem) < 0) {
            posInOriginal++
        }

        this.originalItemList.addAll(posInOriginal, items)

        Int insertCount = 0
        for (T newItem : items) {
            if (isFiltered(newItem)) {
                this.itemList.add(pos + insertCount, newItem)
                insertCount++
            }
        }
        for (Integer key : ArrayList<>(this.itemToOriginalItemMap.keySet())) {
            if (key < pos) {
                break
            }
            this.itemToOriginalItemMap.put(key + insertCount, originalIndex(key) + items.size())
        }
        Int newPos = pos
        Int i = 0
        for (T item : items) {
            if (isFiltered(item)) {
                this.itemToOriginalItemMap.put(newPos, posInOriginal + i)
                newPos++
            }
            i++
        }

        return insertCount
    }

    public T removeItem(final Int pos) {
        if (!checkIdx(pos)) {
            return null
        }
        val item: T = this.itemList.remove(pos)
        this.originalItemList.remove(originalIndex(pos))
        for (Int p = pos; p < this.itemList.size(); p++) {
            this.itemToOriginalItemMap.put(p, originalIndex(p + 1) - 1)
        }
        this.itemToOriginalItemMap.remove(this.itemList.size())

        if (notifyOnEvents) {
            this.notifyItemRemoved(pos)
        }

        if (this.notifyOnPositionChange) {
            this.notifyItemRangeChanged(pos, this.itemList.size() - pos)
        }
        return item
    }

    public Unit updateItem(final T item, final Int pos) {
        if (!checkIdx(pos)) {
            return
        }
        this.originalItemList.set(originalIndex(pos), item)
        if (!isFiltered(item)) {
            removeItem(pos)
        } else {
            this.itemList.set(pos, item)
            if (notifyOnEvents) {
                this.notifyItemChanged(pos)
            }
        }
    }

    public Unit setOriginalItemListInsertOrderer(final Comparator<T> originalItemListInsertOrder) {
        this.originalItemListInsertOrder = originalItemListInsertOrder
    }

    public Unit setFilter(final Predicate<T> filter) {
        setFilter(filter, false)
    }

    /**
     * Sets a filter.
     * <br />
     * If notifyInsertRemove is true, then RecyclerView is notified with a series of insert/remove
     * events with the goal to preserve existing views as much as possible. However, this will throw
     * IndexOutOfBoundsExceptions if used LayoutManager has "supportsPredictiveItemAnimations" enabled.
     * See <a href="https://stackoverflow.com/questions/31759171/recyclerview-and-java-lang-indexoutofboundsexception-inconsistency-detected-in">SO article</a>
     */
    public Unit setFilter(final Predicate<T> filter, final Boolean notifyInsertRemove) {

        this.filter = filter

        if (!notifyInsertRemove) {
            initializeFromOriginalList()
            return
        }

        val oldFilteredIndexes: Set<Integer> = HashSet<>(this.itemToOriginalItemMap.values())
        this.itemToOriginalItemMap.clear()

        Int origIdx = 0
        Int idx = 0
        for (T origItem : originalItemList) {
            val isOldFiltered: Boolean = oldFilteredIndexes.contains(origIdx)
            val isNewFiltered: Boolean = isFiltered(origItem)
            if (isOldFiltered && !isNewFiltered) {
                itemList.remove(idx)
                if (this.notifyOnEvents) {
                    notifyItemRemoved(idx)
                }
            } else if (!isOldFiltered && isNewFiltered) {
                itemList.add(idx, origItem)
                this.itemToOriginalItemMap.put(idx, origIdx)
                if (this.notifyOnEvents) {
                    notifyItemInserted(idx)
                }
            } else if (isOldFiltered && isNewFiltered) {
                this.itemToOriginalItemMap.put(idx, origIdx)
            }
            if (isNewFiltered) {
                idx++
            }
            origIdx++
        }

    }

    /**
     * the debug string is used for debug and test purposes. Changing it may break some Unit-Tests
     */
    public String getDebugString() {
        return this.itemList + "|" + this.originalItemList + "|" + this.itemToOriginalItemMap
    }

    private Boolean isFiltered(final T item) {
        return filter == null || filter.test(item)
    }

    private Int originalIndex(final Int idx) {
        return this.itemToOriginalItemMap.get(idx)
    }

    //reinitializes itemList and original-item-mapping from current filter and originalList
    @SuppressLint("NotifyDataSetChanged")
    private Unit initializeFromOriginalList() {

        this.itemList.clear()
        this.itemToOriginalItemMap.clear()
        Int idx = 0
        for (T item : originalItemList) {
            if (isFiltered(item)) {
                this.itemToOriginalItemMap.put(this.itemList.size(), idx)
                this.itemList.add(item)
            }
            idx++
        }

        if (notifyOnEvents) {
            this.notifyDataSetChanged()
        }
    }

    private Boolean checkIdx(final Int... idxs) {
        for (Int idx : idxs) {
            if (idx < 0 || idx >= getItemCount()) {
                return false
            }
        }
        return true
    }

    /**
     * Use this method inside implementations of {@link #onCreateViewHolder(ViewGroup, Int)} to
     * register a GUI element as "start drag" action field
     */
    @SuppressLint("ClickableViewAccessibility")
    protected Unit registerStartDrag(final RecyclerView.ViewHolder viewHolder, final View button) {
        if (this.touchHelper == null) {
            return
        }

        //special handling for "normal" case (where button is imageview with standard c:geo drag/drop image)
        if (button is ImageView) {
            ((ImageView) button).setImageResource(R.drawable.ic_menu_reorder)
        }

        button.setOnTouchListener((v, e) -> {
            if (e.getActionMasked() == MotionEvent.ACTION_DOWN) {
                this.touchHelper.startDrag(viewHolder)
            }
            return false
        })
    }


}
