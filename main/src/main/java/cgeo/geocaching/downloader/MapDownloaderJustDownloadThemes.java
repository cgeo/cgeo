package cgeo.geocaching.downloader;

import cgeo.geocaching.R;
import cgeo.geocaching.models.Download;

import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.List;

public class MapDownloaderJustDownloadThemes extends AbstractThemeDownloader {

    private static final MapDownloaderJustDownloadThemes INSTANCE = new MapDownloaderJustDownloadThemes();

    private MapDownloaderJustDownloadThemes() {
        super(Download.DownloadType.DOWNLOADTYPE_THEME_JUSTDOWNLOAD, 0, R.string.downloadmap_themefile, 0, 0, 0);
        useCompanionFiles = false; // use single uri, and no companion files
    }

    @Override
    protected void analyzePage(final Uri uri, final List<Download> list, final @NonNull String page) {
        // do nothing
    }

    @Nullable
    @Override
    protected Download checkUpdateFor(final @NonNull String page, final String remoteUrl, final String remoteFilename) {
        return null;
    }

    @NonNull
    public static MapDownloaderJustDownloadThemes getInstance() {
        return INSTANCE;
    }

}
