package cgeo.geocaching.storage.extension;

import cgeo.geocaching.models.GCList;
import cgeo.geocaching.storage.DataStore;


/**
 * Offline storage for pocket query generation timestamps. These are used to detect if the pocket query has changed since the last download.
 */
public class PocketQueryHistory extends DataStore.DBExtension {

    private static final DataStore.DBExtensionType type = DataStore.DBExtensionType.DBEXTENSION_POCKETQUERY_HISTORY;

    private PocketQueryHistory(final DataStore.DBExtension copyFrom) {
        super(copyFrom);
    }

    /**
     * Check if the pocket query has been regenerated since the last download
     *
     * @param pocketQuery the list / pocket query which should be checked
     * @return true if the list has changed since the last download. Otherwise false.
     */
    public static boolean isNew(final GCList pocketQuery) {
        final DataStore.DBExtension lastDownload = load(type, pocketQuery.getGuid());

        if (!pocketQuery.isDownloadable()) {
            // The pocket query cannot be downloaded. It doesn't make sense to show "new" in this case.
            return false;
        }

        if (lastDownload == null) {
            // The pocket query was never downloaded. We decided to show "new" in this case.
            return true;
        }

        return pocketQuery.getLastGenerationTime() > lastDownload.getLong1();
    }

    public static void updateLastDownload(final GCList pocketQuery) {
        removeAll(type, pocketQuery.getGuid());
        add(type, pocketQuery.getGuid(), pocketQuery.getLastGenerationTime(), 0, 0, 0, "", "", "", "");
    }
}
