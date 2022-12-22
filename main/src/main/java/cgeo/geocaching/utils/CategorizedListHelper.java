package cgeo.geocaching.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;

/**
 * A helper class / companion object to help maintaining a list which is divided into categories
 *
 * "Categorized lists" mean flat lists where however some entries are category headers and the
 * following entries are content entries for this category. For example consider following list:
 *
 * Colors, Blue, Red, Animals, Cat, Dog, Snake, Fruits, Apple, Orange
 *
 * In this list, the entries "Colors", "Animals" and "Fruits" can be considered "category headers"
 * whereas all other entries are considered "content" (belonging to the category they are placed under)
 *
 * For such lists, an instance of {@link CategorizedListHelper} helps with common tasks such as
 * * maintaining contained categories and their order
 * * tracks number of actual content entries
 * * maintains the correct list indexes for category headers
 * * maps from "list index" to "content index" and vice versa. For example the entry "Red" in above list
 *   would have a list index of 2 (being in 3rd list position) but a content index of "1" (being the 2nd
 *   "content entry" in the list)
 * */
public class CategorizedListHelper {

    private final List<String> categories = new ArrayList<>();
    private final Map<String, Integer> categoryCounts = new HashMap<>();
    private int contentCount = 0;

    // indexes for faster access performance
    private final Map<String, Integer> categoryIndexMap = new HashMap<>();
    private final NavigableMap<Integer, Integer> contentIndexListIndexMap = new TreeMap<>();

    public List<String> getCategories() {
        return categories;
    }

    public boolean containsCategory(final String cat) {
        return categoryCounts.containsKey(cat);
    }

    public int getCategoryCount(final String cat) {
        return categoryCounts.containsKey(cat) ? categoryCounts.get(cat) : 0;
    }

    public int getCategoryTitlePosition(final String cat) {
        if (!categoryCounts.containsKey(cat)) {
            return -1;
        }
        ensureCategoryIndexMap();
        return categoryIndexMap.get(cat);
    }

    public int getCategoryInsertPosition(final String cat) {
        final int startPos = getCategoryTitlePosition(cat);
        if (startPos < 0) {
            return -1;
        }
        return startPos + categoryCounts.get(cat) + 1;
    }

    public void addOrMoveCategory(final String cat, final boolean atStart, final int count) {
        addOrMoveCategory(cat, atStart);
        setCount(cat, count);
    }

    public void addOrMoveCategory(final String cat, final boolean atStart) {
        if (categoryCounts.containsKey(cat)) {
            categories.remove(cat);
        } else {
            categoryCounts.put(cat, 0);
        }
        categories.add(atStart ? 0 : categories.size(), cat);
        invalidateIndexes(cat);
    }

    public void removeCategory(final String cat) {
        if (!categoryCounts.containsKey(cat)) {
            return;
        }
        contentCount -= categoryCounts.get(cat);
        categories.remove(cat);
        categoryCounts.remove(cat);
        invalidateIndexes(null);
    }

    public void addToCount(final String cat, final int diff) {
        changeCount(cat, diff, true);    }

    public void setCount(final String cat, final int newCount) {
        changeCount(cat, newCount, false);
    }

    private void changeCount(final String cat, final int value, final boolean isDiff) {

        if (!categoryCounts.containsKey(cat)) {
            addOrMoveCategory(cat, false);
        }
        final int currentCount = categoryCounts.get(cat);
        final int newCount = isDiff ? currentCount + value : value;
        contentCount += newCount - currentCount;
        categoryCounts.put(cat, newCount);
        invalidateIndexes(cat);
    }

    public int getListIndexForContentIndex(final int contentIndex) {
        if (contentIndex < 0 || contentIndex >= contentCount) {
            return -1;
        }
        ensureContentIndexListIndexMap();
        return contentIndex + contentIndexListIndexMap.floorEntry(contentIndex).getValue();
    }

    public int getContentIndexForListIndex(final int listIndex) {

        if (listIndex <= 0 || listIndex >= contentCount + categories.size()) {
            return -1;
        }

        //no caching of anything yet
        int contentIndex = listIndex - 1;

        int pos = 0;
        for (String category : categories) {
            pos += categoryCounts.get(category) + 1;
            if (pos == listIndex) {
                //listindex points to a category header -> no content index
                return -1;
            }
            if (pos > listIndex) {
                return contentIndex;
            }
            contentIndex--;
        }
        return contentIndex;

    }

    /** gets number of content entries in the list */
    public int getContentSize() {
        return contentCount;
    }

    public void clear() {
        this.categories.clear();
        this.categoryCounts.clear();
        this.contentCount = 0;
        this.contentIndexListIndexMap.clear();
        this.categoryIndexMap.clear();
    }

    private void invalidateIndexes(final String category) {

        //special case: this is a new last category. Then just build up the index (if it exists) instead of deleting it
        if (category != null && categories.get(categories.size() - 1).equals(category) &&
                !categoryIndexMap.isEmpty() && !categoryIndexMap.containsKey(category)) {
            if (categories.size() == 1) {
                //-> first category added
                categoryIndexMap.put(category, 0);
            } else {
                final String prevCategory = categories.get(categories.size() - 2);
                final int prevIndex = categoryIndexMap.get(prevCategory);
                final int prevCount = categoryCounts.get(prevIndex);
                categoryIndexMap.put(category, prevIndex + prevCount + 1);
            }
        } else {
            //just marke everything dirty
            categoryIndexMap.clear();
        }
        contentIndexListIndexMap.clear();
    }

    private void ensureCategoryIndexMap() {
        if (categories.isEmpty() || !categoryIndexMap.isEmpty()) {
            return;
        }

        int pos = 0;
        for (String cat : categories) {
            final int catCount = categoryCounts.get(cat);
            categoryIndexMap.put(cat, pos);
            pos += catCount + 1;
        }
    }

    private void ensureContentIndexListIndexMap() {
        if (categories.isEmpty() || !contentIndexListIndexMap.isEmpty()) {
            return;
        }

        int contentPos = 0;
        int catCounter = 0;
        for (String cat : categories) {
            catCounter++;
            contentIndexListIndexMap.put(contentPos, catCounter);
            contentPos += categoryCounts.get(cat);
        }
    }




}
