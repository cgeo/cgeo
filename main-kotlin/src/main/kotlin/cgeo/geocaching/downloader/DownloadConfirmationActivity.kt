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

import android.net.Uri
import android.os.Bundle

class DownloadConfirmationActivity : AbstractActivity() {

    public static val BUNDLE_FILENAME: String = "filename"

    override     public Unit onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState)
        setTheme()

        val bundle: Bundle = getIntent().getExtras()
        if (bundle != null) {
            val uri: Uri = Uri.parse(getString(R.string.brouter_downloadurl) + bundle.getString(BUNDLE_FILENAME))
            DownloaderUtils.triggerDownload(this, R.string.downloadtile_title, Download.DownloadType.DOWNLOADTYPE_BROUTER_TILES.id, uri, getString(R.string.downloadtile_info), "", this::finish, null)
        }
    }

}
