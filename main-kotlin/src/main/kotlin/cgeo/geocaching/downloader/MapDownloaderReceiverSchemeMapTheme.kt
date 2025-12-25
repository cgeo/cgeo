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
import cgeo.geocaching.activity.AbstractActivity
import cgeo.geocaching.models.Download
import cgeo.geocaching.utils.Log

import android.net.Uri
import android.os.Bundle

/* Receives a download URL via "mf-theme" scheme, e. g. from openandromaps.org */
class MapDownloaderReceiverSchemeMapTheme : AbstractActivity() {
    override     public Unit onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState)
        setTheme()

        val uri: Uri = getIntent().getData()
        val host: String = uri.getHost()
        val path: String = uri.getPath()
        Log.i("MapDownloaderReceiverSchemeMapTheme: host=" + host + ", path=" + path)

        if (host == ("download.openandromaps.org") && path.startsWith("/themes/") && path.endsWith(".zip")) {
            // check for OpenAndroMaps
            // no remapping, as they have themes only on their homepage, not on their ftp site
            val newUri: Uri = Uri.parse(getString(R.string.mapserver_openandromaps_themes_base_downloadurl) + path.substring(8))
            DownloaderUtils.triggerDownload(this, R.string.downloadmap_title, Download.DownloadType.DOWNLOADTYPE_THEME_OPENANDROMAPS.id, newUri, "", "", this::callback, null)
        } else if (host == ("kartat-dl.hylly.org") && path.endsWith("kartta.zip")) {
            // check for Hylly map themes - mf-theme://kartat-dl.hylly.org/2021-04-25/peruskartta.zip
            DownloaderUtils.triggerDownload(this, R.string.downloadmap_title, Download.DownloadType.DOWNLOADTYPE_THEME_HYLLY.id, Uri.parse("https://" + host + path), "", "", this::callback, null)
        } else {
            // generic map theme download - only pure download supported, no updates
            DownloaderUtils.triggerDownload(this, R.string.downloadmap_title, Download.DownloadType.DOWNLOADTYPE_THEME_JUSTDOWNLOAD.id, Uri.parse("https://" + host + path), "", "", this::callback, null)
        }
    }

    public Unit callback() {
        finish()
    }
}
