package cgeo.geocaching.storage.extension;

import cgeo.geocaching.storage.DataStore;

import androidx.annotation.Nullable;

/**
 * Offline storage for the connector specific found number counter, mainly used in the NUMBER log template.
 *
 */
public class FoundNumCounter extends DataStore.DBExtension {

    private static final DataStore.DBExtensionType type = DataStore.DBExtensionType.DBEXTENSION_FOUNDNUM;

    private FoundNumCounter(final DataStore.DBExtension copyFrom) {
        super(copyFrom);
    }

    public int getCounter(final boolean incrementCounter) {
        return (int) (incrementCounter ? getLong1() + 1 : getLong1());
    }

    @Nullable
    public static FoundNumCounter load(final String serviceName) {
        final DataStore.DBExtension temp = load(type, serviceName);
        return null == temp ? null : new FoundNumCounter(temp);
    }

    public static void updateFoundNum(final String serviceName, final int foundNum) {

        //avoid recreation of same key/value pair
        final FoundNumCounter f = load(serviceName);
        if (f != null && f.getCounter(false) == foundNum) {
            return;
        }

        removeAll(type, serviceName);
        add(type, serviceName, foundNum, 0, 0, 0, "", "", "", "");
    }
}
