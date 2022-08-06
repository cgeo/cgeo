package cgeo.geocaching.downloader;

import cgeo.geocaching.R;
import cgeo.geocaching.activity.AbstractActivity;
import cgeo.geocaching.models.Download;
import cgeo.geocaching.utils.Log;

import android.net.Uri;
import android.os.Bundle;

/* Receives a download URL via "mf-theme" scheme, e. g. from openandromaps.org */
public class MapDownloaderReceiverSchemeMapTheme extends AbstractActivity {
    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTheme();

        final Uri uri = getIntent().getData();
        final String host = uri.getHost();
        final String path = uri.getPath();
        Log.i("MapDownloaderReceiverSchemeMapTheme: host=" + host + ", path=" + path);

        if (host.equals("download.openandromaps.org") && path.startsWith("/themes/") && path.endsWith(".zip")) {
            // check for OpenAndroMaps
            // no remapping, as they have themes only on their homepage, not on their ftp site
            final Uri newUri = Uri.parse(getString(R.string.mapserver_openandromaps_themes_downloadurl) + path.substring(8));
            DownloaderUtils.triggerDownload(this, R.string.downloadmap_title, Download.DownloadType.DOWNLOADTYPE_THEME_OPENANDROMAPS.id, newUri, "", "", this::callback, null);
        } else if (host.equals("kartat-dl.hylly.org") && path.endsWith("kartta.zip")) {
            // check for Hylly map themes - mf-theme://kartat-dl.hylly.org/2021-04-25/peruskartta.zip
            DownloaderUtils.triggerDownload(this, R.string.downloadmap_title, Download.DownloadType.DOWNLOADTYPE_THEME_HYLLY.id, Uri.parse("https://" + host + path), "", "", this::callback, null);
        } else {
            // generic map theme download - only pure download supported, no updates
            DownloaderUtils.triggerDownload(this, R.string.downloadmap_title, Download.DownloadType.DOWNLOADTYPE_THEME_JUSTDOWNLOAD.id, Uri.parse("https://" + host + path), "", "", this::callback, null);
        }
    }

    public void callback() {
        finish();
    }
}
