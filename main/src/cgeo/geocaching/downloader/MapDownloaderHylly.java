package cgeo.geocaching.downloader;

import cgeo.geocaching.CgeoApplication;
import cgeo.geocaching.R;
import cgeo.geocaching.models.Download;
import cgeo.geocaching.utils.MatcherWrapper;

import android.app.Activity;
import android.net.Uri;

import androidx.annotation.NonNull;

import java.util.List;
import java.util.regex.Pattern;

public class MapDownloaderHylly extends AbstractMapDownloader {
    private static final Pattern PATTERN_MAP = Pattern.compile("href=\"(https:\\/\\/kartat-dl\\.hylly\\.org\\/(\\d{4}-\\d\\d-\\d\\d)\\/(mtk_suomi\\.map))\">mtk_suomi.map<\\/a>\\s*<\\/td>\\s*<td>(\\d+\\.\\d+ GB)<\\/td>"); // 1:url, 2:date yyyy-MM-dd, 3:file name, 4:size (string)
    private static final String[] THEME_FILES = {"peruskartta.zip"};
    private static final MapDownloaderHylly INSTANCE = new MapDownloaderHylly();

    private MapDownloaderHylly() {
        super (Download.DownloadType.DOWNLOADTYPE_MAP_HYLLY, R.string.mapserver_hylly_updatecheckurl, R.string.mapserver_hylly_name, R.string.mapserver_hylly_info, R.string.mapserver_hylly_projecturl, R.string.mapserver_hylly_likeiturl);
    }

    @Override
    protected void analyzePage(final Uri uri, final List<Download> list, final String page) {
        final MatcherWrapper matchMap = new MatcherWrapper(PATTERN_MAP, page);
        while (matchMap.find()) {
            final Download offlineMap = new Download(matchMap.group(3), Uri.parse(matchMap.group(1)), false, matchMap.group(2), matchMap.group(4), offlineMapType);
            list.add(offlineMap);
        }
    }

    @Override
    protected Download checkUpdateFor(final String page, final String remoteUrl, final String remoteFilename) {
        final MatcherWrapper matchMap = new MatcherWrapper(PATTERN_MAP, page);
        while (matchMap.find()) {
            final String filename = matchMap.group(3);
            if (filename.equals(remoteFilename)) {
                return new Download(matchMap.group(3), Uri.parse(remoteUrl + "/" + filename), false, matchMap.group(2), matchMap.group(4), offlineMapType);
            }
        }
        return null;
    }

    // hylly uses different servers for update check and download, need to map here
    @Override
    protected String getUpdatePageUrl(final String downloadPageUrl) {
        return CgeoApplication.getInstance().getString(R.string.mapserver_hylly_updatecheckurl);
    }


    @Override
    protected void onFollowup(final Activity activity, final Runnable callback) {
        // check whether theme file exists in theme folder and ask whether user wants to download it as well, if it does not exist yet
        findOrDownload(activity, THEME_FILES, activity.getString(R.string.mapserver_hylly_themes_downloadurl), Download.DownloadType.DOWNLOADTYPE_THEME_HYLLY, callback);
    }

    @NonNull
    public static MapDownloaderHylly getInstance() {
        return INSTANCE;
    }
}
