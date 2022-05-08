package cgeo.geocaching.downloader;

import cgeo.geocaching.CgeoApplication;
import cgeo.geocaching.R;
import cgeo.geocaching.models.Download;
import cgeo.geocaching.utils.MatcherWrapper;

import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.List;
import java.util.regex.Pattern;

public class MapDownloaderHyllyThemes extends AbstractThemeDownloader {
    private static final Pattern PATTERN_MAP = Pattern.compile("href=\"(https:\\/\\/kartat-dl\\.hylly\\.org\\/(\\d{4}-\\d\\d-\\d\\d)\\/([\\w ]+\\.zip))\">[\\w ]+kartta\\.zip<\\/a>"); // 1:url, 2:date yyyy-MM-dd, 3:file name, 4:size (string)
    private static final MapDownloaderHyllyThemes INSTANCE = new MapDownloaderHyllyThemes();

    private MapDownloaderHyllyThemes() {
        super(Download.DownloadType.DOWNLOADTYPE_THEME_HYLLY, R.string.mapserver_hylly_themes_updatecheckurl, R.string.mapserver_hylly_themes_name, R.string.mapserver_hylly_themes_info, R.string.mapserver_hylly_projecturl, R.string.mapserver_hylly_likeiturl);
    }

    @Override
    protected void analyzePage(final Uri uri, final List<Download> list, final @NonNull String page) {
        final MatcherWrapper matchMap = new MatcherWrapper(PATTERN_MAP, page);
        while (matchMap.find()) {
            final Download offlineMap = new Download(matchMap.group(3), Uri.parse(matchMap.group(1)), false, matchMap.group(2), "", offlineMapType, iconRes);
            list.add(offlineMap);
        }
    }

    @Nullable
    @Override
    protected Download checkUpdateFor(final @NonNull String page, final String remoteUrl, final String remoteFilename) {
        final MatcherWrapper matchMap = new MatcherWrapper(PATTERN_MAP, page);
        while (matchMap.find()) {
            final String filename = matchMap.group(3);
            if (filename.equals(remoteFilename)) {
                return new Download(matchMap.group(3), Uri.parse(remoteUrl + "/" + filename), false, matchMap.group(2), "", offlineMapType, iconRes);
            }
        }
        return null;
    }

    // hylly uses different servers for update check and download, need to map here
    @Override
    protected String getUpdatePageUrl(final String downloadPageUrl) {
        return CgeoApplication.getInstance().getString(R.string.mapserver_hylly_updatecheckurl);
    }

    @NonNull
    public static MapDownloaderHyllyThemes getInstance() {
        return INSTANCE;
    }
}
