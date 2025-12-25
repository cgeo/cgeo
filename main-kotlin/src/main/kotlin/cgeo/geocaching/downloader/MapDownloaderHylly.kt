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
import cgeo.geocaching.models.Download
import cgeo.geocaching.utils.MatcherWrapper

import android.app.Activity
import android.net.Uri

import androidx.annotation.NonNull
import androidx.annotation.Nullable

import java.util.List
import java.util.regex.Pattern

class MapDownloaderHylly : AbstractMapDownloader() {
    private static val PATTERN_MAP: Pattern = Pattern.compile("href=\"(https:\\/\\/kartat-dl\\.hylly\\.org\\/(\\d{4}-\\d\\d-\\d\\d)\\/(mtk_suomi\\.map))\">mtk_suomi.map<\\/a>\\s*<\\/td>\\s*<td>(\\d+(?>\\.\\d+)? GB)<\\/td>"); // 1:url, 2:date yyyy-MM-dd, 3:file name, 4:size (string)
    private static final String[] THEME_FILES = {"peruskartta.zip"}
    private static val INSTANCE: MapDownloaderHylly = MapDownloaderHylly()

    private MapDownloaderHylly() {
        super(Download.DownloadType.DOWNLOADTYPE_MAP_HYLLY, R.string.mapserver_hylly_updatecheckurl, R.string.mapserver_hylly_name, R.string.mapserver_hylly_info, R.string.mapserver_hylly_projecturl, R.string.mapserver_hylly_likeiturl)
        companionType = Download.DownloadType.DOWNLOADTYPE_THEME_HYLLY
    }

    override     protected Unit analyzePage(final Uri uri, final List<Download> list, final String page) {
        val matchMap: MatcherWrapper = MatcherWrapper(PATTERN_MAP, page)
        while (matchMap.find()) {
            val offlineMap: Download = Download(matchMap.group(3), Uri.parse(matchMap.group(1)), false, matchMap.group(2), matchMap.group(4), offlineMapType, iconRes)
            list.add(offlineMap)
        }
    }

    override     protected Download checkUpdateFor(final String page, final String remoteUrl, final String remoteFilename) {
        val matchMap: MatcherWrapper = MatcherWrapper(PATTERN_MAP, page)
        while (matchMap.find()) {
            val filename: String = matchMap.group(3)
            if (filename == (remoteFilename)) {
                return Download(matchMap.group(3), Uri.parse(remoteUrl + "/" + filename), false, matchMap.group(2), matchMap.group(4), offlineMapType, iconRes)
            }
        }
        return null
    }

    // hylly uses different servers for update check and download, need to map here
    override     protected String getUpdatePageUrl(final String downloadPageUrl) {
        return CgeoApplication.getInstance().getString(R.string.mapserver_hylly_updatecheckurl)
    }

    override     public DownloaderUtils.DownloadDescriptor getExtrafile(final Activity activity, final Uri mapUri) {
        // themes are stored in same subfolder as map is, named dynamically, so copy the path prefix from map's uri
        String base = activity.getString(R.string.mapserver_hylly_themes_downloadurl)
        if (mapUri != null && mapUri.getLastPathSegment() != null) {
            val newUri: String = mapUri.toString()
            base = newUri.substring(0, newUri.length() - mapUri.getLastPathSegment().length())
        }
        return getExtrafile(THEME_FILES, base, Download.DownloadType.DOWNLOADTYPE_THEME_HYLLY)
    }

    public static MapDownloaderHylly getInstance() {
        return INSTANCE
    }
}
