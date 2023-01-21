package cgeo.geocaching.downloader;

import cgeo.geocaching.R;
import cgeo.geocaching.models.Download;
import cgeo.geocaching.utils.MatcherWrapper;

import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.List;
import java.util.regex.Pattern;

public class MapDownloaderMapsforge extends AbstractMapDownloader {

    private static final Pattern PATTERN_MAP = Pattern.compile("alt=\"\\[ \\]\"><\\/td><td><a href=\"(([-a-z]+)\\.map)\">[-a-z]+\\.map<\\/a><\\/td><td align=\"right\">([-0-9]+)[ 0-9:]+<\\/td><td align=\"right\">([ 0-9\\.]+[KMG])<\\/td>");
    private static final Pattern PATTERN_DIR = Pattern.compile("alt=\"\\[DIR\\]\"><\\/td><td><a href=\"([-a-z]+\\/)");
    private static final Pattern PATTERN_UP = Pattern.compile("alt=\"\\[PARENTDIR\\]\"><\\/td><td><a href=\"((\\/[-a-zA-Z0-9\\.]+)+\\/)");
    private static final MapDownloaderMapsforge INSTANCE = new MapDownloaderMapsforge();

    private MapDownloaderMapsforge() {
        super(Download.DownloadType.DOWNLOADTYPE_MAP_MAPSFORGE, R.string.mapserver_mapsforge_downloadurl, R.string.mapserver_mapsforge_name, R.string.mapserver_mapsforge_info, R.string.mapserver_mapsforge_projecturl, R.string.mapserver_mapsforge_likeiturl);
    }

    @Override
    protected void analyzePage(final Uri uri, final List<Download> list, final @NonNull String page) {
        basicUpMatcher(uri, list, page, PATTERN_UP);

        final MatcherWrapper matchDir = new MatcherWrapper(PATTERN_DIR, page);
        while (matchDir.find()) {
            final Download offlineMap = new Download(matchDir.group(1), Uri.parse(uri + matchDir.group(1)), true, "", "", offlineMapType, ICONRES_FOLDER);
            list.add(offlineMap);
        }

        final MatcherWrapper matchMap = new MatcherWrapper(PATTERN_MAP, page);
        while (matchMap.find()) {
            final Download offlineMap = new Download(matchMap.group(2), Uri.parse(uri + matchMap.group(1)), false, matchMap.group(3), matchMap.group(4), offlineMapType, iconRes);
            list.add(offlineMap);
        }
    }

    @Nullable
    @Override
    protected Download checkUpdateFor(final @NonNull String page, final String remoteUrl, final String remoteFilename) {
        final MatcherWrapper matchMap = new MatcherWrapper(PATTERN_MAP, page);
        while (matchMap.find()) {
            final String filename = matchMap.group(1);
            if (filename.equals(remoteFilename)) {
                return new Download(matchMap.group(2), Uri.parse(remoteUrl + "/" + filename), false, matchMap.group(3), matchMap.group(4), offlineMapType, iconRes);
            }
        }
        return null;
    }

    @NonNull
    public static MapDownloaderMapsforge getInstance() {
        return INSTANCE;
    }
}

