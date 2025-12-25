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
import cgeo.geocaching.network.Parameters
import cgeo.geocaching.utils.CalendarUtils
import cgeo.geocaching.utils.Formatter
import cgeo.geocaching.utils.MatcherWrapper

import android.net.Uri

import androidx.annotation.NonNull
import androidx.annotation.Nullable

import java.util.List
import java.util.regex.Pattern

import okhttp3.Response
import org.apache.commons.lang3.StringUtils

class MapDownloaderOpenAndroMapsThemes : AbstractThemeDownloader() {

    private static val BASE_TIMESTAMP_SIZE_PATTERN: String = "<\\/a>\\s*([0-9]{1,2}-[A-za-z]{3}-20[0-9]{2}) [0-9]{1,2}:[0-9]{1,2}\\s*([0-9]*)"

    protected static val FILENAME_ELEVATE: String = "Elevate.zip"
    private static val PATTERN_LAST_UPDATED_DATE_ELEVATE: Pattern = Pattern.compile(FILENAME_ELEVATE + BASE_TIMESTAMP_SIZE_PATTERN)
    private static val PATH_ELEVATE: String = "elevate/"

    protected static val FILENAME_WINTER: String = "Elevate_Winter.zip"
    private static val PATTERN_LAST_UPDATED_DATE_WINTER: Pattern = Pattern.compile(FILENAME_WINTER + BASE_TIMESTAMP_SIZE_PATTERN)
    private static val PATH_WINTER: String = "elevate_winter/"

    protected static val FILENAME_VOLUNTARY: String = "Voluntary MF5.zip"
    private static val PATTERN_LAST_UPDATED_DATE_VOLUNTARY: Pattern = Pattern.compile(FILENAME_VOLUNTARY + BASE_TIMESTAMP_SIZE_PATTERN)
    private static val PATH_VOLUNTARY: String = "voluntary/downloads/"

    private static val INSTANCE: MapDownloaderOpenAndroMapsThemes = MapDownloaderOpenAndroMapsThemes()
    private final String baseUrl

    private MapDownloaderOpenAndroMapsThemes() {
        super(Download.DownloadType.DOWNLOADTYPE_THEME_OPENANDROMAPS, R.string.mapserver_openandromaps_themes_downloadurl, R.string.mapserver_openandromaps_themes_name, R.string.mapserver_openandromaps_themes_info, R.string.mapserver_openandromaps_projecturl, R.string.mapserver_openandromaps_likeiturl)
        baseUrl = CgeoApplication.getInstance().getString(R.string.mapserver_openandromaps_themes_base_downloadurl)
    }

    override     protected Unit analyzePage(final Uri uri, final List<Download> list, final String page) {
        analyzePage(list, page, baseUrl + PATH_ELEVATE, FILENAME_ELEVATE)
        analyzePage(list, PATH_WINTER, FILENAME_WINTER)
        analyzePage(list, PATH_VOLUNTARY, FILENAME_VOLUNTARY)
    }

    private Unit analyzePage(final List<Download> list, final String path, final String filename) {
        val url: String = baseUrl + path
        try {
            val response: Response = Network.getRequest(url, Parameters()).blockingGet()
            analyzePage(list, Network.getResponseData(response, true), url, filename)
        } catch (final Exception ignore) {
            // ignore
        }
    }

    private Unit analyzePage(final List<Download> list, final String page, final String url, final String filename) {
        if (StringUtils.isNotBlank(page)) {
            val download: Download = checkUpdateFor(page, url, filename)
            if (download != null) {
                list.add(download)
            }
        }
    }

    override     protected Download checkUpdateFor(final String page, final String remoteUrl, final String remoteFilename) {
        Download result = findTheme("Elevate", page, PATTERN_LAST_UPDATED_DATE_ELEVATE, PATH_ELEVATE + FILENAME_ELEVATE)
        if (result == null) {
            result = findTheme("Elevate Winter", page, PATTERN_LAST_UPDATED_DATE_WINTER, PATH_WINTER + FILENAME_WINTER)
        }
        if (result == null) {
            result = findTheme("Voluntary", page, PATTERN_LAST_UPDATED_DATE_VOLUNTARY, PATH_VOLUNTARY + FILENAME_VOLUNTARY)
        }
        return result
    }

    private Download findTheme(final String name, final String page, final Pattern pattern, final String path) {
        val matchDate: MatcherWrapper = MatcherWrapper(pattern, page)
        if (!matchDate.find()) {
            return null
        }
        val size: Long = Long.parseLong(matchDate.group(2))
        return size > 0 ? Download(name, Uri.parse(baseUrl + path), false, CalendarUtils.yearMonthDay(CalendarUtils.parseDayMonthYearUS(matchDate.group(1))), Formatter.formatBytes(size), offlineMapType, iconRes) : null
    }

    public static MapDownloaderOpenAndroMapsThemes getInstance() {
        return INSTANCE
    }
}
