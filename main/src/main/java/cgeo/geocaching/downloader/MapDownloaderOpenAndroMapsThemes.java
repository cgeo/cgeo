package cgeo.geocaching.downloader;

import cgeo.geocaching.CgeoApplication;
import cgeo.geocaching.R;
import cgeo.geocaching.models.Download;
import cgeo.geocaching.network.Network;
import cgeo.geocaching.network.Parameters;
import cgeo.geocaching.utils.CalendarUtils;
import cgeo.geocaching.utils.Formatter;
import cgeo.geocaching.utils.MatcherWrapper;

import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.List;
import java.util.regex.Pattern;

import okhttp3.Response;
import org.apache.commons.lang3.StringUtils;

public class MapDownloaderOpenAndroMapsThemes extends AbstractThemeDownloader {

    private static final String BASE_TIMESTAMP_SIZE_PATTERN = "<\\/a>\\s*([0-9]{1,2}-[A-za-z]{3}-20[0-9]{2}) [0-9]{1,2}:[0-9]{1,2}\\s*([0-9]*)";

    protected static final String FILENAME_ELEVATE = "Elevate.zip";
    private static final Pattern PATTERN_LAST_UPDATED_DATE_ELEVATE = Pattern.compile(FILENAME_ELEVATE + BASE_TIMESTAMP_SIZE_PATTERN);
    private static final String PATH_ELEVATE = "elevate/";

    protected static final String FILENAME_WINTER = "Elevate_Winter.zip";
    private static final Pattern PATTERN_LAST_UPDATED_DATE_WINTER = Pattern.compile(FILENAME_WINTER + BASE_TIMESTAMP_SIZE_PATTERN);
    private static final String PATH_WINTER = "elevate_winter/";

    protected static final String FILENAME_VOLUNTARY = "Voluntary MF5.zip";
    private static final Pattern PATTERN_LAST_UPDATED_DATE_VOLUNTARY = Pattern.compile(FILENAME_VOLUNTARY + BASE_TIMESTAMP_SIZE_PATTERN);
    private static final String PATH_VOLUNTARY = "voluntary/downloads/";

    private static final MapDownloaderOpenAndroMapsThemes INSTANCE = new MapDownloaderOpenAndroMapsThemes();
    private final String baseUrl;

    private MapDownloaderOpenAndroMapsThemes() {
        super(Download.DownloadType.DOWNLOADTYPE_THEME_OPENANDROMAPS, R.string.mapserver_openandromaps_themes_downloadurl, R.string.mapserver_openandromaps_themes_name, R.string.mapserver_openandromaps_themes_info, R.string.mapserver_openandromaps_projecturl, R.string.mapserver_openandromaps_likeiturl);
        baseUrl = CgeoApplication.getInstance().getString(R.string.mapserver_openandromaps_themes_base_downloadurl);
    }

    @Override
    protected void analyzePage(final Uri uri, final List<Download> list, final @NonNull String page) {
        analyzePage(list, page, baseUrl + PATH_ELEVATE, FILENAME_ELEVATE);
        analyzePage(list, PATH_WINTER, FILENAME_WINTER);
        analyzePage(list, PATH_VOLUNTARY, FILENAME_VOLUNTARY);
    }

    private void analyzePage(final List<Download> list, final String path, final String filename) {
        final String url = baseUrl + path;
        try {
            final Response response = Network.getRequest(url, new Parameters()).blockingGet();
            analyzePage(list, Network.getResponseData(response, true), url, filename);
        } catch (final Exception ignore) {
            // ignore
        }
    }

    private void analyzePage(final List<Download> list, final String page, final String url, final String filename) {
        if (StringUtils.isNotBlank(page)) {
            final Download download = checkUpdateFor(page, url, filename);
            if (download != null) {
                list.add(download);
            }
        }
    }

    @Nullable
    @Override
    protected Download checkUpdateFor(final @NonNull String page, final String remoteUrl, final String remoteFilename) {
        Download result = findTheme("Elevate", page, PATTERN_LAST_UPDATED_DATE_ELEVATE, PATH_ELEVATE + FILENAME_ELEVATE);
        if (result == null) {
            result = findTheme("Elevate Winter", page, PATTERN_LAST_UPDATED_DATE_WINTER, PATH_WINTER + FILENAME_WINTER);
        }
        if (result == null) {
            result = findTheme("Voluntary", page, PATTERN_LAST_UPDATED_DATE_VOLUNTARY, PATH_VOLUNTARY + FILENAME_VOLUNTARY);
        }
        return result;
    }

    private Download findTheme(final String name, final String page, final Pattern pattern, final String path) {
        final MatcherWrapper matchDate = new MatcherWrapper(pattern, page);
        return matchDate.find() ? new Download(name, Uri.parse(baseUrl + path), false, CalendarUtils.yearMonthDay(CalendarUtils.parseDayMonthYearUS(matchDate.group(1))), Formatter.formatBytes(Long.parseLong(matchDate.group(2))), offlineMapType, iconRes) : null;
    }

    @NonNull
    public static MapDownloaderOpenAndroMapsThemes getInstance() {
        return INSTANCE;
    }
}
