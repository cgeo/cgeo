package cgeo.geocaching.utils;

import androidx.annotation.NonNull;
import androidx.core.util.Predicate;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Objects;
import java.util.RandomAccess;
import java.util.Set;
import java.util.TreeMap;

/**
 * Provides a list collection which maintains a group (Integer) for each contained element.
 * <br>
 * At any time, each element in the list has a group value assigned. Multiple elements can have
 * the same group value. Elements with same group value are always behind each other, forming a
 * so-called "group area". Group Areas are sorted ascending.
 * <br>
 * The above is not achieved by active resorting of items but rather in the "add" functions as follows:
 * * If an element is added at a specific index via {@link #add(int, Object)}, then it gets the group value currently active
 *   for this indexes' group area. If the list is empty then a defaultGroup is assigned.
 * * A new  method {@link #addToGroup(Object, int)} is provided to add elements at the beginning
 *   of a group
 * * For more elaborated grouping, methods {@link #groupStart(int)}, @link #groupSize(int)} and
 *   {@link #groupForInternal(int)} are provided
 */
public class GroupedList<T> extends AbstractList<T> implements RandomAccess {
    private final List<T> list;
    private final int defaultGroup;
    private final NavigableMap<Integer, Integer> groupToStart = new TreeMap<>();
    private final NavigableMap<Integer, Integer> startToGroup = new TreeMap<>();

    private final Set<Integer> groupsReadOnly = Collections.unmodifiableSet(groupToStart.keySet());

    /** Creates GroupedList with internal storage and a defaultGroup of 0 */
    public GroupedList() {
        this(new ArrayList<>(), 0);
    }

    /**
     * Creates new grouped list
     * @param list container to be used by this group list.
     *             WARNING: GroupList assumes to be the sole manipulator of this list. Parallel editing in other places leads to undefined behaviour
     * @param defaultGroup default group to assign to new/first element when list is empty
     */
    public GroupedList(@NonNull final List<T> list, final int defaultGroup) {
        this.list = Objects.requireNonNull(list);
        this.defaultGroup = defaultGroup;
        if (!list.isEmpty()) {
            groupToStart.put(defaultGroup, 0);
            startToGroup.put(0, defaultGroup);
        }
    }

    // Methods to be implemented to fulfill AbstractList contract

    @Override
    public T get(final int index) {
        return list.get(index);
    }

    @Override
    public int size() {
        return list.size();
    }

    @Override
    public T set(final int index, final T element) {
        return list.set(index, element);
    }

    @Override
    public void add(final int index, final T element) {
        int group = groupForInternal(index);
        if (group < 0) {
            group = defaultGroup;
        }
        addInternal(element, group, index);
    }

    @Override
    public T remove(final int index) {
        return removeInternal(groupForInternal(index), index);
    }

    @NonNull
    @Override
    public String toString() {
        return super.toString() + "{" + this.groupToStart + "}";
    }

    // additional public methods

    /**
     * Adds a new element at the beginning of the group area for given group.
     * If the given group doesn't exist yet, it is created in the right place and element is added there
     * @param element element to add
     * @param group group to assign onto element
     */
    public void addToGroup(final T element, final int group) {
        int index = groupStart(group);
        if (index < 0) {
            final Map.Entry<Integer, Integer> higherEntry = this.groupToStart.higherEntry(group);
            index = higherEntry == null ? size() : higherEntry.getValue();
        }
        addInternal(element, group, index);
    }

    public boolean addAllToGroup(final Collection<T> elements, final int group) {
        if (elements == null || elements.isEmpty()) {
            return false;
        }
        boolean first = true;
        int index = -1;
        for (T e : elements) {
            if (first) {
                addToGroup(e, group);
                index = groupStart(group) + 1;
                first = false;
            } else {
                add(index++, e);
            }
        }
        return true;
    }

    public boolean removeGroup(final int group) {
        final int start = groupStart(group);
        if (start < 0) {
            return false;
        }
        final int size = groupSize(group);
        for (int i = 0; i < size; i++) {
            remove(start);
        }
        return true;
    }

    /** Returns group for given index, or -1 if index doesn't exist in this list */
    public int groupFor(final int index) {
        if (index < 0 || index >= size()) {
            return -1;
        }
        return groupForInternal(index);
    }

    /**
     * Returns a read-only set of all groups currently present in this list.
     * "Present" means that there's at least one element of this group in the list
     */
    public Set<Integer> groups() {
        return this.groupsReadOnly;
    }

    /** Returns the index where the group area for given group starts, or -1 if group isn't present in list */
    public int groupStart(final int group) {
        final Integer start = this.groupToStart.get(group);
        return start == null ? -1 : start;
    }

    /** Returns number of elements for given group, or 0 if group isn't present in list */
    public int groupSize(final int group) {
        final Integer start = this.groupToStart.get(group);
        if (start == null) {
            return 0;
        }
        final Map.Entry<Integer, Integer> endEntry = this.groupToStart.higherEntry(group);
        return endEntry == null ? size() - start : endEntry.getValue() - start;
    }

    /** returns the list index for the first object found in given group, or -1 if there's no such object */
    public int groupIndexOf(final int group, final T object) {
        return groupIndexOf(group, o -> Objects.equals(o, object));
    }

    /** returns the list index for the first object found in given group and fulfilling given test, or -1 if there's no such object */
    public int groupIndexOf(final int group, final Predicate<T> test) {
        final int start = groupStart(group);
        final int size = groupSize(group);
        if (start < 0 || size <= 0) {
            return -1;
        }
        for (int idx = start; idx < start + size; idx++) {
            final T candidate = get(idx);
            if (test.test(candidate)) {
                return idx;
            }
        }
        return -1;
    }

    // internal methods

    private int groupForInternal(final int index) {
        final Map.Entry<Integer, Integer> groupEntry = this.startToGroup.floorEntry(index);
        return (groupEntry == null ? -1 : groupEntry.getValue());
    }

    private void addInternal(final T item, final int group, final int index) {
        this.list.add(index, item);

        modifyUpperIndex(group, 1);
        if (!this.groupToStart.containsKey(group)) {
            this.groupToStart.put(group, index);
            this.startToGroup.put(index, group);
        }
    }

    private T removeInternal(final int group, final int index) {
        final T result = this.list.remove(index);

        final Map.Entry<Integer, Integer> groupEntry = this.groupToStart.higherEntry(group);
        if (index >= size() || (groupEntry != null && groupEntry.getValue() == index + 1)) {
            this.startToGroup.remove(index);
            this.groupToStart.remove(group);
        }
        modifyUpperIndex(group, -1);
        return result;
    }

    private void modifyUpperIndex(final int group, final int addValue) {
        Integer groupKey = this.groupToStart.higherKey(group);
        while (groupKey != null) {
            final int groupStart = this.groupToStart.get(groupKey);
            this.groupToStart.put(groupKey, groupStart + addValue);
            this.startToGroup.remove(groupStart);
            this.startToGroup.put(groupStart + addValue, groupKey);

            groupKey = this.groupToStart.higherKey(groupKey);
        }
    }
}
