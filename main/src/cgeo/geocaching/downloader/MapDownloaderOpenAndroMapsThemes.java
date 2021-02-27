package cgeo.geocaching.downloader;

import cgeo.geocaching.CgeoApplication;
import cgeo.geocaching.R;
import cgeo.geocaching.maps.mapsforge.v6.RenderThemeHelper;
import cgeo.geocaching.models.Download;
import cgeo.geocaching.storage.PersistableFolder;
import cgeo.geocaching.utils.MatcherWrapper;
import cgeo.geocaching.utils.UriUtils;

import android.net.Uri;

import androidx.annotation.NonNull;

import java.util.List;
import java.util.regex.Pattern;

public class MapDownloaderOpenAndroMapsThemes extends AbstractDownloader {

    private static final Pattern PATTERN_LAST_UPDATED_DATE = Pattern.compile("<a href=\"https:\\/\\/www\\.openandromaps\\.org\\/wp-content\\/users\\/tobias\\/version\\.txt\">[0-9]\\.[0-9]\\.[0-9]<\\/a><\\/strong>, ([0-9]{1,2})\\.([0-9]{1,2})\\.([0-9]{2}) ");
    private static final MapDownloaderOpenAndroMapsThemes INSTANCE = new MapDownloaderOpenAndroMapsThemes();

    private MapDownloaderOpenAndroMapsThemes() {
        super(Download.DownloadType.DOWNLOADTYPE_THEME_OPENANDROMAPS, R.string.mapserver_openandromaps_themes_updatecheckurl, R.string.mapserver_openandromaps_themes_name, R.string.mapserver_openandromaps_themes_info, R.string.mapserver_openandromaps_projecturl, R.string.mapserver_openandromaps_likeiturl, PersistableFolder.OFFLINE_MAP_THEMES);
        this.forceExtension = ".zip";
    }

    @Override
    protected void analyzePage(final Uri uri, final List<Download> list, final String page) {
        final Download file = checkUpdateFor(page, CgeoApplication.getInstance().getString(R.string.mapserver_openandromaps_themes_downloadurl), "Elevate.zip");
        if (file != null) {
            list.add(file);
        }
    }

    @Override
    protected Download checkUpdateFor(final String page, final String remoteUrl, final String remoteFilename) {
        final MatcherWrapper matchDate = new MatcherWrapper(PATTERN_LAST_UPDATED_DATE, page);
        if (matchDate.find()) {
            final String date = "20" + matchDate.group(3) + "-" + matchDate.group(2) + "-" + matchDate.group(1);
            return new Download("Elevate", Uri.parse(CgeoApplication.getInstance().getString(R.string.mapserver_openandromaps_themes_downloadurl) + "Elevate.zip"), false, date, "", offlineMapType);
        }
        return null;
    }

    // elevate uses different servers for update check and download, need to map here
    @Override
    protected String getUpdatePageUrl(final String downloadPageUrl) {
        final String compare = downloadPageUrl.endsWith("/") ? downloadPageUrl : downloadPageUrl + "/";
        final String downloadUrl = CgeoApplication.getInstance().getString(R.string.mapserver_openandromaps_themes_downloadurl);
        final String updateUrl = CgeoApplication.getInstance().getString(R.string.mapserver_openandromaps_themes_updatecheckurl);
        if (compare.startsWith(downloadUrl)) {
            final String result = updateUrl + compare.substring(downloadUrl.length());
            return result.endsWith("/") ? result : result + "/";
        }
        return downloadPageUrl;
    }

    @Override
    protected void onSuccessfulReceive(final Uri result) {

        //resync
        RenderThemeHelper.resynchronizeOrDeleteMapThemeFolder();

        //set map theme
        RenderThemeHelper.setSelectedMapThemeDirect(UriUtils.getLastPathSegment(result));
    }

    @NonNull
    public static MapDownloaderOpenAndroMapsThemes getInstance() {
        return INSTANCE;
    }
}
