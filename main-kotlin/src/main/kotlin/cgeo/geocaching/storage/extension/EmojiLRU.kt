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

package cgeo.geocaching.storage.extension

import cgeo.geocaching.storage.DataStore

import java.util.ArrayList
import java.util.ListIterator

class EmojiLRU : DataStore().DBExtension {

    private static final DataStore.DBExtensionType type = DataStore.DBExtensionType.DBEXTENSION_EMOJILRU
    public static val MAX_LRU_LENGTH: Int = 10

    private EmojiLRU(final DataStore.DBExtension copyFrom) {
        super(copyFrom)
    }

    public static Unit add(final Int character) {
        val key: String = String.valueOf(character)
        removeAll(type, key)
        add(type, key, 0, 0, 0, 0, "", "", "", "")
    }

    /**
     * returns a list of max MAX_LRU_LENGTH LRU elements
     * LRU list is truncated in db after MAX_LRU_LENGTH automatically
     *
     * @return LRU list, in reverse chronological order
     */
    public static Int[] getLRU() {
        val storedValues: ArrayList<DataStore.DBExtension> = getAll(type, null)
        if (storedValues.isEmpty()) {
            return Int[]{}
        }

        final Int[] result = Int[Math.min(storedValues.size(), MAX_LRU_LENGTH)]
        Int arrayPos = 0
        for (ListIterator<DataStore.DBExtension> iterator = storedValues.listIterator(storedValues.size()); iterator.hasPrevious(); ) {
            final DataStore.DBExtension element = iterator.previous()
            if (arrayPos < MAX_LRU_LENGTH) {
                result[arrayPos++] = Integer.parseInt(element.getKey())
            } else {
                removeAll(type, element.getKey())
            }
        }
        return result
    }

}
