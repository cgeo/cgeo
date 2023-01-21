package cgeo.geocaching.downloader;

import cgeo.geocaching.R;
import cgeo.geocaching.models.Download;
import cgeo.geocaching.utils.CalendarUtils;
import cgeo.geocaching.utils.Formatter;
import cgeo.geocaching.utils.MatcherWrapper;

import android.app.Activity;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.List;
import java.util.regex.Pattern;

public class MapDownloaderOpenAndroMaps extends AbstractMapDownloader {
    private static final Pattern PATTERN_MAP = Pattern.compile("<a href=\"([A-Za-z0-9_-]+\\.zip)\">([A-Za-z0-9_-]+)\\.zip<\\/a>[ ]*([0-9]{2}-[A-Za-z]{3}-[0-9]{4}) [0-9]{2}:[0-9]{2}[ ]*([0-9]+)"); // 1:file name, 2:display name, 3:date DD-MMM-YYYY, 4:size (bytes)
    private static final Pattern PATTERN_DIR = Pattern.compile("<a href=\"([A-Z-a-z0-9]+\\/)\">([A-Za-z0-9]+)\\/<\\/a>");  // 1:file name, 2:display name
    private static final Pattern PATTERN_UP = Pattern.compile("<a href=\"(\\.\\.\\/)\">(\\.\\.)\\/<\\/a>"); // 1:relative dir, 2:..
    private static final String[] THEME_FILES = {"Elevate.zip"};
    private static final MapDownloaderOpenAndroMaps INSTANCE = new MapDownloaderOpenAndroMaps();

    private MapDownloaderOpenAndroMaps() {
        super(Download.DownloadType.DOWNLOADTYPE_MAP_OPENANDROMAPS, R.string.mapserver_openandromaps_downloadurl, R.string.mapserver_openandromaps_name, R.string.mapserver_openandromaps_info, R.string.mapserver_openandromaps_projecturl, R.string.mapserver_openandromaps_likeiturl);
        companionType = Download.DownloadType.DOWNLOADTYPE_THEME_OPENANDROMAPS;
    }

    @Override
    protected void analyzePage(final Uri uri, final List<Download> list, final @NonNull String page) {
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

    @Nullable
    @Override
    protected Download checkUpdateFor(final @NonNull String page, final String remoteUrl, final String remoteFilename) {
        // another change between v4 and v5 OAM: "_" in filenames got replaced by "-"
        final String remoteFilenameNew = remoteFilename.replace("_", "-");
        final MatcherWrapper matchMap = new MatcherWrapper(PATTERN_MAP, page);
        while (matchMap.find()) {
            final String filename = matchMap.group(1);
            if (filename.equals(remoteFilenameNew)) {
                return new Download(matchMap.group(2), Uri.parse(getUpdatedPath(remoteUrl) + "/" + filename), false, CalendarUtils.yearMonthDay(CalendarUtils.parseDayMonthYearUS(matchMap.group(3))), Formatter.formatBytes(Long.parseLong(matchMap.group(4))), offlineMapType, iconRes);
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
    public DownloaderUtils.DownloadDescriptor getExtrafile(final Activity activity) {
        return getExtrafile(THEME_FILES, activity.getString(R.string.mapserver_openandromaps_themes_downloadurl), Download.DownloadType.DOWNLOADTYPE_THEME_OPENANDROMAPS);
    }

    @Override
    protected String getUpdatePageUrl(final String downloadPageUrl) {
        return getUpdatedPath(downloadPageUrl);
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
        final int lastSegmentStart = oldPath.lastIndexOf('/');
        return oldPath.substring(0, lastSegmentStart).replace("mapsV4", "mapsV5") + oldPath.substring(lastSegmentStart).toLowerCase();
    }

    @NonNull
    public static MapDownloaderOpenAndroMaps getInstance() {
        return INSTANCE;
    }
}
