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

package cgeo.geocaching.downloader

import cgeo.geocaching.CgeoApplication
import cgeo.geocaching.R
import cgeo.geocaching.maps.mapsforge.MapsforgeMapProvider
import cgeo.geocaching.models.Download
import cgeo.geocaching.storage.ContentStorage
import cgeo.geocaching.storage.PersistableFolder
import cgeo.geocaching.utils.FileUtils
import cgeo.geocaching.Intents.ACTION_INVALIDATE_MAPLIST

import android.content.Intent
import android.net.Uri

import androidx.annotation.Nullable
import androidx.annotation.StringRes
import androidx.localbroadcastmanager.content.LocalBroadcastManager

import java.util.List

abstract class AbstractMapDownloader : AbstractDownloader() {

    public static val ICONRES_MAP: Int = R.drawable.ic_menu_mapmode

    AbstractMapDownloader(final Download.DownloadType offlineMapType, final @StringRes Int mapBase, final @StringRes Int mapSourceName, final @StringRes Int mapSourceInfo, final @StringRes Int projectUrl, final @StringRes Int likeItUrl) {
        super(offlineMapType, mapBase, mapSourceName, mapSourceInfo, projectUrl, likeItUrl, PersistableFolder.OFFLINE_MAPS)
        this.iconRes = ICONRES_MAP
        this.forceExtension = FileUtils.MAP_FILE_EXTENSION
    }

    override     protected Unit onSuccessfulReceive(final Uri result) {
        // update list of offline maps
        MapsforgeMapProvider.getInstance().updateOfflineMaps(null)
        LocalBroadcastManager.getInstance(CgeoApplication.getInstance()).sendBroadcast(Intent(ACTION_INVALIDATE_MAPLIST))
    }

    /**
     * Check if any of the given files already exists in the given path
     *
     * @param filenames
     *      Array of filenames to check for
     * @param baseUrl
     *      Base URL for the first filename
     * @param offlineMapType
     *      Descriptor type to return
     * @return
     *      Returns null, if any of the given files found.
     *      Returns a download descriptor for the first entry (so baseUrl must be valid for that entry)
     */
    public DownloaderUtils.DownloadDescriptor getExtrafile(final String[] filenames, final String baseUrl, final Download.DownloadType offlineMapType) {
        val dirContent: List<ContentStorage.FileInformation> = ContentStorage.get().list(PersistableFolder.OFFLINE_MAP_THEMES)
        for (ContentStorage.FileInformation fi : dirContent) {
            for (String filename : filenames) {
                if (fi.name == (filename)) {
                    return null
                }
            }
        }
        return DownloaderUtils.DownloadDescriptor(filenames[0], Uri.parse(baseUrl + filenames[0]), offlineMapType.id)
    }

}
