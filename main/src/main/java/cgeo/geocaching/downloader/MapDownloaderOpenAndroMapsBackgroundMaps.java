package cgeo.geocaching.downloader;

import cgeo.geocaching.R;
import cgeo.geocaching.models.Download;
import cgeo.geocaching.storage.PersistableFolder;
import cgeo.geocaching.utils.CalendarUtils;
import cgeo.geocaching.utils.Formatter;
import cgeo.geocaching.utils.MatcherWrapper;

import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.List;
import java.util.regex.Pattern;

public class MapDownloaderOpenAndroMapsBackgroundMaps extends AbstractDownloader {
    private static final Pattern PATTERN_MAP = Pattern.compile("<a href=\"([A-Za-z0-9_-]+\\.mbtiles)\">(OAM-World-[A-Za-z0-9_-]+)\\.mbtiles<\\/a>[ ]*([0-9]{2}-[A-Za-z]{3}-[0-9]{4}) [0-9]{2}:[0-9]{2}[ ]*([0-9]+)"); // 1:file name, 2:display name, 3:date DD-MMM-YYYY, 4:size (bytes)
    private static final MapDownloaderOpenAndroMapsBackgroundMaps INSTANCE = new MapDownloaderOpenAndroMapsBackgroundMaps();

    private MapDownloaderOpenAndroMapsBackgroundMaps() {
        super(Download.DownloadType.DOWNLOADTYPE_MAP_OPENANDROMAPS_BACKGROUNDS, R.string.mapserver_openandromaps_backgroundmaps, R.string.mapserver_openandromaps_name, R.string.mapserver_openandromaps_info, R.string.mapserver_openandromaps_projecturl, R.string.mapserver_openandromaps_likeiturl, PersistableFolder.BACKGROUND_MAPS);
        this.iconRes = AbstractMapDownloader.ICONRES_MAP;
        this.forceExtension = ".mbtiles";
        companionType = null;
        downloadHasExtraContents = true;
    }

    @Override
    protected void analyzePage(final Uri uri, final List<Download> list, final @NonNull String page) {
        final MatcherWrapper matchMap = new MatcherWrapper(PATTERN_MAP, page);
        while (matchMap.find()) {
            final long size = Long.parseLong(matchMap.group(4));
            if (size > 0) {
                final Download offlineMap = new Download(matchMap.group(2), Uri.parse(uri + matchMap.group(1)), false, CalendarUtils.yearMonthDay(CalendarUtils.parseDayMonthYearUS(matchMap.group(3))), Formatter.formatBytes(size), offlineMapType, iconRes);
                list.add(offlineMap);
            }
        }
    }

    @Nullable
    @Override
    protected Download checkUpdateFor(final @NonNull String page, final String remoteUrl, final String remoteFilename) {
        final MatcherWrapper matchMap = new MatcherWrapper(PATTERN_MAP, page);
        while (matchMap.find()) {
            final String filename = matchMap.group(1);
            if (filename.equals(remoteFilename)) {
                final long size = Long.parseLong(matchMap.group(4));
                if (size > 0) {
                    return new Download(matchMap.group(2), Uri.parse(remoteUrl + "/" + filename), false, CalendarUtils.yearMonthDay(CalendarUtils.parseDayMonthYearUS(matchMap.group(3))), Formatter.formatBytes(size), offlineMapType, iconRes);
                }
            }
        }
        return null;
    }

    @NonNull
    public static MapDownloaderOpenAndroMapsBackgroundMaps getInstance() {
        return INSTANCE;
    }
}
