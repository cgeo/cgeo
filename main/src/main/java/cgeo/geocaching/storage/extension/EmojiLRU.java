package cgeo.geocaching.storage.extension;

import cgeo.geocaching.storage.DataStore;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.ListIterator;

public class EmojiLRU extends DataStore.DBExtension {

    private static final DataStore.DBExtensionType type = DataStore.DBExtensionType.DBEXTENSION_EMOJISTRINGLRU;

    private EmojiLRU(final DataStore.DBExtension copyFrom) {
        super(copyFrom);
    }

    public static void add(final String emoji, final int maxLruLength) {
        if (emoji == null || emoji.isEmpty()) {
            return;
        }
        removeAll(type, emoji);
        add(type, emoji, 0, 0, 0, 0, "", "", "", "");

        // limit list to X items, remove all items beyond that
        final List<String> result = getLRU();
        if (result.size() > maxLruLength) {
            for (String e : result.subList(maxLruLength, result.size())) {
                removeAll(type, e);
            }
        }
    }

    /**
     * returns a list of max MAX_LRU_LENGTH LRU elements
     * LRU list is truncated in db after MAX_LRU_LENGTH automatically
     *
     * @return LRU list, in reverse chronological order with no null entries
     */
    @NonNull
    public static List<String> getLRU() {
        final ArrayList<DataStore.DBExtension> storedValues = getAll(type, null);
        if (storedValues.isEmpty()) {
            return Collections.emptyList();
        }

        final List<String> result = new ArrayList<>(storedValues.size());
        for (ListIterator<DataStore.DBExtension> iterator = storedValues.listIterator(storedValues.size()); iterator.hasPrevious(); ) {
            final DataStore.DBExtension element = iterator.previous();
            if (element.getKey() != null) {
                result.add(element.getKey());
            }
        }
        return result;
    }
}
