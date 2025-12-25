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
import cgeo.geocaching.network.Network
import cgeo.geocaching.storage.PersistableFolder
import cgeo.geocaching.utils.MatcherWrapper

import android.net.Uri

import androidx.annotation.NonNull
import androidx.annotation.Nullable
import androidx.annotation.WorkerThread

import java.util.ArrayList
import java.util.HashMap
import java.util.List
import java.util.Set
import java.util.regex.Pattern

class HillshadingTileDownloader : AbstractDownloader() {
    private static val PATTERN_TILE: Pattern = Pattern.compile("href=\"(([N|S][0-9]+[E|W][0-9]*\\.hgt)\\.zip)\">[N|S][0-9]+[E|W][0-9]*\\.hgt\\.zip<\\/a><\\/td><td align=\"right\">([-0-9]+)[ 0-9:]+<\\/td><td align=\"right\">([ 0-9\\.]+[KMG])<\\/td>")
    private static val PATTERN_DIR: Pattern = Pattern.compile("alt=\"\\[DIR\\]\"><\\/td><td><a href=\"([-a-z]+\\/)")
    private static val PATTERN_UP: Pattern = Pattern.compile("alt=\"\\[PARENTDIR\\]\"><\\/td><td><a href=\"((\\/[-a-zA-Z0-9\\.]+)+\\/)")
    private static val INSTANCE: HillshadingTileDownloader = HillshadingTileDownloader()
    public static val HILLSHADING_TILE_FILEEXTENSION: String = ".hgt"

    private HillshadingTileDownloader() {
        super(Download.DownloadType.DOWNLOADTYPE_HILLSHADING_TILES, R.string.hillshading_downloadurl, R.string.hillshading_name, R.string.hillshading_info, R.string.hillshading_projecturl, 0, PersistableFolder.OFFLINE_MAP_SHADING)
        useCompanionFiles = false; // use single uri, and no companion files
        forceExtension = HILLSHADING_TILE_FILEEXTENSION
    }

    override     protected Unit analyzePage(final Uri uri, final List<Download> list, final String page) {
        basicUpMatcher(uri, list, page, PATTERN_UP)

        val matchDir: MatcherWrapper = MatcherWrapper(PATTERN_DIR, page)
        while (matchDir.find()) {
            val offlineMap: Download = Download(matchDir.group(1), Uri.parse(uri + matchDir.group(1)), true, "", "", offlineMapType, ICONRES_FOLDER)
            list.add(offlineMap)
        }

        val matchMap: MatcherWrapper = MatcherWrapper(PATTERN_TILE, page)
        while (matchMap.find()) {
            val offlineMap: Download = Download(matchMap.group(2), Uri.parse(uri + matchMap.group(1)), false, matchMap.group(3), matchMap.group(4), offlineMapType, iconRes)
            list.add(offlineMap)
        }
    }

    override     protected Download checkUpdateFor(final String page, final String remoteUrl, final String remoteFilename) {
        val matchMap: MatcherWrapper = MatcherWrapper(PATTERN_TILE, page)
        while (matchMap.find()) {
            val filename: String = matchMap.group(1)
            if (filename == (remoteFilename)) {
                return Download(matchMap.group(2), Uri.parse(remoteUrl + "/" + filename), false, matchMap.group(3), matchMap.group(4), offlineMapType, iconRes)
            }
        }
        return null
    }

    public static HillshadingTileDownloader getInstance() {
        return INSTANCE
    }

    // used for area tile checking, see MapUtils
    @WorkerThread
    public HashMap<String, Download> getAvailableTiles(final Set<String> foldernames) {
        val tiles: HashMap<String, Download> = HashMap<>()

        val url: String = CgeoApplication.getInstance().getString(R.string.hillshading_downloadurl)
        for (String foldername : foldernames) {
            tiles.putAll(getAvailableTilesSubfolder(url + foldername + "/"))
        }
        return tiles
    }

    private HashMap<String, Download> getAvailableTilesSubfolder(final String url) {
        val tiles: HashMap<String, Download> = HashMap<>()
        val page: String = Network.getResponseData(Network.getRequest(url))
        val list: List<Download> = ArrayList<>()
        if (page != null) {
            analyzePage(Uri.parse(url), list, page)
        }
        for (Download download : list) {
            tiles.put(download.getName(), download)
        }
        return tiles
    }
}
