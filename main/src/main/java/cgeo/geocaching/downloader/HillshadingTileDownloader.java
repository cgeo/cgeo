package cgeo.geocaching.downloader;

import cgeo.geocaching.CgeoApplication;
import cgeo.geocaching.R;
import cgeo.geocaching.models.Download;
import cgeo.geocaching.network.Network;
import cgeo.geocaching.storage.PersistableFolder;
import cgeo.geocaching.utils.MatcherWrapper;

import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

public class HillshadingTileDownloader extends AbstractDownloader {
    private static final Pattern PATTERN_TILE = Pattern.compile("href=\"(([N|S][0-9]+[E|W][0-9]*\\.hgt)\\.zip)\">[N|S][0-9]+[E|W][0-9]*\\.hgt\\.zip<\\/a><\\/td><td align=\"right\">([-0-9]+)[ 0-9:]+<\\/td><td align=\"right\">([ 0-9\\.]+[KMG])<\\/td>");
    private static final Pattern PATTERN_DIR = Pattern.compile("alt=\"\\[DIR\\]\"><\\/td><td><a href=\"([-a-z]+\\/)");
    private static final Pattern PATTERN_UP = Pattern.compile("alt=\"\\[PARENTDIR\\]\"><\\/td><td><a href=\"((\\/[-a-zA-Z0-9\\.]+)+\\/)");
    private static final HillshadingTileDownloader INSTANCE = new HillshadingTileDownloader();
    public static final String HILLSHADING_TILE_FILEEXTENSION = ".hgt";

    private HillshadingTileDownloader() {
        super(Download.DownloadType.DOWNLOADTYPE_HILLSHADING_TILES, R.string.hillshading_downloadurl, R.string.hillshading_name, R.string.hillshading_info, R.string.hillshading_projecturl, 0, PersistableFolder.OFFLINE_MAP_SHADING);
        useCompanionFiles = false; // use single uri, and no companion files
        forceExtension = HILLSHADING_TILE_FILEEXTENSION;
    }

    @Override
    protected void analyzePage(final Uri uri, final List<Download> list, final @NonNull String page) {
        basicUpMatcher(uri, list, page, PATTERN_UP);

        final MatcherWrapper matchDir = new MatcherWrapper(PATTERN_DIR, page);
        while (matchDir.find()) {
            final Download offlineMap = new Download(matchDir.group(1), Uri.parse(uri + matchDir.group(1)), true, "", "", offlineMapType, ICONRES_FOLDER);
            list.add(offlineMap);
        }

        final MatcherWrapper matchMap = new MatcherWrapper(PATTERN_TILE, page);
        while (matchMap.find()) {
            final Download offlineMap = new Download(matchMap.group(2), Uri.parse(uri + matchMap.group(1)), false, matchMap.group(3), matchMap.group(4), offlineMapType, iconRes);
            list.add(offlineMap);
        }
    }

    @Nullable
    @Override
    protected Download checkUpdateFor(final @NonNull String page, final String remoteUrl, final String remoteFilename) {
        final MatcherWrapper matchMap = new MatcherWrapper(PATTERN_TILE, page);
        while (matchMap.find()) {
            final String filename = matchMap.group(1);
            if (filename.equals(remoteFilename)) {
                return new Download(matchMap.group(2), Uri.parse(remoteUrl + "/" + filename), false, matchMap.group(3), matchMap.group(4), offlineMapType, iconRes);
            }
        }
        return null;
    }

    @NonNull
    public static HillshadingTileDownloader getInstance() {
        return INSTANCE;
    }

    // used for area tile checking, see MapUtils
    @WorkerThread
    public HashMap<String, Download> getAvailableTiles(final Set<String> foldernames) {
        final HashMap<String, Download> tiles = new HashMap<>();

        final String url = CgeoApplication.getInstance().getString(R.string.hillshading_downloadurl);
        for (String foldername : foldernames) {
            tiles.putAll(getAvailableTilesSubfolder(url + foldername + "/"));
        }
        return tiles;
    }

    private HashMap<String, Download> getAvailableTilesSubfolder(final String url) {
        final HashMap<String, Download> tiles = new HashMap<>();
        final String page = Network.getResponseData(Network.getRequest(url));
        final List<Download> list = new ArrayList<>();
        if (page != null) {
            analyzePage(Uri.parse(url), list, page);
        }
        for (Download download : list) {
            tiles.put(download.getName(), download);
        }
        return tiles;
    }
}
