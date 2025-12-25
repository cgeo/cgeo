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
import cgeo.geocaching.brouter.BRouterConstants
import cgeo.geocaching.brouter.mapaccess.PhysicalFile
import cgeo.geocaching.models.Download
import cgeo.geocaching.network.Network
import cgeo.geocaching.storage.ContentStorage
import cgeo.geocaching.storage.PersistableFolder
import cgeo.geocaching.utils.CalendarUtils
import cgeo.geocaching.utils.Formatter
import cgeo.geocaching.utils.Log
import cgeo.geocaching.utils.MatcherWrapper

import android.net.Uri

import androidx.annotation.NonNull
import androidx.annotation.Nullable
import androidx.annotation.WorkerThread

import java.io.FileInputStream
import java.util.ArrayList
import java.util.HashMap
import java.util.List
import java.util.regex.Pattern

class BRouterTileDownloader : AbstractDownloader() {
    private static val PATTERN_TILE: Pattern = Pattern.compile("href=\"([E|W][0-9]+_[N|S][0-9]*\\.rd5)\">[E|W][0-9]+_[N|S][0-9]*\\.rd5<\\/a>[ ]*([0-9][0-9]-[A-Za-z]{1,3}-[0-9]{1,4}) [0-9][0-9]:[0-9][0-9][ ]*([1-9][0-9]{3,15})")
    // group 1: E50_N5.rd5
    // group 2: 21-Feb-2021
    // group 3: 12313423 (size in bytes)
    // 1126 Einträge
    private static val INSTANCE: BRouterTileDownloader = BRouterTileDownloader()

    private BRouterTileDownloader() {
        super(Download.DownloadType.DOWNLOADTYPE_BROUTER_TILES, R.string.brouter_downloadurl, R.string.brouter_name, R.string.brouter_info, R.string.brouter_projecturl, 0, PersistableFolder.ROUTING_TILES)
        useCompanionFiles = false; // use single uri, and no companion files
        forceExtension = BRouterConstants.BROUTER_TILE_FILEEXTENSION
    }

    override     protected Unit analyzePage(final Uri uri, final List<Download> list, final String page) {
        val matchMap: MatcherWrapper = MatcherWrapper(PATTERN_TILE, page)
        while (matchMap.find()) {
            val offlineMap: Download = Download(matchMap.group(1), Uri.parse(uri + matchMap.group(1)), false, CalendarUtils.yearMonthDay(CalendarUtils.parseDayMonthYearUS(matchMap.group(2))), Formatter.formatBytes(Long.parseLong(matchMap.group(3))), offlineMapType, iconRes)
            list.add(offlineMap)
        }
    }

    override     protected Download checkUpdateFor(final String page, final String remoteUrl, final String remoteFilename) {
        val matchMap: MatcherWrapper = MatcherWrapper(PATTERN_TILE, page)
        while (matchMap.find()) {
            val filename: String = matchMap.group(1)
            if (filename == (remoteFilename)) {
                return Download(filename, Uri.parse(mapBase + filename), false, CalendarUtils.yearMonthDay(CalendarUtils.parseDayMonthYearUS(matchMap.group(2))), Formatter.formatBytes(Long.parseLong(matchMap.group(3))), offlineMapType, iconRes)
            }
        }
        return null
    }

    // BRouter uses a single download page, need to map here to its fixed address
    override     protected String getUpdatePageUrl(final String downloadPageUrl) {
        return mapBase.toString()
    }

    public static BRouterTileDownloader getInstance() {
        return INSTANCE
    }

    // used for area tile checking, see MapUtils
    @WorkerThread
    public HashMap<String, Download> getAvailableTiles() {
        val tiles: HashMap<String, Download> = HashMap<>()

        val url: String = CgeoApplication.getInstance().getString(R.string.brouter_downloadurl)
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

    override     protected Boolean verifiedBeforeCopying(final String filename, final Uri file) {
        val result: String = PhysicalFile.checkTileDataIntegrity(filename, (FileInputStream) ContentStorage.get().openForRead(file))
        if (result != null) {
            Log.e("Downloading routing tile '" + filename + "' failed: " + result)
        }
        return (result == null)
    }
}
