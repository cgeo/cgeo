package cgeo.geocaching.downloader;

import cgeo.geocaching.R;
import cgeo.geocaching.activity.AbstractActivity;
import cgeo.geocaching.models.Download;

import android.net.Uri;
import android.os.Bundle;

public class DownloadConfirmationActivity extends AbstractActivity {

    public static final String BUNDLE_FILENAME = "filename";

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTheme();

        final Bundle bundle = getIntent().getExtras();
        if (bundle != null) {
            final Uri uri = Uri.parse(getString(R.string.brouter_downloadurl) + bundle.getString(BUNDLE_FILENAME));
            DownloaderUtils.triggerDownload(this, R.string.downloadtile_title, Download.DownloadType.DOWNLOADTYPE_BROUTER_TILES.id, uri, getString(R.string.downloadtile_info), "", this::finish, null);
        }
    }

}
