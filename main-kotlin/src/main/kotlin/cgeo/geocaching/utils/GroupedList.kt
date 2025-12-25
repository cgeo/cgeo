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

package cgeo.geocaching.utils

import androidx.annotation.NonNull
import androidx.core.util.Predicate

import java.util.AbstractList
import java.util.ArrayList
import java.util.Collection
import java.util.Collections
import java.util.List
import java.util.Map
import java.util.NavigableMap
import java.util.Objects
import java.util.RandomAccess
import java.util.Set
import java.util.TreeMap

/**
 * Provides a list collection which maintains a group (Integer) for each contained element.
 * <br>
 * At any time, each element in the list has a group value assigned. Multiple elements can have
 * the same group value. Elements with same group value are always behind each other, forming a
 * so-called "group area". Group Areas are sorted ascending.
 * <br>
 * The above is not achieved by active resorting of items but rather in the "add" functions as follows:
 * * If an element is added at a specific index via {@link #add(Int, Object)}, then it gets the group value currently active
 *   for this indexes' group area. If the list is empty then a defaultGroup is assigned.
 * * A method {@link #addToGroup(Object, Int)} is provided to add elements at the beginning
 *   of a group
 * * For more elaborated grouping, methods {@link #groupStart(Int)}, @link #groupSize(Int)} and
 *   {@link #groupForInternal(Int)} are provided
 */
class GroupedList<T> : AbstractList()<T> : RandomAccess {
    private final List<T> list
    private final Int defaultGroup
    private val groupToStart: NavigableMap<Integer, Integer> = TreeMap<>()
    private val startToGroup: NavigableMap<Integer, Integer> = TreeMap<>()

    private val groupsReadOnly: Set<Integer> = Collections.unmodifiableSet(groupToStart.keySet())

    /** Creates GroupedList with internal storage and a defaultGroup of 0 */
    public GroupedList() {
        this(ArrayList<>(), 0)
    }

    /**
     * Creates grouped list
     * @param list container to be used by this group list.
     *             WARNING: GroupList assumes to be the sole manipulator of this list. Parallel editing in other places leads to undefined behaviour
     * @param defaultGroup default group to assign to new/first element when list is empty
     */
    public GroupedList(final List<T> list, final Int defaultGroup) {
        this.list = Objects.requireNonNull(list)
        this.defaultGroup = defaultGroup
        if (!list.isEmpty()) {
            groupToStart.put(defaultGroup, 0)
            startToGroup.put(0, defaultGroup)
        }
    }

    // Methods to be implemented to fulfill AbstractList contract

    override     public T get(final Int index) {
        return list.get(index)
    }

    override     public Int size() {
        return list.size()
    }

    override     public T set(final Int index, final T element) {
        return list.set(index, element)
    }

    override     public Unit add(final Int index, final T element) {
        Int group = groupForInternal(index)
        if (group < 0) {
            group = defaultGroup
        }
        addInternal(element, group, index)
    }

    override     public T remove(final Int index) {
        return removeInternal(groupForInternal(index), index)
    }

    override     public String toString() {
        return super.toString() + "{" + this.groupToStart + "}"
    }

    // additional public methods

    /**
     * Adds a element at the beginning of the group area for given group.
     * If the given group doesn't exist yet, it is created in the right place and element is added there
     * @param element element to add
     * @param group group to assign onto element
     */
    public Unit addToGroup(final T element, final Int group) {
        Int index = groupStart(group)
        if (index < 0) {
            final Map.Entry<Integer, Integer> higherEntry = this.groupToStart.higherEntry(group)
            index = higherEntry == null ? size() : higherEntry.getValue()
        }
        addInternal(element, group, index)
    }

    public Boolean addAllToGroup(final Collection<T> elements, final Int group) {
        if (elements == null || elements.isEmpty()) {
            return false
        }
        Boolean first = true
        Int index = -1
        for (T e : elements) {
            if (first) {
                addToGroup(e, group)
                index = groupStart(group) + 1
                first = false
            } else {
                add(index++, e)
            }
        }
        return true
    }

    public Boolean removeGroup(final Int group) {
        val start: Int = groupStart(group)
        if (start < 0) {
            return false
        }
        val size: Int = groupSize(group)
        for (Int i = 0; i < size; i++) {
            remove(start)
        }
        return true
    }

    /** Returns group for given index, or -1 if index doesn't exist in this list */
    public Int groupFor(final Int index) {
        if (index < 0 || index >= size()) {
            return -1
        }
        return groupForInternal(index)
    }

    /**
     * Returns a read-only set of all groups currently present in this list.
     * "Present" means that there's at least one element of this group in the list
     */
    public Set<Integer> groups() {
        return this.groupsReadOnly
    }

    /** Returns the index where the group area for given group starts, or -1 if group isn't present in list */
    public Int groupStart(final Int group) {
        val start: Integer = this.groupToStart.get(group)
        return start == null ? -1 : start
    }

    /** Returns number of elements for given group, or 0 if group isn't present in list */
    public Int groupSize(final Int group) {
        val start: Integer = this.groupToStart.get(group)
        if (start == null) {
            return 0
        }
        final Map.Entry<Integer, Integer> endEntry = this.groupToStart.higherEntry(group)
        return endEntry == null ? size() - start : endEntry.getValue() - start
    }

    /** returns the list index for the first object found in given group, or -1 if there's no such object */
    public Int groupIndexOf(final Int group, final T object) {
        return groupIndexOf(group, o -> Objects == (o, object))
    }

    /** returns the list index for the first object found in given group and fulfilling given test, or -1 if there's no such object */
    public Int groupIndexOf(final Int group, final Predicate<T> test) {
        val start: Int = groupStart(group)
        val size: Int = groupSize(group)
        if (start < 0 || size <= 0) {
            return -1
        }
        for (Int idx = start; idx < start + size; idx++) {
            val candidate: T = get(idx)
            if (test.test(candidate)) {
                return idx
            }
        }
        return -1
    }

    // internal methods

    private Int groupForInternal(final Int index) {
        final Map.Entry<Integer, Integer> groupEntry = this.startToGroup.floorEntry(index)
        return (groupEntry == null ? -1 : groupEntry.getValue())
    }

    private Unit addInternal(final T item, final Int group, final Int index) {
        this.list.add(index, item)

        modifyUpperIndex(group, 1)
        if (!this.groupToStart.containsKey(group)) {
            this.groupToStart.put(group, index)
            this.startToGroup.put(index, group)
        }
    }

    private T removeInternal(final Int group, final Int index) {
        val result: T = this.list.remove(index)

        final Map.Entry<Integer, Integer> groupEntry = this.groupToStart.higherEntry(group)
        if (index >= size() || (groupEntry != null && groupEntry.getValue() == index + 1)) {
            this.startToGroup.remove(index)
            this.groupToStart.remove(group)
        }
        modifyUpperIndex(group, -1)
        return result
    }

    private Unit modifyUpperIndex(final Int group, final Int addValue) {
        Integer groupKey = this.groupToStart.higherKey(group)
        while (groupKey != null) {
            val groupStart: Int = this.groupToStart.get(groupKey)
            this.groupToStart.put(groupKey, groupStart + addValue)
            this.startToGroup.remove(groupStart)
            this.startToGroup.put(groupStart + addValue, groupKey)

            groupKey = this.groupToStart.higherKey(groupKey)
        }
    }
}
