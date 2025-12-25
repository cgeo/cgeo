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

import cgeo.geocaching.R
import cgeo.geocaching.maps.mapsforge.v6.RenderThemeHelper
import cgeo.geocaching.models.Download
import cgeo.geocaching.storage.PersistableFolder
import cgeo.geocaching.utils.FileUtils

import android.net.Uri

import androidx.annotation.StringRes

abstract class AbstractThemeDownloader : AbstractDownloader() {

    public static val ICONRES_THEME: Int = R.drawable.downloader_theme

    AbstractThemeDownloader(final Download.DownloadType offlineMapType, final @StringRes Int mapBase, final @StringRes Int mapSourceName, final @StringRes Int mapSourceInfo, final @StringRes Int projectUrl, final @StringRes Int likeItUrl) {
        super(offlineMapType, mapBase, mapSourceName, mapSourceInfo, projectUrl, likeItUrl, PersistableFolder.OFFLINE_MAP_THEMES)
        this.iconRes = ICONRES_THEME
        this.forceExtension = FileUtils.ZIP_FILE_EXTENSION
    }

    override     protected Unit onSuccessfulReceive(final Uri result) {
        //resync
        RenderThemeHelper.resynchronizeOrDeleteMapThemeFolder()
    }

}
