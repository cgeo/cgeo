package cgeo.geocaching.downloader;

import cgeo.geocaching.R;
import cgeo.geocaching.models.Download;
import cgeo.geocaching.utils.CalendarUtils;
import cgeo.geocaching.utils.Formatter;
import cgeo.geocaching.utils.MatcherWrapper;

import android.app.Activity;
import android.net.Uri;

import androidx.annotation.NonNull;

import java.util.List;
import java.util.regex.Pattern;

public class MapDownloaderOpenAndroMaps extends AbstractMapDownloader {
    private static final Pattern PATTERN_MAP = Pattern.compile("<a href=\"([A-Za-z0-9_-]+\\.zip)\">([A-Za-z0-9_-]+)\\.zip<\\/a>[ ]*([0-9]{2}-[A-Za-z]{3}-[0-9]{4}) [0-9]{2}:[0-9]{2}[ ]*([0-9]+)"); // 1:file name, 2:display name, 3:date DD-MMM-YYYY, 4:size (bytes)
    private static final Pattern PATTERN_DIR = Pattern.compile("<a href=\"([A-Z-a-z0-9]+\\/)\">([A-Za-z0-9]+)\\/<\\/a>");  // 1:file name, 2:display name
    private static final Pattern PATTERN_UP = Pattern.compile("<a href=\"(\\.\\.\\/)\">(\\.\\.)\\/<\\/a>"); // 1:relative dir, 2:..
    private static final String[] THEME_FILES = {"Elevate.zip"};
    private static final MapDownloaderOpenAndroMaps INSTANCE = new MapDownloaderOpenAndroMaps();

    private MapDownloaderOpenAndroMaps() {
        super (Download.DownloadType.DOWNLOADTYPE_MAP_OPENANDROMAPS, R.string.mapserver_openandromaps_downloadurl, R.string.mapserver_openandromaps_name, R.string.mapserver_openandromaps_info, R.string.mapserver_openandromaps_projecturl, R.string.mapserver_openandromaps_likeiturl);
    }

    @Override
    protected void analyzePage(final Uri uri, final List<Download> list, final String page) {
        basicUpMatcher(uri, list, page, PATTERN_UP);

        final MatcherWrapper matchDir = new MatcherWrapper(PATTERN_DIR, page);
        while (matchDir.find()) {
            final Download offlineMap = new Download(matchDir.group(2), Uri.parse(uri + matchDir.group(1)), true, "", "", offlineMapType, ICONRES_FOLDER);
            list.add(offlineMap);
        }

        final MatcherWrapper matchMap = new MatcherWrapper(PATTERN_MAP, page);
        while (matchMap.find()) {
            final Download offlineMap = new Download(matchMap.group(2), Uri.parse(uri + matchMap.group(1)), false, CalendarUtils.yearMonthDay(CalendarUtils.parseDayMonthYearUS(matchMap.group(3))), Formatter.formatBytes(Long.parseLong(matchMap.group(4))), offlineMapType, iconRes);
            list.add(offlineMap);
        }
    }

    @Override
    protected Download checkUpdateFor(final String page, final String remoteUrl, final String remoteFilename) {
        final MatcherWrapper matchMap = new MatcherWrapper(PATTERN_MAP, page);
        while (matchMap.find()) {
            final String filename = matchMap.group(1);
            if (filename.equals(remoteFilename)) {
                return new Download(matchMap.group(2), Uri.parse(remoteUrl + "/" + filename), false, CalendarUtils.yearMonthDay(CalendarUtils.parseDayMonthYearUS(matchMap.group(3))), Formatter.formatBytes(Long.parseLong(matchMap.group(4))), offlineMapType, iconRes);
            }
        }
        return null;
    }

    @Override
    protected String toVisibleFilename(final String filename) {
        final int posInfix = filename.indexOf("_oam.osm.");
        return toInfixedString(posInfix == -1 ? filename : filename.substring(0, posInfix) + filename.substring(posInfix + 8), " (OAM)");
    }

    @Override
    protected void onFollowup(final Activity activity, final Runnable callback) {
        // check whether Elevate.zip exists in theme folder and ask whether user wants to download it as well, if it does not exist yet
        findOrDownload(activity, THEME_FILES, activity.getString(R.string.mapserver_openandromaps_themes_downloadurl), Download.DownloadType.DOWNLOADTYPE_THEME_OPENANDROMAPS, callback);
    }

    @NonNull
    public static MapDownloaderOpenAndroMaps getInstance() {
        return INSTANCE;
    }
}
