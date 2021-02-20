package cgeo.geocaching.downloader;

import cgeo.geocaching.R;
import cgeo.geocaching.models.OfflineMap;
import cgeo.geocaching.storage.ContentStorage;
import cgeo.geocaching.storage.PersistableFolder;
import cgeo.geocaching.ui.dialog.Dialogs;
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
    private static final String ELEVATE_THEME = "Elevate.zip";
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

    @Override
    protected String toVisibleFilename(final String filename) {
        final int posInfix = filename.indexOf("_oam.osm.");
        return toInfixedString(posInfix == -1 ? filename : filename.substring(0, posInfix) + filename.substring(posInfix + 8), " (OAM)");
    }

    @Override
    protected void onFollowup(final Activity activity, final Runnable callback) {
        // check whether Elevate.zip exists in theme folder and ask whether user wants to download it as well, if it does not exist yet
        boolean themeFound = false;
        final List<ContentStorage.FileInformation> mapDirContent = ContentStorage.get().list(PersistableFolder.OFFLINE_MAP_THEMES);
        for (ContentStorage.FileInformation fi : mapDirContent) {
            if (fi.name.equals(ELEVATE_THEME)) {
                themeFound = true;
                break;
            }
        }

        if (!themeFound) {
            Dialogs.confirm(activity, activity.getString(R.string.downloadmap_install_theme_title), activity.getString(R.string.downloadmap_install_theme_info), activity.getString(android.R.string.ok), (d, w) -> {
                final Uri newUri = Uri.parse(activity.getString(R.string.mapserver_elevate_downloadurl) + ELEVATE_THEME);
                MapDownloaderUtils.triggerDownload(activity, OfflineMap.OfflineMapType.MAP_DOWNLOAD_TYPE_ELEVATE.id, newUri, "", System.currentTimeMillis(), callback);
            }, dialog -> callback.run());
        } else {
            callback.run();
        }
    }

    @NonNull
    public static MapDownloaderOpenAndroMaps getInstance() {
        return INSTANCE;
    }
}
