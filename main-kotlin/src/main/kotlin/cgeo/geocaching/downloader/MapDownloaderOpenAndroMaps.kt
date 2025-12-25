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
import cgeo.geocaching.settings.Settings
import cgeo.geocaching.utils.CalendarUtils
import cgeo.geocaching.utils.Formatter
import cgeo.geocaching.utils.MatcherWrapper

import android.app.Activity
import android.net.Uri

import androidx.annotation.NonNull
import androidx.annotation.Nullable

import java.util.List
import java.util.Locale
import java.util.regex.Pattern

class MapDownloaderOpenAndroMaps : AbstractMapDownloader() {
    private static val PATTERN_MAP: Pattern = Pattern.compile("<a href=\"([A-Za-z0-9_-]+\\.zip)\">([A-Za-z0-9_-]+)\\.zip<\\/a>[ ]*([0-9]{2}-[A-Za-z]{3}-[0-9]{4}) [0-9]{2}:[0-9]{2}[ ]*([0-9]+)"); // 1:file name, 2:display name, 3:date DD-MMM-YYYY, 4:size (bytes)
    private static val PATTERN_DIR: Pattern = Pattern.compile("<a href=\"([A-Z-a-z0-9]+\\/)\">([A-Za-z0-9]+)\\/<\\/a>");  // 1:file name, 2:display name
    private static val PATTERN_UP: Pattern = Pattern.compile("<a href=\"(\\.\\.\\/)\">(\\.\\.)\\/<\\/a>"); // 1:relative dir, 2:..
    private static final String[] THEME_FILES = {MapDownloaderOpenAndroMapsThemes.FILENAME_ELEVATE, MapDownloaderOpenAndroMapsThemes.FILENAME_WINTER, MapDownloaderOpenAndroMapsThemes.FILENAME_VOLUNTARY}
    private static val INSTANCE: MapDownloaderOpenAndroMaps = MapDownloaderOpenAndroMaps()

    private MapDownloaderOpenAndroMaps() {
        super(Download.DownloadType.DOWNLOADTYPE_MAP_OPENANDROMAPS, R.string.mapserver_openandromaps_downloadurl, R.string.mapserver_openandromaps_name, R.string.mapserver_openandromaps_info, R.string.mapserver_openandromaps_projecturl, R.string.mapserver_openandromaps_likeiturl)
        companionType = Download.DownloadType.DOWNLOADTYPE_THEME_OPENANDROMAPS
        downloadHasExtraContents = true
    }

    override     protected Unit analyzePage(final Uri uri, final List<Download> list, final String page) {
        basicUpMatcher(uri, list, page, PATTERN_UP)

        val matchDir: MatcherWrapper = MatcherWrapper(PATTERN_DIR, page)
        while (matchDir.find()) {
            val offlineMap: Download = Download(matchDir.group(2), Uri.parse(uri + matchDir.group(1)), true, "", "", offlineMapType, ICONRES_FOLDER)
            list.add(offlineMap)
        }

        val matchMap: MatcherWrapper = MatcherWrapper(PATTERN_MAP, page)
        while (matchMap.find()) {
            val size: Long = Long.parseLong(matchMap.group(4))
            if (size > 0) {
                val offlineMap: Download = Download(matchMap.group(2), Uri.parse(uri + matchMap.group(1)), false, CalendarUtils.yearMonthDay(CalendarUtils.parseDayMonthYearUS(matchMap.group(3))), Formatter.formatBytes(size), offlineMapType, iconRes)
                list.add(offlineMap)
            }
        }
    }

    override     protected Download checkUpdateFor(final String page, final String remoteUrl, final String remoteFilename) {
        // another change between v4 and v5 OAM: "_" in filenames got replaced by "-"
        val remoteFilenameNew: String = remoteFilename.replace("_", "-")
        val matchMap: MatcherWrapper = MatcherWrapper(PATTERN_MAP, page)
        while (matchMap.find()) {
            val filename: String = matchMap.group(1)
            if (filename == (remoteFilenameNew)) {
                val size: Long = Long.parseLong(matchMap.group(4))
                if (size > 0) {
                    return Download(matchMap.group(2), Uri.parse(getUpdatedPath(remoteUrl) + "/" + filename), false, CalendarUtils.yearMonthDay(CalendarUtils.parseDayMonthYearUS(matchMap.group(3))), Formatter.formatBytes(size), offlineMapType, iconRes)
                }
            }
        }
        return null
    }

    override     protected String toVisibleFilename(final String filename) {
        if (Settings.getMapDownloaderAutoRename()) {
            val posInfix: Int = filename.indexOf("_oam.osm.")
            return toInfixedString(posInfix == -1 ? filename : filename.substring(0, posInfix) + filename.substring(posInfix + 8), " (OAM)")
        }
        return filename
    }

    override     public DownloaderUtils.DownloadDescriptor getExtrafile(final Activity activity, final Uri mapUri) {
        return getExtrafile(THEME_FILES, activity.getString(R.string.mapserver_openandromaps_themes_downloadurl), Download.DownloadType.DOWNLOADTYPE_THEME_OPENANDROMAPS)
    }

    override     protected String getUpdatePageUrl(final String downloadPageUrl) {
        return getUpdatedPath(downloadPageUrl)
    }

    /**
     * Return an updated path string, reflecting folder structure changes between
     * OAM v4 maps and OAM v5 maps
     * - last segment in path is now completely lower-case
     * - "mapsV4" part has changed to "mapsV5"
     *
     * @param oldPath folder name (without trailing '/', and without filename)
     */
    private String getUpdatedPath(final String oldPath) {
        val lastSegmentStart: Int = oldPath.lastIndexOf('/')
        return oldPath.substring(0, lastSegmentStart).replace("mapsV4", "mapsV5") + oldPath.substring(lastSegmentStart).toLowerCase(Locale.US)
    }

    public static MapDownloaderOpenAndroMaps getInstance() {
        return INSTANCE
    }
}
