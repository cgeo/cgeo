package cgeo.geocaching.downloader;

import cgeo.geocaching.maps.mapsforge.MapsforgeMapProvider;
import cgeo.geocaching.models.OfflineMap;
import cgeo.geocaching.storage.PersistableFolder;
import cgeo.geocaching.utils.FileUtils;

import android.net.Uri;

import androidx.annotation.StringRes;

abstract class AbstractMapDownloader extends AbstractDownloader {

    AbstractMapDownloader(final OfflineMap.OfflineMapType offlineMapType, final @StringRes int mapBase, final @StringRes int mapSourceName, final @StringRes int mapSourceInfo, final @StringRes int projectUrl, final @StringRes int likeItUrl) {
        super(offlineMapType, mapBase, mapSourceName, mapSourceInfo, projectUrl, likeItUrl, PersistableFolder.OFFLINE_MAPS);
        this.forceExtension = FileUtils.MAP_FILE_EXTENSION;
    }

    @Override
    protected void onSuccessfulReceive(final Uri result) {
        // update offline maps
        MapsforgeMapProvider.getInstance().updateOfflineMaps(result);
    }

}
