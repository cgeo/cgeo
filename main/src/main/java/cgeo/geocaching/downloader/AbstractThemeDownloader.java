package cgeo.geocaching.downloader;

import cgeo.geocaching.R;
import cgeo.geocaching.maps.mapsforge.v6.RenderThemeHelper;
import cgeo.geocaching.models.Download;
import cgeo.geocaching.storage.PersistableFolder;
import cgeo.geocaching.utils.FileUtils;

import android.net.Uri;

import androidx.annotation.StringRes;

abstract class AbstractThemeDownloader extends AbstractDownloader {

    public static final int ICONRES_THEME = R.drawable.downloader_theme;

    AbstractThemeDownloader(final Download.DownloadType offlineMapType, final @StringRes int mapBase, final @StringRes int mapSourceName, final @StringRes int mapSourceInfo, final @StringRes int projectUrl, final @StringRes int likeItUrl) {
        super(offlineMapType, mapBase, mapSourceName, mapSourceInfo, projectUrl, likeItUrl, PersistableFolder.OFFLINE_MAP_THEMES);
        this.iconRes = ICONRES_THEME;
        this.forceExtension = FileUtils.ZIP_FILE_EXTENSION;
    }

    @Override
    protected void onSuccessfulReceive(final Uri result) {
        //resync
        RenderThemeHelper.resynchronizeOrDeleteMapThemeFolder();
    }

}
