package cgeo.geocaching.storage.extension;

import cgeo.geocaching.storage.DataStore;

import java.util.ArrayList;
import java.util.ListIterator;

public class EmojiLRU extends DataStore.DBExtension {

    private static final DataStore.DBExtensionType type = DataStore.DBExtensionType.DBEXTENSION_EMOJILRU;
    public static final int MAX_LRU_LENGTH = 10;

    private EmojiLRU(final DataStore.DBExtension copyFrom) {
        super(copyFrom);
    }

    public static void add(final int character) {
        final String key = String.valueOf(character);
        removeAll(type, key);
        add(type, key, 0, 0, 0, 0, "", "", "", "");
    }

    /**
     * returns a list of max MAX_LRU_LENGTH LRU elements
     * LRU list is truncated in db after MAX_LRU_LENGTH automatically
     *
     * @return LRU list, in reverse chronological order
     */
    public static int[] getLRU() {
        final ArrayList<DataStore.DBExtension> storedValues = getAll(type, null);
        if (storedValues.isEmpty()) {
            return new int[]{};
        }

        final int[] result = new int[Math.min(storedValues.size(), MAX_LRU_LENGTH)];
        int arrayPos = 0;
        for (ListIterator<DataStore.DBExtension> iterator = storedValues.listIterator(storedValues.size()); iterator.hasPrevious(); ) {
            final DataStore.DBExtension element = iterator.previous();
            if (arrayPos < MAX_LRU_LENGTH) {
                result[arrayPos++] = Integer.parseInt(element.getKey());
            } else {
                removeAll(type, element.getKey());
            }
        }
        return result;
    }

}
