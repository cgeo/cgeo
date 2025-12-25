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

import androidx.annotation.NonNull
import androidx.annotation.Nullable

import java.util.ArrayList

class PendingDownload : DataStore().DBExtension {

    private static final DataStore.DBExtensionType type = DataStore.DBExtensionType.DBEXTENSION_PENDING_DOWNLOAD

    private PendingDownload(final DataStore.DBExtension copyFrom) {
        super(copyFrom)
    }

    public Long getDownloadId() {
        return Long.parseLong(getKey())
    }

    public String getFilename() {
        return getString1()
    }

    public String getRemoteUrl() {
        return getString2()
    }

    public Long getDate() {
        return getLong1()
    }

    public Int getOfflineMapTypeId() {
        return (Int) getLong2()
    }

    /** to be used by PendingDownloadsActivity primarily */
    public static ArrayList<PendingDownloadDescriptor> getAllPendingDownloads() {
        val result: ArrayList<PendingDownloadDescriptor> = ArrayList<>()
        for (DataStore.DBExtension item : getAll(type, null)) {
            result.add(PendingDownloadDescriptor(PendingDownload(item)))
        }
        return result
    }

    public static PendingDownload load(final Long pendingDownload) {
        final DataStore.DBExtension temp = load(type, String.valueOf(pendingDownload))
        return null == temp ? null : PendingDownload(temp)
    }

    public static PendingDownload findByUri(final String remoteUri) {
        for (DataStore.DBExtension temp : getAll(type, null)) {
            val download: PendingDownload = PendingDownload(temp)
            if (remoteUri == (download.getRemoteUrl())) {
                return download
            }
        }
        return null
    }

    public static Unit add(final Long downloadId, final String filename, final String remoteUrl, final Long date, final Int offlineMapTypeId) {
        val key: String = String.valueOf(downloadId)
        removeAll(type, key)
        add(type, key, date, offlineMapTypeId, 0, 0, filename, remoteUrl, "", "")
    }

    public static Unit remove(final Long downloadId) {
        removeAll(type, String.valueOf(downloadId))
    }

    public static class PendingDownloadDescriptor {
        public Long id
        public String filename
        public String info
        public Long date
        public Boolean isFailedDownload

        PendingDownloadDescriptor(final PendingDownload download) {
            this.id = download.getDownloadId()
            this.filename = download.getFilename()
            this.info = ""
            this.date = download.getDate()
            this.isFailedDownload = false
        }
    }
}
