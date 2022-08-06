package cgeo.geocaching.downloader;

import cgeo.geocaching.R;
import cgeo.geocaching.activity.AbstractActivity;
import cgeo.geocaching.models.Download;
import cgeo.geocaching.utils.Log;

import android.net.Uri;
import android.os.Bundle;

/* Receives a download URL via "mf-v4-map" scheme, e. g. from openandromaps.org */
public class MapDownloaderReceiverSchemeMap extends AbstractActivity {
    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTheme();

        final Uri uri = getIntent().getData();
        final String host = uri.getHost();
        final String path = uri.getPath();
        Log.i("MapDownloaderReceiverSchemeMap: host=" + host + ", path=" + path);

        if (host.equals("download.openandromaps.org") && path.startsWith("/mapsV4/") && path.endsWith(".zip")) {
            // check for OpenAndroMaps - mf-v4-map://download.openandromaps.org/mapsV4/Germany/bayern.zip
            // remap Uri to their ftp server
            final Uri newUri = Uri.parse(getString(R.string.mapserver_openandromaps_downloadurl) + path.substring(8));
            DownloaderUtils.triggerDownload(this, R.string.downloadmap_title, Download.DownloadType.DOWNLOADTYPE_MAP_OPENANDROMAPS.id, newUri, "", "", this::callback, null);
        } else if (host.equals("kartat-dl.hylly.org") && path.endsWith("/mtk_suomi.map")) {
            // check for Hylly maps - mf-v4-map://kartat-dl.hylly.org/2021-04-25/mtk_suomi.map
            DownloaderUtils.triggerDownload(this, R.string.downloadmap_title, Download.DownloadType.DOWNLOADTYPE_MAP_HYLLY.id, Uri.parse("https://" + host + path), "", "", this::callback, null);
        } else {
            // generic map download - only pure download supported, no updates
            DownloaderUtils.triggerDownload(this, R.string.downloadmap_title, Download.DownloadType.DOWNLOADTYPE_MAP_JUSTDOWNLOAD.id, Uri.parse("https://" + host + path), "", "", this::callback, null);
        }
    }

    public void callback() {
        finish();
    }
}
