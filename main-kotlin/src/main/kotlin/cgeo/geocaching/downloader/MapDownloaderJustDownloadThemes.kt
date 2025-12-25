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
import cgeo.geocaching.models.Download

import android.net.Uri

import androidx.annotation.NonNull
import androidx.annotation.Nullable

import java.util.List

class MapDownloaderJustDownloadThemes : AbstractThemeDownloader() {

    private static val INSTANCE: MapDownloaderJustDownloadThemes = MapDownloaderJustDownloadThemes()

    private MapDownloaderJustDownloadThemes() {
        super(Download.DownloadType.DOWNLOADTYPE_THEME_JUSTDOWNLOAD, 0, R.string.downloadmap_themefile, 0, 0, 0)
        useCompanionFiles = false; // use single uri, and no companion files
    }

    override     protected Unit analyzePage(final Uri uri, final List<Download> list, final String page) {
        // do nothing
    }

    override     protected Download checkUpdateFor(final String page, final String remoteUrl, final String remoteFilename) {
        return null
    }

    public static MapDownloaderJustDownloadThemes getInstance() {
        return INSTANCE
    }

}
