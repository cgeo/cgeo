package cgeo.geocaching.storage.extension;

import cgeo.geocaching.storage.DataStore;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class PendingDownload extends DataStore.DBExtension {

    private static final DataStore.DBExtensionType type = DataStore.DBExtensionType.DBEXTENSION_PENDING_DOWNLOAD;

    private PendingDownload(final DataStore.DBExtension copyFrom) {
        super(copyFrom);
    }

    public long getDownloadId() {
        return Long.parseLong(getKey());
    }

    public String getFilename() {
        return getString1();
    }

    public String getRemoteUrl() {
        return getString2();
    }

    public long getDate() {
        return getLong1();
    }

    public int getOfflineMapTypeId() {
        return (int) getLong2();
    }

    @Nullable
    public static PendingDownload load(final long pendingDownload) {
        final DataStore.DBExtension temp = load(type, String.valueOf(pendingDownload));
        return null == temp ? null : new PendingDownload(temp);
    }

    @Nullable
    public static PendingDownload findByUri(@NonNull final String remoteUri) {
        for (DataStore.DBExtension temp : getAll(type, null)) {
            final PendingDownload download = new PendingDownload(temp);
            if (remoteUri.equals(download.getRemoteUrl())) {
                return download;
            }
        }
        return null;
    }


    public static void add(final long downloadId, @NonNull final String filename, @NonNull final String remoteUrl, final long date, final int offlineMapTypeId) {
        final String key = String.valueOf(downloadId);
        removeAll(type, key);
        add(type, key, date, offlineMapTypeId, 0, 0, filename, remoteUrl, "", "");
    }

    public static void remove(final long downloadId) {
        removeAll(type, String.valueOf(downloadId));
    }
}
