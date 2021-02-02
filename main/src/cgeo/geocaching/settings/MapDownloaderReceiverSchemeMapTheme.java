package cgeo.geocaching.settings;

import cgeo.geocaching.R;
import cgeo.geocaching.activity.AbstractActivity;
import cgeo.geocaching.models.OfflineMap;
import cgeo.geocaching.utils.Log;
import cgeo.geocaching.utils.MapDownloadUtils;

import android.net.Uri;
import android.os.Bundle;

/* Receives a download URL via "mf-theme" scheme, e. g. from openandromaps.org */
class MapDownloaderReceiverSchemeMapTheme extends AbstractActivity {
    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTheme();

        final Uri uri = getIntent().getData();
        final String host = uri.getHost();
        final String path = uri.getPath();
        Log.i("MapDownloaderReceiverSchemeMapTheme: host=" + host + ", path=" + path);

        // check for OpenAndroMaps
        if (host.equals("download.openandromaps.org") && path.startsWith("/themes/") && path.endsWith(".zip")) {
            // no remapping, as they have themes only on their homepage, not on their ftp site
            final Uri newUri = Uri.parse(getString(R.string.mapserver_elevate_downloadurl) + path.substring(8));
            MapDownloadUtils.triggerDownload(this, OfflineMap.OfflineMapType.MAP_DOWNLOAD_TYPE_ELEVATE.id, newUri, "", System.currentTimeMillis(), this::callback);
        } else {
            // generic map theme download - not yet supported
            Log.w("MapDownloaderReceiverSchemeMapTheme: Received map theme download intent from unknown source: " + uri.toString());
            finish();
        }
    }

    public void callback() {
        finish();
    }
}
