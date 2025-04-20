package cgeo.geocaching.utils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Represents a group of items
 * <br>
 * This class is meant to be used to build a tree of groups containing other groups
 * */
public class ItemGroup<T, G> {

    private final G group;
    private ItemGroup<T, G> parent;

    private final List<Object> itemsAndGroups = new ArrayList<>();
    private final List<ItemGroup<T, G>> groups = new ArrayList<>();
    private final List<T> items = new ArrayList<>();

    private final List<Object> itemsAndGroupsReadOnly = Collections.unmodifiableList(itemsAndGroups);
    private final List<ItemGroup<T, G>> groupsReadOnly = Collections.unmodifiableList(groups);
    private final List<T> itemsReadOnly = Collections.unmodifiableList(items);

    private int level = -1;

    private ItemGroup(final G group) {
        this.group = group;
    }

    public G getGroup() {
        return group;
    }

    public ItemGroup<T, G> getParent() {
        return parent;
    }

    public List<ItemGroup<T, G>> getGroups() {
        return groupsReadOnly;
    }

    public List<T> getItems() {
        return itemsReadOnly;
    }

    public List<Object> getItemsAndGroups() {
        return itemsAndGroupsReadOnly;
    }

    public int getSize() {
        return itemsAndGroups.size();
    }

    public int getContainedItemCount() {
        int cnt = items.size();
        for (ItemGroup<T, G> child : getGroups()) {
            cnt += child.getContainedItemCount();
        }
        return cnt;
    }

    public int getLevel() {
        return level;
    }

    private void recalculateItemsAndGroups(final Comparator<Object> comp) {

        itemsAndGroups.clear();
        itemsAndGroups.addAll(items);
        itemsAndGroups.addAll(groups);
        if (comp != null) {
            Collections.sort(items, comp);
            Collections.sort(groups, comp);
            Collections.sort(itemsAndGroups, comp);
        }
    }

    @Override
    @NonNull
    public String toString() {
        return (group == null ? "<empty>" : group) + ":" + itemsAndGroups;
    }

    /**
     * Creates a new tree of grouped items and returns it
     *
     * @param items items to group
     * @param groupMapper function to map an item to a group
     * @param groupGroupMapper function to map groups to other groups (thus creating a tree)
     * @param itemOrder function to order groups and items within each other
     * @param pruner checker for created groups if the group should be pruned from the tree
     * @return root of grouped tree
     */
    public static <T, G> ItemGroup<T, G> create(final Iterable<T> items,
            @Nullable final Function<T, G> groupMapper,
            @Nullable final Function<G, G> groupGroupMapper,
            @Nullable final Comparator<Object> itemOrder,
            @Nullable final Predicate<ItemGroup<T, G>> pruner) {

        if (items == null) {
            return new ItemGroup<>(null);
        }

        //create base map
        final Map<G, ItemGroup<T, G>> groupMap = createBaseInfoMap(items, groupMapper, groupGroupMapper);
        for (Map.Entry<G, ItemGroup<T, G>> entry : groupMap.entrySet()) {
            entry.getValue().recalculateItemsAndGroups(itemOrder);
        }
        final ItemGroup<T, G> root = groupMap.get(null); // this will never be null

        //prune
        if (pruner != null) {
            pruneTree(root, itemOrder, pruner);
        }

        //level
        calculateLevel(root, 0);

        return root;
    }

    private static <T, G> Map<G, ItemGroup<T, G>> createBaseInfoMap(final Iterable<T> items,
           @Nullable final Function<T, G> groupMapper, @Nullable final Function<G, G> groupGroupMapper) {
        final Map<G, ItemGroup<T, G>> groupMap = new HashMap<>();
        groupMap.put(null, new ItemGroup<>(null));
        //shortcut
        if (items == null) {
            return groupMap;
        }
        for (T item : items) {
            final G group = groupMapper == null ? null : groupMapper.apply(item);
            if (!groupMap.containsKey(group)) {
                G currentGroup = group;
                ItemGroup<T, G> lastGroup = null;
                while (!groupMap.containsKey(currentGroup)) {
                    final ItemGroup<T, G> gi = new ItemGroup<>(currentGroup);
                    groupMap.put(currentGroup, gi);
                    if (lastGroup != null) {
                        gi.groups.add(lastGroup);
                        lastGroup.parent = gi;
                    }
                    lastGroup = gi;
                    currentGroup = groupGroupMapper == null ? null : groupGroupMapper.apply(currentGroup);
                }
                if (lastGroup != null) {
                    groupMap.get(currentGroup).groups.add(lastGroup);
                    lastGroup.parent = groupMap.get(currentGroup);
                }
            }
            groupMap.get(group).items.add(item);
        }
        return groupMap;
    }

    //recursive
    private static <T, G> void pruneTree(final ItemGroup<T, G> item, final Comparator<Object> comp, final Predicate<ItemGroup<T, G>> predicate) {
        //child-first
        for (ItemGroup<T, G> child : new LinkedList<>(item.groups)) {
            pruneTree(child, comp, predicate);
        }
        if (predicate != null && !predicate.test(item)) {
            prune(item, comp);
        }
    }

    private static <T, G> void prune(final ItemGroup<T, G> item, final Comparator<Object> comp) {
        final ItemGroup<T, G> parent = item.parent;
        if (parent == null) {
            return;
        }
        parent.items.addAll(item.items);
        for (ItemGroup<T, G> child : item.groups) {
            child.parent = parent;
        }
        parent.groups.remove(item);
        parent.groups.addAll(item.groups);
        parent.recalculateItemsAndGroups(comp);
    }

    //recursive
    private static <T, G> void calculateLevel(final ItemGroup<T, G> item, final int level) {
        item.level = level;
        for (ItemGroup<T, G> child : item.groups) {
            calculateLevel(child, level + 1);
        }
    }
}
