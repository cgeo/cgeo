package cgeo.geocaching.downloader;

import cgeo.geocaching.R;
import cgeo.geocaching.models.OfflineMap;
import cgeo.geocaching.utils.MatcherWrapper;

import android.net.Uri;

import androidx.annotation.NonNull;

import java.util.List;
import java.util.regex.Pattern;

public class MapDownloaderMapsforge extends AbstractMapDownloader {

    private static final Pattern PATTERN_MAP = Pattern.compile("alt=\"\\[ \\]\"><\\/td><td><a href=\"(([-a-z]+)\\.map)\">[-a-z]+\\.map<\\/a><\\/td><td align=\"right\">([-0-9]+)[ 0-9:]+<\\/td><td align=\"right\">([ 0-9\\.]+[KMG])<\\/td>");
    private static final Pattern PATTERN_DIR = Pattern.compile("alt=\"\\[DIR\\]\"><\\/td><td><a href=\"([-a-z]+\\/)");
    private static final Pattern PATTERN_UP = Pattern.compile("alt=\"\\[PARENTDIR\\]\"><\\/td><td><a href=\"((\\/[-a-zA-Z0-9\\.]+)+\\/)");
    private static final MapDownloaderMapsforge INSTANCE = new MapDownloaderMapsforge();

    private MapDownloaderMapsforge() {
        super (OfflineMap.OfflineMapType.MAP_DOWNLOAD_TYPE_MAPSFORGE, R.string.mapserver_mapsforge_downloadurl, R.string.mapserver_mapsforge_name, R.string.mapserver_mapsforge_info, R.string.mapserver_mapsforge_projecturl, R.string.mapserver_mapsforge_likeiturl);
    }

    @Override
    protected void analyzePage(final Uri uri, final List<OfflineMap> list, final String page) {
        basicUpMatcher(uri, list, page, PATTERN_UP);

        final MatcherWrapper matchDir = new MatcherWrapper(PATTERN_DIR, page);
        while (matchDir.find()) {
            final OfflineMap offlineMap = new OfflineMap(matchDir.group(1), Uri.parse(uri + matchDir.group(1)), true, "", "", offlineMapType);
            list.add(offlineMap);
        }

        final MatcherWrapper matchMap = new MatcherWrapper(PATTERN_MAP, page);
        while (matchMap.find()) {
            final OfflineMap offlineMap = new OfflineMap(matchMap.group(2), Uri.parse(uri + matchMap.group(1)), false, matchMap.group(3), matchMap.group(4), offlineMapType);
            list.add(offlineMap);
        }
    }

    @Override
    protected OfflineMap checkUpdateFor(final String page, final String remoteUrl, final String remoteFilename) {
        final MatcherWrapper matchMap = new MatcherWrapper(PATTERN_MAP, page);
        while (matchMap.find()) {
            final String filename = matchMap.group(1);
            if (filename.equals(remoteFilename)) {
                return new OfflineMap(matchMap.group(2), Uri.parse(remoteUrl + "/" + filename), false, matchMap.group(3), matchMap.group(4), offlineMapType);
            }
        }
        return null;
    }

    @NonNull
    public static MapDownloaderMapsforge getInstance() {
        return INSTANCE;
    }
}

