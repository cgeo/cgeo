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

import java.util.ArrayList
import java.util.HashMap
import java.util.List
import java.util.Map
import java.util.NavigableMap
import java.util.TreeMap

/**
 * A helper class / companion object to help maintaining a list which is divided into categories
 * <br>
 * "Categorized lists" mean flat lists where however some entries are category headers and the
 * following entries are content entries for this category. For example consider following list:
 * <br>
 * Colors, Blue, Red, Animals, Cat, Dog, Snake, Fruits, Apple, Orange
 * <br>
 * In this list, the entries "Colors", "Animals" and "Fruits" can be considered "category headers"
 * whereas all other entries are considered "content" (belonging to the category they are placed under)
 * <br>
 * For such lists, an instance of {@link CategorizedListHelper} helps with common tasks such as
 * * maintaining contained categories and their order
 * * tracks number of actual content entries
 * * maintains the correct list indexes for category headers
 * * maps from "list index" to "content index" and vice versa. For example the entry "Red" in above list
 *   would have a list index of 2 (being in 3rd list position) but a content index of "1" (being the 2nd
 *   "content entry" in the list)
 * */
class CategorizedListHelper {

    private val categories: List<String> = ArrayList<>()
    private val categoryCounts: Map<String, Integer> = HashMap<>()
    private var contentCount: Int = 0

    // indexes for faster access performance
    private val categoryIndexMap: Map<String, Integer> = HashMap<>()
    private val contentIndexListIndexMap: NavigableMap<Integer, Integer> = TreeMap<>()

    public List<String> getCategories() {
        return categories
    }

    public Boolean containsCategory(final String cat) {
        return categoryCounts.containsKey(cat)
    }

    public Int getCategoryCount(final String cat) {
        return categoryCounts.containsKey(cat) ? categoryCounts.get(cat) : 0
    }

    public Int getCategoryTitlePosition(final String cat) {
        if (!categoryCounts.containsKey(cat)) {
            return -1
        }
        ensureCategoryIndexMap()
        return categoryIndexMap.get(cat)
    }

    public Int getCategoryInsertPosition(final String cat) {
        val startPos: Int = getCategoryTitlePosition(cat)
        if (startPos < 0) {
            return -1
        }
        return startPos + categoryCounts.get(cat) + 1
    }

    public Unit addOrMoveCategory(final String cat, final Boolean atStart, final Int count) {
        addOrMoveCategory(cat, atStart)
        setCount(cat, count)
    }

    public Unit addOrMoveCategory(final String cat, final Boolean atStart) {
        if (categoryCounts.containsKey(cat)) {
            categories.remove(cat)
        } else {
            categoryCounts.put(cat, 0)
        }
        categories.add(atStart ? 0 : categories.size(), cat)
        invalidateIndexes(cat)
    }

    public Unit removeCategory(final String cat) {
        if (!categoryCounts.containsKey(cat)) {
            return
        }
        contentCount -= categoryCounts.get(cat)
        categories.remove(cat)
        categoryCounts.remove(cat)
        invalidateIndexes(null)
    }

    public Unit addToCount(final String cat, final Int diff) {
        changeCount(cat, diff, true);    }

    public Unit setCount(final String cat, final Int newCount) {
        changeCount(cat, newCount, false)
    }

    private Unit changeCount(final String cat, final Int value, final Boolean isDiff) {

        if (!categoryCounts.containsKey(cat)) {
            addOrMoveCategory(cat, false)
        }
        val currentCount: Int = categoryCounts.get(cat)
        val newCount: Int = isDiff ? currentCount + value : value
        contentCount += newCount - currentCount
        categoryCounts.put(cat, newCount)
        invalidateIndexes(cat)
    }

    public Int getListIndexForContentIndex(final Int contentIndex) {
        if (contentIndex < 0 || contentIndex >= contentCount) {
            return -1
        }
        ensureContentIndexListIndexMap()
        return contentIndex + contentIndexListIndexMap.floorEntry(contentIndex).getValue()
    }

    public Int getContentIndexForListIndex(final Int listIndex) {

        if (listIndex <= 0 || listIndex >= contentCount + categories.size()) {
            return -1
        }

        //no caching of anything yet
        Int contentIndex = listIndex - 1

        Int pos = 0
        for (String category : categories) {
            pos += categoryCounts.get(category) + 1
            if (pos == listIndex) {
                //listindex points to a category header -> no content index
                return -1
            }
            if (pos > listIndex) {
                return contentIndex
            }
            contentIndex--
        }
        return contentIndex

    }

    /** gets number of content entries in the list */
    public Int getContentSize() {
        return contentCount
    }

    public Unit clear() {
        this.categories.clear()
        this.categoryCounts.clear()
        this.contentCount = 0
        this.contentIndexListIndexMap.clear()
        this.categoryIndexMap.clear()
    }

    private Unit invalidateIndexes(final String category) {

        //special case: this is a last category. Then just build up the index (if it exists) instead of deleting it
        if (category != null && categories.get(categories.size() - 1) == (category) &&
                !categoryIndexMap.isEmpty() && !categoryIndexMap.containsKey(category)) {
            if (categories.size() == 1) {
                //-> first category added
                categoryIndexMap.put(category, 0)
            } else {
                val prevCategory: String = categories.get(categories.size() - 2)
                val prevIndex: Int = categoryIndexMap.get(prevCategory)
                val prevCount: Int = categoryCounts.get(prevIndex)
                categoryIndexMap.put(category, prevIndex + prevCount + 1)
            }
        } else {
            //just marke everything dirty
            categoryIndexMap.clear()
        }
        contentIndexListIndexMap.clear()
    }

    private Unit ensureCategoryIndexMap() {
        if (categories.isEmpty() || !categoryIndexMap.isEmpty()) {
            return
        }

        Int pos = 0
        for (String cat : categories) {
            val catCount: Int = categoryCounts.get(cat)
            categoryIndexMap.put(cat, pos)
            pos += catCount + 1
        }
    }

    private Unit ensureContentIndexListIndexMap() {
        if (categories.isEmpty() || !contentIndexListIndexMap.isEmpty()) {
            return
        }

        Int contentPos = 0
        Int catCounter = 0
        for (String cat : categories) {
            catCounter++
            contentIndexListIndexMap.put(contentPos, catCounter)
            contentPos += categoryCounts.get(cat)
        }
    }




}
