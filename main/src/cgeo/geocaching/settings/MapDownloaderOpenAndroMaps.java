package cgeo.geocaching.settings;

import cgeo.geocaching.R;
import cgeo.geocaching.models.OfflineMap;
import cgeo.geocaching.utils.CalendarUtils;
import cgeo.geocaching.utils.Formatter;
import cgeo.geocaching.utils.MatcherWrapper;

import android.net.Uri;

import androidx.annotation.NonNull;

import java.util.List;
import java.util.regex.Pattern;

public class MapDownloaderOpenAndroMaps extends AbstractMapDownloader {
    private static final Pattern PATTERN_MAP = Pattern.compile("<a href=\"([A-Za-z0-9_-]+\\.zip)\">([A-Za-z0-9_-]+)\\.zip<\\/a>[ ]*([0-9]{2}-[A-Za-z]{3}-[0-9]{4}) [0-9]{2}:[0-9]{2}[ ]*([0-9]+)"); // 1:file name, 2:display name, 3:date DD-MMM-YYYY, 4:size (bytes)
    private static final Pattern PATTERN_DIR = Pattern.compile("<a href=\"([A-Z-a-z0-9]+\\/)\">([A-Za-z0-9]+)\\/<\\/a>");  // 1:file name, 2:display name
    private static final Pattern PATTERN_UP = Pattern.compile("<a href=\"(\\.\\.\\/)\">(\\.\\.)\\/<\\/a>"); // 1:relative dir, 2:..
    private static final MapDownloaderOpenAndroMaps INSTANCE = new MapDownloaderOpenAndroMaps();

    private MapDownloaderOpenAndroMaps() {
        super (OfflineMap.OfflineMapType.MAP_DOWNLOAD_TYPE_OPENANDROMAPS, R.string.mapserver_openandromaps_downloadurl, R.string.mapserver_openandromaps_name, R.string.mapserver_openandromaps_info, R.string.mapserver_openandromaps_projecturl, R.string.mapserver_openandromaps_likeiturl);
    }

    @Override
    protected void analyzePage(final Uri uri, final List<OfflineMap> list, final String page) {
        basicUpMatcher(uri, list, page, PATTERN_UP);

        final MatcherWrapper matchDir = new MatcherWrapper(PATTERN_DIR, page);
        while (matchDir.find()) {
            final OfflineMap offlineMap = new OfflineMap(matchDir.group(2), Uri.parse(uri + matchDir.group(1)), true, "", "", offlineMapType);
            list.add(offlineMap);
        }

        final MatcherWrapper matchMap = new MatcherWrapper(PATTERN_MAP, page);
        while (matchMap.find()) {
            final OfflineMap offlineMap = new OfflineMap(matchMap.group(2), Uri.parse(uri + matchMap.group(1)), false, CalendarUtils.yearMonthDay(CalendarUtils.parseDayMonthYearUS(matchMap.group(3))), Formatter.formatBytes(Long.parseLong(matchMap.group(4))), offlineMapType);
            list.add(offlineMap);
        }
    }

    @Override
    protected OfflineMap checkUpdateFor(final String page, final String remoteUrl, final String remoteFilename) {
        final MatcherWrapper matchMap = new MatcherWrapper(PATTERN_MAP, page);
        while (matchMap.find()) {
            final String filename = matchMap.group(1);
            if (filename.equals(remoteFilename)) {
                return new OfflineMap(matchMap.group(2), Uri.parse(remoteUrl + "/" + filename), false, CalendarUtils.yearMonthDay(CalendarUtils.parseDayMonthYearUS(matchMap.group(3))), Formatter.formatBytes(Long.parseLong(matchMap.group(4))), offlineMapType);
            }
        }
        return null;
    }

    @NonNull
    public static MapDownloaderOpenAndroMaps getInstance() {
        return INSTANCE;
    }
}
