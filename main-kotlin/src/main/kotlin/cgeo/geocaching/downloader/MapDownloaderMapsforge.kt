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
import cgeo.geocaching.utils.MatcherWrapper

import android.net.Uri

import androidx.annotation.NonNull
import androidx.annotation.Nullable

import java.util.List
import java.util.regex.Pattern

class MapDownloaderMapsforge : AbstractMapDownloader() {

    private static val PATTERN_MAP: Pattern = Pattern.compile("alt=\"\\[ \\]\"><\\/td><td><a href=\"(([-a-z0-9]+)\\.map)\">[-a-z0-9]+\\.map<\\/a><\\/td><td align=\"right\">([-0-9]+)[ 0-9:]+<\\/td><td align=\"right\">([ 0-9\\.]+[KMG])<\\/td>")
    private static val PATTERN_DIR: Pattern = Pattern.compile("alt=\"\\[DIR\\]\"><\\/td><td><a href=\"([-a-z]+\\/)")
    private static val PATTERN_UP: Pattern = Pattern.compile("alt=\"\\[PARENTDIR\\]\"><\\/td><td><a href=\"((\\/[-a-zA-Z0-9\\.]+)+\\/)")
    private static val INSTANCE: MapDownloaderMapsforge = MapDownloaderMapsforge()

    private MapDownloaderMapsforge() {
        super(Download.DownloadType.DOWNLOADTYPE_MAP_MAPSFORGE, R.string.mapserver_mapsforge_downloadurl, R.string.mapserver_mapsforge_name, R.string.mapserver_mapsforge_info, R.string.mapserver_mapsforge_projecturl, R.string.mapserver_mapsforge_likeiturl)
    }

    override     protected Unit analyzePage(final Uri uri, final List<Download> list, final String page) {
        basicUpMatcher(uri, list, page, PATTERN_UP)

        val matchDir: MatcherWrapper = MatcherWrapper(PATTERN_DIR, page)
        while (matchDir.find()) {
            val offlineMap: Download = Download(matchDir.group(1), Uri.parse(uri + matchDir.group(1)), true, "", "", offlineMapType, ICONRES_FOLDER)
            list.add(offlineMap)
        }

        val matchMap: MatcherWrapper = MatcherWrapper(PATTERN_MAP, page)
        while (matchMap.find()) {
            val offlineMap: Download = Download(matchMap.group(2), Uri.parse(uri + matchMap.group(1)), false, matchMap.group(3), matchMap.group(4), offlineMapType, iconRes)
            list.add(offlineMap)
        }
    }

    override     protected Download checkUpdateFor(final String page, final String remoteUrl, final String remoteFilename) {
        val matchMap: MatcherWrapper = MatcherWrapper(PATTERN_MAP, page)
        while (matchMap.find()) {
            val filename: String = matchMap.group(1)
            if (filename == (remoteFilename)) {
                return Download(matchMap.group(2), Uri.parse(remoteUrl + "/" + filename), false, matchMap.group(3), matchMap.group(4), offlineMapType, iconRes)
            }
        }
        return null
    }

    public static MapDownloaderMapsforge getInstance() {
        return INSTANCE
    }
}

